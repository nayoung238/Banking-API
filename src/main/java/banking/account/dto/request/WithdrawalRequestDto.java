package banking.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawalRequestDto (

	@NotBlank(message = "출금 계좌번호는 필수입니다.")
	@Schema(description = "출금 계좌번호",example = "5618752-3157985")
	String accountNumber,

	@NotBlank(message = "출금 계좌 비밀번호는 필수입니다.")
	@Schema(description = "출금 계좌 비밀번호",example = "965618")
	String accountPassword,

	@NotNull(message = "출금 금액은 필수입니다.")
	@Schema(description = "출금 금액", example = "10000")
	BigDecimal amount
) {
}
