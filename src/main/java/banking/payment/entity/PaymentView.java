package banking.payment.entity;

import banking.payment.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Immutable
@Subselect(
	"SELECT " +
		"p.id AS payment_id, " +
		"p.payment_status AS payment_status, " +
		"a.account_number AS withdrawal_account_number, " +
		"u.name AS payee_name, " +
		"t.amount AS amount, " +
		"t.exchange_rate AS exchange_rate, " +
		"t.currency AS currency, " +
		"t.created_at AS paid_at " +
		"FROM payment p " +
		"LEFT JOIN users u ON p.payee_id = u.id " +
		"LEFT JOIN transfer t ON p.transfer_id = t.id " +
		"LEFT JOIN account a ON t.withdrawal_account_id = a.id"
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentView {

	@Id
	@Column(name = "payment_id")
	Long paymentId;

	@Column(name = "payment_status")
	@Enumerated(EnumType.STRING)
	PaymentStatus paymentStatus;

	@Column(name = "withdrawal_account_number")
	String withdrawalAccountNumber;

	@Column(name = "payee_name")
	String payeeName;

	@Column(name = "amount", precision = 38, scale = 2)
	BigDecimal amount;

	@Column(name = "exchange_rate", precision = 38, scale = 2)
	BigDecimal exchangeRate;

	@Column(name = "currency")
	String currency;

	@Column(name = "paid_at")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
	LocalDateTime paidAt;
}
