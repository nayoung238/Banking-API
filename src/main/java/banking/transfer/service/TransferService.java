package banking.transfer.service;

import banking.account.dto.response.AccountPublicInfoDto;
import banking.account.entity.Account;
import banking.account.repository.AccountRepository;
import banking.account.service.AccountBalanceService;
import banking.account.service.AccountService;
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
import banking.users.dto.response.UserPublicInfoDto;
import banking.users.service.UsersService;
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

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final ExchangeRateService exchangeRateService;
    private final UsersService usersService;
    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;
    private final AccountRepository accountRepository;

    private final PlatformTransactionManager transactionManager;

    @Transactional
    public TransferDetailsResponseDto transfer(Long requesterId, TransferRequestDto request) {
        return executeTransfer(requesterId, request, (transfer, withdrawalAccount, depositAccount) -> {
                AccountPublicInfoDto withdrawalAccountPublicInfo = accountService.findAccountPublicInfo(withdrawalAccount.getId());
                return TransferDetailsResponseDto.of(transfer, TransferType.WITHDRAWAL, withdrawalAccountPublicInfo, depositAccount);
            }
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

        // 송금 계좌 Entity GET with GET
        Account withdrawalAccount = accountService.findAuthorizedAccountWithLock(requesterId, request.withdrawalAccountNumber(), request.withdrawalAccountPassword());

        // 락 없이 Account DTO GET
        AccountPublicInfoDto depositAccountPublicInfo = accountService.findAccountPublicInfo(request.depositAccountNumber());

        // 출금 (sync)
        Transfer withdrawalTransfer = processWithdrawal(withdrawalAccount, depositAccountPublicInfo, request.amount());

        // 입금 (async)
        processDepositAsync(withdrawalTransfer);

        return resultHandler.handle(withdrawalTransfer, withdrawalAccount, depositAccountPublicInfo);
    }

    public Transfer processWithdrawal(Account withdrawalAccount, AccountPublicInfoDto depositAccountPublicInfo, BigDecimal amount) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(withdrawalAccount.getCurrency(), depositAccountPublicInfo.currency());
        BigDecimal convertedAmount = amount.multiply(exchangeRate);
        withdrawalAccount.decreaseBalance(convertedAmount);
        return saveTransferAndWithdrawalTransferDetails(withdrawalAccount, depositAccountPublicInfo, exchangeRate, convertedAmount);
    }

    // TODO: 비동기 트랜잭션 관리 & 실패 시 재시도 처리
    public void processDepositAsync(Transfer withdrawalTransfer) {
        CompletableFuture.runAsync(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(status -> {
                Account depositAccount = accountService.findAccountWithLock(withdrawalTransfer.getDepositAccountId());

                // TODO: 소수점 정합성 확인 필요
                BigDecimal withdrawalAmount = withdrawalTransfer.getAmount();
                BigDecimal depositAmount = withdrawalAmount.divide(withdrawalTransfer.getExchangeRate());

                // 입금 계좌 잔액 변경
                depositAccount.increaseBalance(depositAmount);
                accountRepository.save(depositAccount);

                // 이체 내역 (입금) 추가
                saveDepositTransferDetails(withdrawalTransfer, depositAmount, depositAccount.getBalance());

                // TODO: 입금 알림

                return null;
            });
        });
    }

    @Transactional
    public Transfer transferForRefund(Long requesterId, String transferGroupId, String requesterAccountPassword) {
        List<Transfer> transfers = transferRepository.findAllByTransferGroupId(transferGroupId);
        verifyTransfer(transfers, requesterId);

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
            originalWithdrawalAccount = accountService.findAuthorizedAccountWithLock(
                requesterId, origianlWithdrawalTransfer.getWithdrawalAccountId(), requesterAccountPassword);
            originalDepositAccount = accountService.findAccountWithLock(origianlWithdrawalTransfer.getDepositAccountId());
        } else {
            originalDepositAccount = accountService.findAccountWithLock(origianlWithdrawalTransfer.getDepositAccountId());
            originalWithdrawalAccount = accountService.findAuthorizedAccountWithLock(
                requesterId, origianlWithdrawalTransfer.getWithdrawalAccountId(), requesterAccountPassword);
        }

        Account refundWithdrawalAccount = originalDepositAccount;
        Account refundDepositAccount = originalWithdrawalAccount;
        AccountPublicInfoDto refundDepositAccountPublicInfo = accountService.findAccountPublicInfo(refundDepositAccount.getId());

        // 결제 취소로 인한 출금
        processWithdrawal(refundWithdrawalAccount, refundDepositAccountPublicInfo, originalDepositTransfer.getAmount());

        // 결제 취소에 대한 출금 내역 생성
        Transfer refundTransfer = saveTransferAndWithdrawalTransferDetails(
            refundWithdrawalAccount, refundDepositAccountPublicInfo,
            originalDepositTransfer.getExchangeRate(), originalDepositTransfer.getAmount());

        // 결제 취소로 인한 입금 (결제 취소로 인한 출금 작업 완료 후 진행)
        accountBalanceService.increaseBalance(refundDepositAccount.getId(), origianlWithdrawalTransfer.getAmount());

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
    public Transfer saveTransferAndWithdrawalTransferDetails(Account withdrawalAccount, AccountPublicInfoDto depositAccountPublicInfo, BigDecimal exchangeRate, BigDecimal amount) {
        Transfer transfer = Transfer.builder()
            .transferGroupId(createTransferGroupId(withdrawalAccount.getAccountNumber(), depositAccountPublicInfo.accountNumber()))
            .transferOwnerId(withdrawalAccount.getId())
            .transferType(TransferType.WITHDRAWAL)
            .withdrawalAccountId(withdrawalAccount.getId())
            .depositAccountId(depositAccountPublicInfo.id())
            .currency(depositAccountPublicInfo.currency() + "/" + withdrawalAccount.getCurrency())
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
    public List<TransferSimpleResponseDto> findAllTransferSimple(Long requesterId, Long accountId) {
        // 계좌 소유자 검증
        accountService.verifyAccountOwner(accountId, requesterId);

        return transferRepository.findAllByTransferOwnerId(accountId)
            .stream()
            .map(transfer -> {
                UserPublicInfoDto peerUserPublicInfo;
                if (transfer.getTransferType().equals(TransferType.WITHDRAWAL)) {
                    peerUserPublicInfo = usersService.findUserPublicInfo(transfer.getDepositAccountId());
                } else if (transfer.getTransferType().equals(TransferType.DEPOSIT)) {
                    peerUserPublicInfo = usersService.findUserPublicInfo(transfer.getWithdrawalAccountId());
                } else {
                    // TODO: 관리자에게 알림하고, 클라이언트에게는 응답
                    throw new CustomException(ErrorCode.UNSUPPORTED_TRANSFER_TYPE);
                }
                return TransferSimpleResponseDto.of(transfer, TransferType.WITHDRAWAL, peerUserPublicInfo.name());
            })
            .toList();
    }

    /**
     * 이체 내역 단건 조회
     */
    public TransferDetailsResponseDto findTransferDetails(Long requesterId, TransferDetailsRequestDto request) {
        // 계좌 소유자 검증
        accountService.verifyAccountOwner(request.accountId(), requesterId);

        Transfer transfer = transferRepository.findById(request.transferId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

        AccountPublicInfoDto withdrawalAccountPublicInfo = accountService.findAccountPublicInfo(transfer.getWithdrawalAccountId());
        AccountPublicInfoDto depositAccountPublicInfo = accountService.findAccountPublicInfo(transfer.getDepositAccountId());

        TransferType transferType = getTransferType(transfer, request.accountId());
        verifyTransferAccount(requesterId, transferType, withdrawalAccountPublicInfo, depositAccountPublicInfo);

        return TransferDetailsResponseDto.of(transfer, transferType, withdrawalAccountPublicInfo, depositAccountPublicInfo);
    }

    private void verifyTransferAccount(Long requestedId, TransferType transferType,
                                       AccountPublicInfoDto withdrawalAccountPublicInfo, AccountPublicInfoDto depositAccountPublicInfo) {

        if (transferType.equals(TransferType.WITHDRAWAL)) {
            if (!withdrawalAccountPublicInfo.ownerUserId().equals(requestedId)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
            }
        } else if (transferType.equals(TransferType.DEPOSIT)) {
            if (!depositAccountPublicInfo.ownerUserId().equals(requestedId)) {
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

    public Transfer findTransferEntity(String transferGroupId, TransferType transferType) {
        return transferRepository.findByTransferGroupIdAndTransferType(transferGroupId, TransferType.WITHDRAWAL)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));
    }

    @FunctionalInterface
    public interface TransferResultHandler<T> {
        T handle(Transfer transfer, Account withdrawalAccount, AccountPublicInfoDto depositAccountPublicInfo);
    }
}
