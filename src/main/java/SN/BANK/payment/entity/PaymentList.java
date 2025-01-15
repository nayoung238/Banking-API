package SN.BANK.payment.entity;

import SN.BANK.account.enums.Currency;
import SN.BANK.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class PaymentList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String withdrawAccountNumber;
    private String depositAccountNumber;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    @Enumerated(EnumType.STRING)
    private Currency currency;
    @Column(precision = 38, scale = 10)
    private BigDecimal exchangeRate;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
}
