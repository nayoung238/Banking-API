package SN.BANK.payment.entity;


import SN.BANK.account.enums.Currency;
import SN.BANK.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


    @Entity
    @Getter
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

        @Builder
        public PaymentList(
                String withdrawAccountNumber
                ,String depositAccountNumber
                ,BigDecimal amount
                ,LocalDateTime paidAt
                ,Currency currency
                ,BigDecimal exchangeRate
                ,PaymentStatus paymentStatus){
            this.withdrawAccountNumber = withdrawAccountNumber;
            this.depositAccountNumber = depositAccountNumber;
            this.amount = amount;
            this.paidAt = paidAt;
            this.currency = currency;
            this.exchangeRate = exchangeRate;
            this.paymentStatus = paymentStatus;
        }

        public void updatePaymentStatus(PaymentStatus newStatus){
            this.paymentStatus = newStatus;
        }
    }
