package SN.BANK.payment.entity;

import SN.BANK.account.enums.Currency;
import SN.BANK.payment.enums.PaymentTag;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
public class PaymentList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    private PaymentTag paymentTag;

    private Long depositId;

    private Long withdrawId;

    private BigDecimal amount;

    private BigDecimal exchangeRate;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @OneToMany(mappedBy = "paymentList", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<InoutList> inoutLists = new ArrayList<>();

    @Builder
    public PaymentList(PaymentTag paymentTag, Long depositId, Long withdrawId,
                       BigDecimal amount, BigDecimal exchangeRate, Currency currency) {
        this.paymentTag = paymentTag;
        this.depositId = depositId;
        this.withdrawId = withdrawId;
        this.amount = amount;
        this.exchangeRate = exchangeRate;
        this.currency = currency;
    }

    public void addInoutList(InoutList inoutList) {
        this.inoutLists.add(inoutList);
        inoutList.setPaymentList(this);
    }

}
