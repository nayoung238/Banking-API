package SN.BANK.transaction.dto.response;

import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionResponse {

    private Long transactionId;

    private Long senderAccountId;

    private Long receiverAccountId;

    private TransactionType transactionType;

    private LocalDateTime transactedAt;

    private BigDecimal amount; // 송금액

    private BigDecimal balance; // 이체(입금) 후 잔액

    public static TransactionResponse of(TransactionEntity tx) {
        return TransactionResponse.builder()
                .transactionId(tx.getId())
                .senderAccountId(tx.getSenderAccountId())
                .receiverAccountId(tx.getReceiverAccountId())
                .transactionType(tx.getType())
                .transactedAt(tx.getTransactedAt())
                .amount(tx.getAmount())
                .balance(tx.getBalance())
                .build();
    }
}
