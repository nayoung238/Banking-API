package SN.BANK.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Schema(description = "결제 취소 응답 DTO")
@Builder
public record RefundPaymentResponseDto (

	@NotBlank
	@Schema(description = "입금 계좌번호", example = "230492-5894257")
	String depositAccountNumber,

	@NotNull
	@Schema(description = "결제 취소로 인한 입금 금액", example = "10000")
	BigDecimal depositAmount
) {

	public static RefundPaymentResponseDto of(String depositAccountNumber, BigDecimal depositAmount) {
		return RefundPaymentResponseDto.builder()
			.depositAccountNumber(depositAccountNumber)
			.depositAmount(stripZeros(depositAmount))
			.build();
	}

	private static BigDecimal stripZeros(BigDecimal value) {
		BigDecimal strippedValue = value.stripTrailingZeros();

		if (strippedValue.scale() <= 0) {
			return strippedValue.setScale(0, RoundingMode.DOWN);
		}
		return strippedValue;
	}
}
