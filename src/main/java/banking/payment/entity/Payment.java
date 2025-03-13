package banking.payment.entity;

import banking.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Payment", indexes = {
    @Index(name = "idx_payer_id", columnList = "payer_id")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "payer_id")
    private Long payerId;

    @Column(nullable = false, name = "payee_id")
    private Long payeeId;

    @Column(nullable = false, unique = true, name = "transfer_id")
    private Long transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "payment_status")
    private PaymentStatus paymentStatus;

    public void updatePaymentStatus(PaymentStatus newStatus){
        this.paymentStatus = newStatus;
    }
}
