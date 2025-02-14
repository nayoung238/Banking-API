package banking.payment.entity;

import banking.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String transferGroupId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    public void updatePaymentStatus(PaymentStatus newStatus){
        this.paymentStatus = newStatus;
    }
}
