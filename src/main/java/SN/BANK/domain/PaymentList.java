package SN.BANK.domain;

import SN.BANK.domain.enums.Currency;
import SN.BANK.domain.enums.PaymentTag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class PaymentList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    private PaymentTag paymentTag;

    private Long depositId;

    private Long withdrawId;

    private Long amount;

    private Double exchangeRate;

    @Enumerated(EnumType.STRING)
    private Currency currency;
}
