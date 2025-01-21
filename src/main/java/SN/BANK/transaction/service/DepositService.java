package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import SN.BANK.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {

    private final AccountService accountService;
    private final TransactionRecoverService recoverService;
    private final TransactionRepository transactionRepository;

    /**
     * 1. 받는(수취) 사람 계좌 조회 (수취 계좌 Lock 획득)
     * 1-1. 받는 사람 돈 증가
     * 1-1-1. 성공 시, 거래 내역(transaction) 생성
     * 1-1-2. 실패 시, 보낸(송금) 사람 계좌에 다시 감소된 돈 만큼 증가
     */
    @Transactional
    public TransactionResponse receiveFrom(TransactionRequest transactionRequest, Account senderAccount,
                                           BigDecimal exchangeRate, BigDecimal convertedAmount) {
        try {
            Account receiverAccount = accountService.getAccountWithLock(transactionRequest.getReceiverAccountId());
            receiverAccount.increaseMoney(convertedAmount);
            return createTransactionRecords(senderAccount, receiverAccount,
                    exchangeRate, transactionRequest.getAmount(), convertedAmount);
        } catch (Exception e) {
            recoverService.rollbackSenderAccount(transactionRequest);
            log.error("error: {}", e.getMessage());
            throw new CustomException(ErrorCode.RECEIVER_TRANSACTION_FAILED);
        }
    }

    private TransactionResponse createTransactionRecords(Account senderAccount, Account receiverAccount,
                                                         BigDecimal exchangeRate, BigDecimal amount, BigDecimal convertedAmount) {

        LocalDateTime transactedAt = LocalDateTime.now();
        String txGroupId = generateTransactionGroupId();

        TransactionEntity senderTx = TransactionEntity.builder()
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .type(TransactionType.WITHDRAWAL) // 출금
                .transactedAt(transactedAt)
                .amount(amount)
                .currency(senderAccount.getCurrency())
                .exchangeRate(exchangeRate)
                .balance(senderAccount.getMoney())
                .groupId(txGroupId)
                .build();

        TransactionEntity receiverTx = TransactionEntity.builder()
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .type(TransactionType.DEPOSIT) // 출금
                .transactedAt(transactedAt)
                .amount(convertedAmount)
                .currency(receiverAccount.getCurrency())
                .exchangeRate(exchangeRate)
                .balance(receiverAccount.getMoney())
                .groupId(txGroupId)
                .build();

        transactionRepository.save(senderTx);
        transactionRepository.save(receiverTx);

        return new TransactionResponse(senderTx, senderAccount.getUser().getName(), receiverAccount.getUser().getName());
    }

    private String generateTransactionGroupId() {
        return UUID.randomUUID().toString();
    }
}
