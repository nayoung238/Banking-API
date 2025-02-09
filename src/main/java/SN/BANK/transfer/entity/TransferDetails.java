package SN.BANK.transfer.entity;

import SN.BANK.transfer.enums.TransferType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "transfer_id", nullable = false)
	private Transfer transfer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransferType type;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(nullable = false)
	private BigDecimal balancePostTransaction;
}
