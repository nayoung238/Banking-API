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
public class TransactionFindResponse {

    private Long transactionId;

    private String transactionName;

    private String accountNumber;

    private TransactionType transactionType;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    private LocalDateTime transactedAt;

    private BigDecimal amount; // 송금액

    private BigDecimal balance; // 이체(입금) 후 잔액

    @Builder
    public TransactionFindResponse(TransactionEntity tx, String accountNumber) {
        this.transactionId = tx.getId();
        this.transactionName = tx.getTransactionName();
        this.accountNumber = accountNumber;
        this.transactionType = tx.getType();
        this.transactedAt = tx.getTransactedAt();
        this.amount = tx.getAmount();
        this.balance = tx.getBalance();
    }
}
