package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import SN.BANK.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionCreateService {

    private final TransactionRepository transactionRepository;

    public TransactionResponse createTransactionRecords(Account senderAccount, Account receiverAccount,
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
