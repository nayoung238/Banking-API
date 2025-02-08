package SN.BANK.transfer.entity;

import SN.BANK.account.enums.Currency;
import SN.BANK.transfer.enums.TransferType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
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
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionName;

    private Long senderAccountId;

    private Long receiverAccountId;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferType type;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
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
    public Transfer(String transactionName, Long senderAccountId, Long receiverAccountId,
                    TransferType type, LocalDateTime transactedAt, BigDecimal amount,
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
