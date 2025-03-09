package banking.payment.repository;

import banking.payment.entity.PaymentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentViewRepository extends JpaRepository<PaymentView, Long> {

	Optional<PaymentView> findByPaymentId(Long paymentId);
}
