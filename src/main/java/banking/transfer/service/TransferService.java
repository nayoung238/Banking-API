package banking.transfer.service;

import banking.account.dto.response.AccountPublicInfoDto;
import banking.account.entity.Account;
import banking.account.service.AccountBalanceService;
import banking.account.service.AccountService;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.exchangeRate.ExchangeRateService;
import banking.payment.dto.request.PaymentRequestDto;
import banking.transfer.dto.request.TransferDetailsRequestDto;
import banking.transfer.dto.request.TransferRequestDto;
import banking.transfer.dto.response.TransferResponseForPaymentDto;
import banking.transfer.dto.response.TransferSimpleResponseDto;
import banking.transfer.dto.response.TransferDetailsResponseDto;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import banking.user.dto.response.UserPublicInfoDto;
import banking.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final DepositAsyncService depositAsyncService;
    private final ExchangeRateService exchangeRateService;
    private final UserService userService;
    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;

    @Transactional
    public TransferDetailsResponseDto transfer(Long requesterId, TransferRequestDto request) {
        return executeTransfer(requesterId, request,
            (transfer, withdrawalAccount, depositAccount) -> {
                AccountPublicInfoDto withdrawalAccountPublicInfo = accountService.findAccountPublicInfo(withdrawalAccount.getId(), transfer);
                return TransferDetailsResponseDto.of(transfer, TransferType.WITHDRAWAL, withdrawalAccountPublicInfo, depositAccount);
            }
        );
    }

    @Transactional
    public TransferResponseForPaymentDto transfer(Long requesterId, PaymentRequestDto request) {
        return executeTransfer(requesterId, TransferRequestDto.of(request),
            (transfer, withdrawalAccount, depositAccount) ->  TransferResponseForPaymentDto.of(transfer));
    }

    public <T> T executeTransfer(Long requesterId, TransferRequestDto request, TransferResultHandler<T> resultHandler) {
        // 출금 계좌 Entity GET with Lock
        Account withdrawalAccount = accountService.findAccountWithLock(requesterId, request.withdrawalAccountId(), request.withdrawalAccountPassword());

        // 같은 계좌 간 이체 불가
        if (withdrawalAccount.getAccountNumber().equals(request.depositAccountNumber())) {
            throw new CustomException(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED);
        }

        // 거래 계좌 Active 상태인지 파악
        accountService.verifyAccountActiveStatus(withdrawalAccount);

        // 입금 계좌 DTO GET
        AccountPublicInfoDto depositAccountPublicInfo = accountService.findAccountPublicInfo(request.depositAccountNumber());

        // 출금 (sync)
        Transfer withdrawalTransfer = processWithdrawal(withdrawalAccount, depositAccountPublicInfo, request.amount());

        // 입금 (async)
        depositAsyncService.processDepositAsync(withdrawalTransfer);

        return resultHandler.handle(withdrawalTransfer, withdrawalAccount, depositAccountPublicInfo);
    }

    public Transfer processWithdrawal(Account withdrawalAccount, AccountPublicInfoDto depositAccountPublicInfo, BigDecimal amount) {
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(depositAccountPublicInfo.currency(), withdrawalAccount.getCurrency());
        BigDecimal convertedAmount = amount.multiply(exchangeRate);
        withdrawalAccount.decreaseBalance(convertedAmount);
        return saveTransferAndWithdrawalTransferDetails(withdrawalAccount, depositAccountPublicInfo, exchangeRate, convertedAmount);
    }

    @Transactional
    public TransferResponseForPaymentDto transferForRefund(Long requesterId, String transferGroupId, String requesterAccountPassword) {
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
            originalWithdrawalAccount = accountService.findAccountWithLock(origianlWithdrawalTransfer.getWithdrawalAccountId(),
                                                                            requesterId,
                                                                            requesterAccountPassword);

            originalDepositAccount = accountService.findAccountWithLock(origianlWithdrawalTransfer.getDepositAccountId(),
                                                                        originalDepositTransfer);
        } else {
            originalDepositAccount = accountService.findAccountWithLock(origianlWithdrawalTransfer.getDepositAccountId(),
                                                                        originalDepositTransfer);

            originalWithdrawalAccount = accountService.findAccountWithLock(origianlWithdrawalTransfer.getWithdrawalAccountId(),
                                                                            requesterId,
                                                                            requesterAccountPassword);
        }

        Account refundWithdrawalAccount = originalDepositAccount;
        Account refundDepositAccount = originalWithdrawalAccount;
        AccountPublicInfoDto refundDepositAccountPublicInfo = accountService.findAccountPublicInfo(refundDepositAccount.getId(), origianlWithdrawalTransfer);

        // 결제 취소에 대한 출금
        processWithdrawal(refundWithdrawalAccount, refundDepositAccountPublicInfo, originalDepositTransfer.getAmount());

        // 결제 취소에 대한 출금 내역 생성
        Transfer refundTransfer = saveTransferAndWithdrawalTransferDetails(refundWithdrawalAccount,
                                                                            refundDepositAccountPublicInfo,
                                                                            originalDepositTransfer.getExchangeRate(),
                                                                            originalDepositTransfer.getAmount());

        // 결제 취소로 인한 입금 (출금 작업 완료 후 진행)
        accountBalanceService.increaseBalance(refundDepositAccount.getId(), origianlWithdrawalTransfer.getAmount());

        // 결제 취소에 대한 입금 내역 생성
        saveDepositTransferDetails(refundTransfer, origianlWithdrawalTransfer.getAmount(), refundDepositAccount.getBalance());

        return TransferResponseForPaymentDto.of(refundTransfer);
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
            .depositAccountId(depositAccountPublicInfo.accountId())
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
                    peerUserPublicInfo = userService.findUserPublicInfo(transfer.getDepositAccountId());
                } else if (transfer.getTransferType().equals(TransferType.DEPOSIT)) {
                    peerUserPublicInfo = userService.findUserPublicInfo(transfer.getWithdrawalAccountId());
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

        AccountPublicInfoDto withdrawalAccountPublicInfo = accountService.findAccountPublicInfo(transfer.getWithdrawalAccountId(), transfer);
        AccountPublicInfoDto depositAccountPublicInfo = accountService.findAccountPublicInfo(transfer.getDepositAccountId(), transfer);

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

    public TransferResponseForPaymentDto findTransfer(String transferGroupId, Long requesterId) {
        Transfer transfer =  transferRepository.findByTransferGroupIdAndTransferOwnerId(transferGroupId, requesterId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

        return TransferResponseForPaymentDto.of(transfer);
    }

    @FunctionalInterface
    public interface TransferResultHandler<T> {
        T handle(Transfer transfer, Account withdrawalAccount, AccountPublicInfoDto depositAccountPublicInfo);
    }
}
