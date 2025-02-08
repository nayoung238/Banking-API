package SN.BANK.transfer.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.exchangeRate.ExchangeRateService;
import SN.BANK.transfer.dto.request.TransferFindDetailRequest;
import SN.BANK.transfer.dto.request.TransferRequestDto;
import SN.BANK.transfer.dto.response.TransferFindDetailResponse;
import SN.BANK.transfer.dto.response.TransferFindResponse;
import SN.BANK.transfer.dto.response.TransferResponseDto;
import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.entity.TransferDetails;
import SN.BANK.transfer.enums.TransferType;
import SN.BANK.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final ExchangeRateService exchangeRateService;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public TransferResponseDto transfer(Long withdrawalUserId, TransferRequestDto request) {
        return executeTransfer(withdrawalUserId, request, (transfer, withdrawalAccount, depositAccount) ->
            TransferResponseDto.of(
                transfer, TransferType.WITHDRAWAL,
                withdrawalAccount.getAccountNumber(), withdrawalAccount.getUser().getName(),
                depositAccount.getAccountNumber(), depositAccount.getUser().getName()
            )
        );
    }

    @Transactional
    public Transfer transfer(TransferRequestDto request) {
        return executeTransfer(null, request, (transfer, withdrawalAccount, depositAccount) -> transfer);
    }

    private <T> T executeTransfer(Long withdrawalUserId, TransferRequestDto request, TransferResultHandler<T> resultHandler) {
        Account withdrawalAccount = accountRepository.findByAccountNumberWithPessimisticLock(request.withdrawalAccountNumber())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

        if(withdrawalUserId != null && !withdrawalAccount.getUser().getId().equals(withdrawalUserId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT);
        }

        if(!withdrawalAccount.getPassword().equals(request.withdrawalAccountPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 락 없이 조회
        Account depositAccount = accountRepository.findByAccountNumber(request.depositAccountNumber())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

        // 출금 (sync)
        Transfer transfer = processWithdrawal(withdrawalAccount, depositAccount, request.amount());

        // 입금 (async)
        processDepositAsync(transfer);

        return resultHandler.handle(transfer, withdrawalAccount, depositAccount);
    }

    private Transfer processWithdrawal(Account withdrawalAccount, Account depositAccount, BigDecimal amount) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(withdrawalAccount.getCurrency(), depositAccount.getCurrency());
        BigDecimal convertedAmount = amount.multiply(exchangeRate);
        withdrawalAccount.decreaseMoney(convertedAmount);
        return saveTransferAndWithdrawalTransferDetails(withdrawalAccount, depositAccount, exchangeRate, convertedAmount);
    }

    // TODO: 실패 시 재시도 처리
    private void processDepositAsync(Transfer transfer) {
        CompletableFuture.runAsync(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(status -> {
                Account depositAccount = accountRepository.findByIdWithPessimisticLock(transfer.getWithdrawalAccountId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

                BigDecimal withdrawalAmount = transfer.getTransferDetails().get(TransferType.WITHDRAWAL).getAmount();
                BigDecimal depositAmount = withdrawalAmount.divide(transfer.getExchangeRate());

                // 입금 계좌 잔액 변경
                depositAccount.increaseMoney(depositAmount);
                accountRepository.save(depositAccount);

                // 이체 내약 (입금) 추가
                saveDepositTransferDetails(transfer, depositAmount, depositAccount.getMoney());

                // TODO: 입금 알림

                return null;
            });
        });
    }

    @Transactional
    public Transfer transferForRefund(Long transferId) {
        Transfer originalTransfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

        // Ordered Locking
        Account originalWithdrawalAccount = null;
        Account originalDepositAccount = null;
        if(originalTransfer.getWithdrawalAccountId().compareTo(originalTransfer.getDepositAccountId()) < 0) {
            originalWithdrawalAccount = accountRepository.findByIdWithPessimisticLock(originalTransfer.getWithdrawalAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

            originalDepositAccount = accountRepository.findByIdWithPessimisticLock(originalTransfer.getDepositAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));
        } else {
            originalDepositAccount = accountRepository.findByIdWithPessimisticLock(originalTransfer.getDepositAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

            originalWithdrawalAccount = accountRepository.findByIdWithPessimisticLock(originalTransfer.getWithdrawalAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));
        }

        Account refundWithdrawalAccount = originalDepositAccount;
        Account refundDepositAccount = originalWithdrawalAccount;

        // 결제 취소로 인한 출금
        BigDecimal refundWithdrawalAmount = originalTransfer.getTransferDetails().get(TransferType.DEPOSIT).getAmount();
        refundWithdrawalAccount.decreaseMoney(refundWithdrawalAmount);

        // 결제 취소로 인한 입금
        BigDecimal refundDepositAmount = originalTransfer.getTransferDetails().get(TransferType.WITHDRAWAL).getAmount();
        refundDepositAccount.increaseMoney(refundDepositAmount);

        // 결제 취소에 대한 이체 내역 생성
        Transfer refundTransfer = saveTransferAndWithdrawalTransferDetails(refundWithdrawalAccount, refundDepositAccount, originalTransfer.getExchangeRate(), refundWithdrawalAmount);
        saveDepositTransferDetails(refundTransfer, refundWithdrawalAmount, refundDepositAccount.getMoney());

        return refundTransfer;
    }

    public Transfer saveTransferAndWithdrawalTransferDetails(Account withdrawalAccount, Account depositAccount, BigDecimal exchangeRate, BigDecimal amount) {
        Transfer transfer = Transfer.builder()
             .withdrawalAccountId(withdrawalAccount.getId())
             .depositAccountId(depositAccount.getId())
             .currency(depositAccount.getCurrency() + "/" + withdrawalAccount.getCurrency())
             .exchangeRate(exchangeRate)
             .build();

        TransferDetails withdrawalTransferDetails = TransferDetails.builder()
            .transfer(transfer)
            .transferType(TransferType.WITHDRAWAL)
            .amount(amount)
            .balancePostTransaction(withdrawalAccount.getMoney())
            .build();

        transfer.getTransferDetails().put(TransferType.WITHDRAWAL, withdrawalTransferDetails);
        transferRepository.save(transfer);
        return transfer;
    }

    public void saveDepositTransferDetails(Transfer transfer, BigDecimal amount, BigDecimal balancePostTransaction) {
        TransferDetails depositTransferDetails = TransferDetails.builder()
            .transfer(transfer)
            .transferType(TransferType.DEPOSIT)
            .amount(amount)
            .balancePostTransaction(balancePostTransaction)
            .build();

        transfer.getTransferDetails().put(TransferType.DEPOSIT, depositTransferDetails);
    }

    /**
     * 전체 이체 내역 조회
     */
    public List<TransferFindResponse> findAllTransfer(Long userId, Long accountId) {
        // 유효한 계좌인지 검증 (+ 사용자 유효성, 계좌-사용자 소유 검증)
        Account userAccount = accountService.getAccount(accountId);
        accountService.validateAccountOwner(userId, userAccount);

        Users user = userAccount.getUser();

        List<Transfer> txFindResponse = new ArrayList<>();
        txFindResponse.addAll(transferRepository.findBySenderAccountIdAndType(accountId, TransferType.WITHDRAWAL));
        txFindResponse.addAll(transferRepository.findByReceiverAccountIdAndType(accountId, TransferType.DEPOSIT));

        return txFindResponse.stream()
                .map(tx -> new TransferFindResponse(tx, user.getName(), userAccount.getAccountNumber()))
                .toList();
    }

    /**
     * 이체 내역 단건 조회
     */
    public TransferFindDetailResponse findTransfer(Long userId, TransferFindDetailRequest request) {
        // 유효한 계좌인지 검증 (+ 사용자 유효성, 계좌-사용자 소유 검증)
        Account userAccount = accountService.getAccount(request.accountId());
        accountService.validateAccountOwner(userId, userAccount);

        // 유효한 거래 내역인지 검증
        Transfer tx = transferRepository.findById(request.transactionId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSACTION));

        Account receiverAccount = getReceiverAccount(userId, tx, userAccount);

        String othersAccountNumber = receiverAccount.getAccountNumber();

        return new TransferFindDetailResponse(tx, receiverAccount.getUser().getName(), othersAccountNumber);
    }

    @FunctionalInterface
    private interface TransferResultHandler<T> {
        T handle(Transfer transfer, Account withdrawalAccount, Account depositAccount);
    }
}
