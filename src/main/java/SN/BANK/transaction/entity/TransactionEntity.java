package SN.BANK.transaction.entity;

import SN.BANK.account.enums.Currency;
import SN.BANK.transaction.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionName;

    private Long senderAccountId;

    private Long receiverAccountId;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private LocalDateTime transactedAt;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private BigDecimal exchangeRate;

    private BigDecimal balance;

    @Column(name = "transaction_group_id", nullable = false)
    private String groupId;

    private String description;

    @Builder
    public TransactionEntity(String transactionName, Long senderAccountId, Long receiverAccountId,
                             TransactionType type, LocalDateTime transactedAt, BigDecimal amount,
                             Currency currency, BigDecimal exchangeRate, BigDecimal balance, String groupId,
                             String description) {
        this.transactionName = transactionName;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.type = type;
        this.transactedAt = transactedAt;
        this.amount = amount;
        this.currency = currency;
        this.exchangeRate = exchangeRate;
        this.balance = balance;
        this.groupId = groupId;
        this.description = description;
    }
}
