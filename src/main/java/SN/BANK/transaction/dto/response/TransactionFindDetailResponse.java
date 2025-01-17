package SN.BANK.transaction.dto.response;

import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class TransactionFindDetailResponse {

    private TransactionType transactionType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    private LocalDateTime transactedAt;

    private String othersAccountNumber;

    private BigDecimal amount;

    private BigDecimal balance;

    private String description;

    @Builder
    public TransactionFindDetailResponse(TransactionEntity tx, String othersAccountNumber) {
        this.transactionType = tx.getType();
        this.transactedAt = tx.getTransactedAt();
        this.othersAccountNumber = othersAccountNumber;
        this.amount = tx.getAmount();
        this.balance = tx.getBalance();
        this.description = tx.getDescription();
    }
}
