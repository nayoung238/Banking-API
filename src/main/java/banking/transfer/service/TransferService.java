package banking.transfer.service;

import banking.account.entity.Account;
import banking.account.repository.AccountRepository;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.exchangeRate.ExchangeRateService;
import banking.transfer.dto.request.TransferDetailsRequestDto;
import banking.transfer.dto.request.TransferRequestDto;
import banking.transfer.dto.response.TransferSimpleResponseDto;
import banking.transfer.dto.response.TransferDetailsResponseDto;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
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
    public TransferDetailsResponseDto transfer(Long requesterId, TransferRequestDto request) {
        return executeTransfer(requesterId, request, (transfer, withdrawalAccount, depositAccount) ->
            TransferDetailsResponseDto.of(transfer, TransferType.WITHDRAWAL, withdrawalAccount, depositAccount)
        );
    }

    @Transactional
    public Transfer transfer(TransferRequestDto request) {
        return executeTransfer(null, request, (transfer, withdrawalAccount, depositAccount) -> transfer);
    }

    public <T> T executeTransfer(Long requesterId, TransferRequestDto request, TransferResultHandler<T> resultHandler) {
        if(request.withdrawalAccountNumber().equals(request.depositAccountNumber())) {
            throw new CustomException(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED);
        }

        Account withdrawalAccount = accountRepository.findByAccountNumberWithPessimisticLock(request.withdrawalAccountNumber())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

        if(requesterId != null && !withdrawalAccount.getUser().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }

        if(!withdrawalAccount.getPassword().equals(request.withdrawalAccountPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 락 없이 조회
        Account depositAccount = accountRepository.findByAccountNumber(request.depositAccountNumber())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

        // 출금 (sync)
        Transfer withdrawalTransfer = processWithdrawal(withdrawalAccount, depositAccount, request.amount());

        // 입금 (async)
        processDepositAsync(withdrawalTransfer);

        return resultHandler.handle(withdrawalTransfer, withdrawalAccount, depositAccount);
    }

    public Transfer processWithdrawal(Account withdrawalAccount, Account depositAccount, BigDecimal amount) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(withdrawalAccount.getCurrency(), depositAccount.getCurrency());
        BigDecimal convertedAmount = amount.multiply(exchangeRate);
        withdrawalAccount.decreaseBalance(convertedAmount);
        return saveTransferAndWithdrawalTransferDetails(withdrawalAccount, depositAccount, exchangeRate, convertedAmount);
    }

    // TODO: 실패 시 재시도 처리
    public void processDepositAsync(Transfer withdrawalTransfer) {
        CompletableFuture.runAsync(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(status -> {
                Account depositAccount = accountRepository.findByIdWithPessimisticLock(withdrawalTransfer.getDepositAccountId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

                // TODO: 소수점 정합성 확인 필요
                BigDecimal withdrawalAmount = withdrawalTransfer.getAmount();
                BigDecimal depositAmount = withdrawalAmount.divide(withdrawalTransfer.getExchangeRate());

                // 입금 계좌 잔액 변경
                depositAccount.increaseBalance(depositAmount);
                accountRepository.save(depositAccount);

                // 이체 내약 (입금) 추가
                saveDepositTransferDetails(withdrawalTransfer, depositAmount, depositAccount.getBalance());

                // TODO: 입금 알림

                return null;
            });
        });
    }

    public void processDeposit(Long depositAccountId, BigDecimal amount) {
        Account depositAccount = accountRepository.findById(depositAccountId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

        depositAccount.increaseBalance(amount);
        accountRepository.save(depositAccount);
    }

    @Transactional
    public Transfer transferForRefund(Long withdrawalUserId, String transferGroupId) {
        List<Transfer> transfers = transferRepository.findAllByTransferGroupId(transferGroupId);
        verifyTransfer(transfers, withdrawalUserId);

        Transfer origianlWithdrawalTransfer;
        Transfer originalDepositTransfer;
        if (transfers.get(0).getTransferType().equals(TransferType.WITHDRAWAL)) {
            origianlWithdrawalTransfer = transfers.get(0);
            originalDepositTransfer = transfers.get(1);
        } else {
            origianlWithdrawalTransfer = transfers.get(1);
            originalDepositTransfer = transfers.get(0);
        }

        // Ordered Locking
        Account originalWithdrawalAccount = null;
        Account originalDepositAccount = null;
        if(origianlWithdrawalTransfer.getWithdrawalAccountId().compareTo(origianlWithdrawalTransfer.getDepositAccountId()) < 0) {
            originalWithdrawalAccount = accountRepository.findByIdWithPessimisticLock(origianlWithdrawalTransfer.getWithdrawalAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

            originalDepositAccount = accountRepository.findByIdWithPessimisticLock(origianlWithdrawalTransfer.getDepositAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));
        } else {
            originalDepositAccount = accountRepository.findByIdWithPessimisticLock(origianlWithdrawalTransfer.getDepositAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

            originalWithdrawalAccount = accountRepository.findByIdWithPessimisticLock(origianlWithdrawalTransfer.getWithdrawalAccountId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));
        }

        Account refundWithdrawalAccount = originalDepositAccount;
        Account refundDepositAccount = originalWithdrawalAccount;

        // 결제 취소로 인한 출금
        processWithdrawal(refundWithdrawalAccount, refundDepositAccount, originalDepositTransfer.getAmount());

        // 결제 취소에 대한 출금 내역 생성
        Transfer refundTransfer = saveTransferAndWithdrawalTransferDetails(refundWithdrawalAccount, refundDepositAccount, originalDepositTransfer.getExchangeRate(), originalDepositTransfer.getAmount());

        // 결제 취소로 인한 입금 (결제 취소로 인한 출금 작업 완료 후 진행)
        processDeposit(refundDepositAccount.getId(), origianlWithdrawalTransfer.getAmount());

        // 결제 취소에 대한 입금 내역 생성
        saveDepositTransferDetails(refundTransfer, origianlWithdrawalTransfer.getAmount(), refundDepositAccount.getBalance());

        return refundTransfer;
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
    public Transfer saveTransferAndWithdrawalTransferDetails(Account withdrawalAccount, Account depositAccount, BigDecimal exchangeRate, BigDecimal amount) {
        Transfer transfer = Transfer.builder()
            .transferGroupId(createTransferGroupId(withdrawalAccount.getAccountNumber(), depositAccount.getAccountNumber()))
            .transferOwnerId(withdrawalAccount.getId())
            .transferType(TransferType.WITHDRAWAL)
            .withdrawalAccountId(withdrawalAccount.getId())
            .depositAccountId(depositAccount.getId())
            .currency(depositAccount.getCurrency() + "/" + withdrawalAccount.getCurrency())
            .exchangeRate(exchangeRate)
            .amount(amount)
            .balancePostTransaction(withdrawalAccount.getBalance())
            .build();

        transferRepository.save(transfer);
        return transfer;
    }

    public void saveDepositTransferDetails(Transfer withdrawalTransfer, BigDecimal amount, BigDecimal balancePostTransaction) {
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
    public interface TransferResultHandler<T> {
        T handle(Transfer transfer, Account withdrawalAccount, Account depositAccount);
    }
}
