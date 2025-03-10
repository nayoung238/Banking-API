package banking.transfer.service;

import banking.account.dto.response.AccountPublicInfoResponse;
import banking.account.entity.Account;
import banking.account.service.AccountBalanceService;
import banking.account.service.AccountService;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.exchangeRate.ExchangeRateService;
import banking.payment.dto.request.PaymentRequest;
import banking.transfer.dto.request.TransferRequest;
import banking.transfer.dto.response.PaymentTransferDetailResponse;
import banking.transfer.dto.response.TransferDetailResponse;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final DepositAsyncService depositAsyncService;
    private final ExchangeRateService exchangeRateService;
    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;

    @Transactional
    public TransferDetailResponse transfer(Long userId, TransferRequest request) {
        return executeTransfer(userId, request,
            (transfer, withdrawalAccount, depositAccountPublicInfo) -> {
                AccountPublicInfoResponse withdrawalAccountPublicInfo = accountService.findAccountPublicInfo(withdrawalAccount.getId(), transfer);
                return TransferDetailResponse.of(transfer, withdrawalAccountPublicInfo, depositAccountPublicInfo);
            }
        );
    }

    @Transactional
    public PaymentTransferDetailResponse transfer(Long userId, PaymentRequest request) {
        return executeTransfer(userId, TransferRequest.of(request),
            (transfer, withdrawalAccount, depositAccountPublicInfo) ->  PaymentTransferDetailResponse.of(transfer));
    }

    public <T> T executeTransfer(Long userId, TransferRequest request, TransferResultHandler<T> resultHandler) {
        // 출금 계좌 Entity GET with Lock
        Account withdrawalAccount = accountService.findAccountWithLock(userId, request.withdrawalAccountId(), request.withdrawalAccountPassword());

        // 같은 계좌 간 이체 불가
        if (withdrawalAccount.getAccountNumber().equals(request.depositAccountNumber())) {
            throw new CustomException(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED);
        }

        // 거래 계좌 Active 상태인지 파악
        accountService.verifyAccountActiveStatus(withdrawalAccount);

        // 입금 계좌 DTO GET
        AccountPublicInfoResponse depositAccountPublicInfo = accountService.findAccountPublicInfo(request.depositAccountNumber());

        // 출금 (sync)
        Transfer withdrawalTransfer = processWithdrawalAndSaveTransferDetail(withdrawalAccount, depositAccountPublicInfo, request.amount());

        // 입금 (async)
        depositAsyncService.processDepositAsync(withdrawalTransfer);

        return resultHandler.handle(withdrawalTransfer, withdrawalAccount, depositAccountPublicInfo);
    }

    public Transfer processWithdrawalAndSaveTransferDetail(Account withdrawalAccount, AccountPublicInfoResponse depositAccountPublicInfo, BigDecimal amount) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(depositAccountPublicInfo.currency(), withdrawalAccount.getCurrency());
        BigDecimal convertedAmount = amount.multiply(exchangeRate);
        withdrawalAccount.decreaseBalance(convertedAmount);
        return saveWithdrawalTransferDetail(withdrawalAccount, depositAccountPublicInfo, exchangeRate, convertedAmount);
    }

    @Transactional
    public PaymentTransferDetailResponse transferForRefund(Long userId, Long transferId, String accountPassword) {
        List<Transfer> transfers = transferRepository.findTransferGroupByTransferId(transferId);
        verifyTransfer(transfers, userId);

        Transfer baseWithdrawalTransfer, baseDepositTransfer;
        if (transfers.get(0).getTransferType().equals(TransferType.WITHDRAWAL)) {
            baseWithdrawalTransfer = transfers.get(0);
            baseDepositTransfer = transfers.get(1);
        } else {
            baseWithdrawalTransfer = transfers.get(1);
            baseDepositTransfer = transfers.get(0);
        }

        Map<Long, Account> accountsWithLock = getAccountsWithLock(baseWithdrawalTransfer, userId, accountPassword);

        Account refundDepositAccount = accountsWithLock.get(baseWithdrawalTransfer.getWithdrawalAccountId());
        Account refundWithdrawalAccount = accountsWithLock.get(baseWithdrawalTransfer.getDepositAccountId());

        AccountPublicInfoResponse refundDepositAccountPublicInfo = accountService.findAccountPublicInfo(refundDepositAccount.getId(), baseWithdrawalTransfer);

        // 결제 취소에 대한 출금 및 내역 생성
        Transfer refundWithdrawalTransfer = processWithdrawalAndSaveTransferDetail(refundWithdrawalAccount, refundDepositAccountPublicInfo, baseDepositTransfer.getAmount());

        // 결제 취소로 인한 입금 및 내역 생성
        accountBalanceService.increaseBalanceWithLock(refundDepositAccount.getId(), baseWithdrawalTransfer.getAmount());
        saveDepositTransferDetail(refundWithdrawalTransfer, baseWithdrawalTransfer.getAmount(), refundDepositAccount.getBalance());

        return PaymentTransferDetailResponse.of(refundWithdrawalTransfer);
    }

    public Map<Long, Account> getAccountsWithLock(Transfer transfer, Long userId, String accountPassword) {
        List<Long> accountIds = Stream.of(
            transfer.getWithdrawalAccountId(),
            transfer.getDepositAccountId()
        ).sorted().toList();

        return accountIds.stream()
            .collect(Collectors.toMap(
                accountId -> accountId,
                accountId -> {
                    if (accountId.equals(transfer.getWithdrawalAccountId())) {
                        return accountService.findAccountWithLock(userId, accountId, accountPassword);
                    }
                    else {
                        return accountService.findAccountWithLock(accountId, transfer);
                    }
                },
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
    }

    private void verifyTransfer(List<Transfer> transfers, Long withdrawalUserId) {
        if (transfers.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_FOUND_TRANSFER);
        }
        if (transfers.size() == 1) {
            if (transfers.get(0).getTransferType().equals(TransferType.WITHDRAWAL)) {
                /*
                    TODO: 어드민 알림 (비동기 처리 지연 or 유실 가능성)
                    상황: 출금만 존재, 수취인 계좌 입금 막아야 함
                    case1: 비동기 입금 처리 지연
                    case2: 비동기 압금 요청 유실
                 */
            }
            else if (transfers.get(0).getTransferType().equals(TransferType.DEPOSIT)) {
                /*
                    TODO: 어드민 알림 (데이터 정합성 깨짐)
                    출금없이 입금만 진행 -> 수취인 계좌 출금 필요
                 */
            }
            else {
                throw new CustomException(ErrorCode.UNSUPPORTED_TRANSFER_TYPE);
            }
        }

        Transfer withdrawalTransfer = transfers.get(0).getTransferType().equals(TransferType.WITHDRAWAL) ? transfers.get(0) : transfers.get(1);
        if (!withdrawalTransfer.getTransferOwnerId().equals(withdrawalUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
        }
    }

    @Retryable (
        retryFor = DataIntegrityViolationException.class,
        maxAttempts = 5,
        backoff = @Backoff(delay = 500)
    )
    public Transfer saveWithdrawalTransferDetail(Account withdrawalAccount, AccountPublicInfoResponse depositAccountPublicInfo, BigDecimal exchangeRate, BigDecimal amount) {
        Transfer transfer = Transfer.builder()
            .transferGroupId(createTransferGroupId(withdrawalAccount.getAccountNumber(), depositAccountPublicInfo.accountNumber()))
            .transferOwnerId(withdrawalAccount.getUser().getId())
            .transferType(TransferType.WITHDRAWAL)
            .withdrawalAccountId(withdrawalAccount.getId())
            .depositAccountId(depositAccountPublicInfo.accountId())
            .currency(depositAccountPublicInfo.currency() + "/" + withdrawalAccount.getCurrency())
            .exchangeRate(exchangeRate)
            .amount(amount)
            .balancePostTransaction(withdrawalAccount.getBalance())
            .build();

        transferRepository.save(transfer);
        return transfer;
    }

    public void saveDepositTransferDetail(Transfer withdrawalTransfer, BigDecimal amount, BigDecimal balancePostTransaction) {
        Transfer transfer = Transfer.builder()
            .transferGroupId(withdrawalTransfer.getTransferGroupId())
            .transferOwnerId(withdrawalTransfer.getDepositAccountId())
            .transferType(TransferType.DEPOSIT)
            .withdrawalAccountId(withdrawalTransfer.getWithdrawalAccountId())
            .depositAccountId(withdrawalTransfer.getDepositAccountId())
            .currency(withdrawalTransfer.getCurrency())
            .exchangeRate(withdrawalTransfer.getExchangeRate())
            .amount(amount)
            .balancePostTransaction(balancePostTransaction)
            .build();

        transferRepository.save(transfer);
    }

    private String createTransferGroupId(String withdrawalAccountNumber, String depositAccountNumber) {
        String transactionId = withdrawalAccountNumber.substring(0, 3) + depositAccountNumber.substring(0, 3);
        String uuidPart;
        do {
            uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        } while (transferRepository.existsByTransferGroupId(transactionId + uuidPart));
        return transactionId + uuidPart;
    }

    @FunctionalInterface
    public interface TransferResultHandler<T> {
        T handle(Transfer transfer, Account withdrawalAccount, AccountPublicInfoResponse depositAccountPublicInfo);
    }
}
