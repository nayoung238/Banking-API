package SN.BANK.transaction.dto.response;

import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionFindResponse {

    private Long transactionId;

    private String accountNumber;

    private TransactionType transactionType;

    private LocalDateTime transactedAt;

    private BigDecimal amount; // 송금액

    private BigDecimal balance; // 이체(입금) 후 잔액

    public static TransactionFindResponse of(TransactionEntity tx, String accountNumber) {
        return TransactionFindResponse.builder()
                .transactionId(tx.getId())
                .accountNumber(accountNumber)
                .transactionType(tx.getType())
                .transactedAt(tx.getTransactedAt())
                .amount(tx.getAmount())
                .balance(tx.getBalance())
                .build();
    }
}
