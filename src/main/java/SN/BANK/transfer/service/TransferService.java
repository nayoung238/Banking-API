package SN.BANK.transfer.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.exchangeRate.ExchangeRateService;
import SN.BANK.transfer.dto.request.TransferDetailsRequestDto;
import SN.BANK.transfer.dto.request.TransferRequestDto;
import SN.BANK.transfer.dto.response.TransferSimpleResponseDto;
import SN.BANK.transfer.dto.response.TransferDetailsResponseDto;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final ExchangeRateService exchangeRateService;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public TransferDetailsResponseDto transfer(Long withdrawalUserId, TransferRequestDto request) {
        return executeTransfer(withdrawalUserId, request, (transfer, withdrawalAccount, depositAccount) ->
            TransferDetailsResponseDto.of(transfer, TransferType.WITHDRAWAL, withdrawalAccount, depositAccount)
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
        withdrawalAccount.decreaseBalance(convertedAmount);
        return saveTransferAndWithdrawalTransferDetails(withdrawalAccount, depositAccount, exchangeRate, convertedAmount);
    }

    // TODO: 실패 시 재시도 처리
    private void processDepositAsync(Transfer transfer) {
        CompletableFuture.runAsync(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(status -> {
                Account depositAccount = accountRepository.findByIdWithPessimisticLock(transfer.getDepositAccountId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

                BigDecimal withdrawalAmount = transfer.getTransferDetails().get(TransferType.WITHDRAWAL).getAmount();
                BigDecimal depositAmount = withdrawalAmount.divide(transfer.getExchangeRate());

                // 입금 계좌 잔액 변경
                depositAccount.increaseBalance(depositAmount);
                accountRepository.save(depositAccount);

                // 이체 내약 (입금) 추가
                saveDepositTransferDetails(transfer, depositAmount, depositAccount.getBalance());

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
        refundWithdrawalAccount.decreaseBalance(refundWithdrawalAmount);

        // 결제 취소로 인한 입금
        BigDecimal refundDepositAmount = originalTransfer.getTransferDetails().get(TransferType.WITHDRAWAL).getAmount();
        refundDepositAccount.increaseBalance(refundDepositAmount);

        // 결제 취소에 대한 이체 내역 생성
        Transfer refundTransfer = saveTransferAndWithdrawalTransferDetails(refundWithdrawalAccount, refundDepositAccount, originalTransfer.getExchangeRate(), refundWithdrawalAmount);
        saveDepositTransferDetails(refundTransfer, refundWithdrawalAmount, refundDepositAccount.getBalance());

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
            .type(TransferType.WITHDRAWAL)
            .amount(amount)
            .balancePostTransaction(withdrawalAccount.getBalance())
            .build();

        transfer.getTransferDetails().put(TransferType.WITHDRAWAL, withdrawalTransferDetails);
        transferRepository.save(transfer);
        return transfer;
    }

    public void saveDepositTransferDetails(Transfer transfer, BigDecimal amount, BigDecimal balancePostTransaction) {
        TransferDetails depositTransferDetails = TransferDetails.builder()
            .transfer(transfer)
            .type(TransferType.DEPOSIT)
            .amount(amount)
            .balancePostTransaction(balancePostTransaction)
            .build();

        transfer.getTransferDetails().put(TransferType.DEPOSIT, depositTransferDetails);
    }

    /**
     * 이체 내역 전체 조회
     */
    // TODO: PAGE 적용
    public List<TransferSimpleResponseDto> findAllTransferSimple(Long userId, Long accountId) {
        // 계좌 소유자 검증
        verifyAccount(userId, accountId);

        List<Transfer> withdrawalTransferList = transferRepository.findAllByWithdrawalAccountId(accountId);
        List<Transfer> depositTransferList = transferRepository.findAllByDepositAccountId(accountId);

        return Stream.concat(
                withdrawalTransferList.stream().map(transfer -> {
                    Account peerAmount = accountRepository.findById(transfer.getDepositAccountId()).orElse(null);
                    String peerName = (peerAmount != null) ? peerAmount.getUser().getName() : "알 수 없는 사용자";
                    return TransferSimpleResponseDto.of(transfer, TransferType.WITHDRAWAL, peerName);
                }),
                depositTransferList.stream().map(transfer -> {
                    Account peerAmount = accountRepository.findById(transfer.getWithdrawalAccountId()).orElse(null);
                    String peerName = (peerAmount != null) ? peerAmount.getUser().getName() : "알 수 없는 사용자";
                    return TransferSimpleResponseDto.of(transfer, TransferType.WITHDRAWAL, peerName);
                })
            )
            .sorted(Comparator.comparing(TransferSimpleResponseDto::transactedAt).reversed())
            .toList();
    }

    /**
     * 이체 내역 단건 조회
     */
    public TransferDetailsResponseDto findTransferDetails(Long userId, TransferDetailsRequestDto request) {
        // 계좌 소유자 검증
        verifyAccount(userId, request.accountId());

        Transfer transfer = transferRepository.findById(request.transferId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

        Account withdrawalAccount = accountRepository.findById(transfer.getWithdrawalAccountId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

        Account depositAccount = accountRepository.findById(transfer.getDepositAccountId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

        TransferType transferType = getTransferType(transfer, request.accountId());
        verifyTransferAccount(request.accountId(), transferType, withdrawalAccount, depositAccount);

        return TransferDetailsResponseDto.of(transfer, transferType, withdrawalAccount, depositAccount);
    }

    private void verifyAccount(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        if(!account.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_ACCOUNT);
        }
    }

    private void verifyTransferAccount(Long requestedAccountId, TransferType transferType, Account withdrawalAccount, Account depositAccount) {
        if (transferType.equals(TransferType.WITHDRAWAL)) {
            if (!withdrawalAccount.getId().equals(requestedAccountId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
            }
        } else if (transferType.equals(TransferType.DEPOSIT)) {
            if (!depositAccount.getId().equals(requestedAccountId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
            }
        } else {
            throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
        }
    }

    private TransferType getTransferType(Transfer transfer, Long accountId) {
        if (Objects.equals(transfer.getWithdrawalAccountId(), accountId)) {
            return TransferType.WITHDRAWAL;
        } else if (Objects.equals(transfer.getDepositAccountId(), accountId)) {
            return TransferType.DEPOSIT;
        }

        throw new CustomException(ErrorCode.NOT_FOUND_TRANSFER);
    }

    @FunctionalInterface
    private interface TransferResultHandler<T> {
        T handle(Transfer transfer, Account withdrawalAccount, Account depositAccount);
    }
}
