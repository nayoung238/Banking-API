package banking.transfer.entity;

import banking.common.entity.BaseTimeEntity;
import banking.transfer.enums.TransferType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String transferGroupId;

    @Column(nullable = false)
    private Long transferOwnerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferType transferType;

    @Column(nullable = false)
    private Long withdrawalAccountId;

    @Column(nullable = false)
    private Long depositAccountId;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal balancePostTransaction;
}
