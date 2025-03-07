package banking.transfer.dto.response;

import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentTransferDetailResponse (

	String transferGroupId,
	TransferType transferType,
	Long withdrawalAccountId,
	Long depositAccountId,
	BigDecimal amount,
	BigDecimal exchangeRate,
	String currency,
	LocalDateTime createdAt
) {

	public static PaymentTransferDetailResponse of(Transfer transfer) {
		return PaymentTransferDetailResponse.builder()
			.transferGroupId(transfer.getTransferGroupId())
			.transferType(transfer.getTransferType())
			.withdrawalAccountId(transfer.getWithdrawalAccountId())
			.depositAccountId(transfer.getDepositAccountId())
			.amount(transfer.getAmount())
			.exchangeRate(transfer.getExchangeRate())
			.currency(transfer.getCurrency())
			.createdAt(transfer.getCreatedAt())
			.build();
	}
}
