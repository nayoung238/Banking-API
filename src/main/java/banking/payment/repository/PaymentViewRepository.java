package banking.payment.repository;

import banking.payment.dto.response.PaymentView;
import banking.payment.entity.Payment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentViewRepository extends CrudRepository<Payment, Long> {

	@Query(
		"SELECT new banking.payment.dto.response.PaymentView (" +
			"p.id, p.paymentStatus, " +
			"a.accountNumber, " +
			"COALESCE(u.name, '탈퇴한 사용자'), " +
			"t.amount, t.exchangeRate, t.currency, t.createdAt) " +
			"FROM Payment p " +
			"INNER JOIN Transfer t ON p.transferId = t.id " +
			"INNER JOIN Account a ON t.withdrawalAccountId = a.id " +
			"LEFT JOIN User u ON p.payeeId = u.id " +
			"WHERE p.id = :paymentId"
	)
	Optional<PaymentView> findByPaymentId(@Param("paymentId") Long paymentId);
}
