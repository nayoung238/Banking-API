package SN.BANK.transaction.dto.response;

import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionFindDetailResponse {

    private TransactionType transactionType;

    private LocalDateTime transactedAt;

    private String othersAccountNumber;

    private BigDecimal amount;

    private BigDecimal balance;

    private String description;

    public static TransactionFindDetailResponse of(TransactionEntity tx, String othersAccountNumber) {
        return TransactionFindDetailResponse.builder()
                .transactionType(tx.getType())
                .transactedAt(tx.getTransactedAt())
                .othersAccountNumber(othersAccountNumber)
                .amount(tx.getAmount())
                .balance(tx.getBalance())
                .description(tx.getDescription())
                .build();
    }
}
