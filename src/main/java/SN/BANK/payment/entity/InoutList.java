package SN.BANK.payment.entity;

import SN.BANK.payment.enums.InoutTag;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@NoArgsConstructor
@Getter
public class InoutList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @Setter
    @JoinColumn(name = "paymentList_id", nullable = false)
    private PaymentList paymentList;

    @Enumerated(EnumType.STRING)
    private InoutTag inoutTag;

    private BigDecimal amount;

    private BigDecimal balance;

    private Long accountId;

    @Builder
    public InoutList(InoutTag inoutTag, BigDecimal amount, BigDecimal balance, Long accountId) {
        this.inoutTag = inoutTag;
        this.amount = amount;
        this.balance = balance;
        this.accountId = accountId;
    }
}
