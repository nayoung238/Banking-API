package banking.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DepositRequestDto (

	@NotBlank(message = "입금 계좌번호는 필수입니다.")
	@Schema(description = "입금 계좌번호",example = "5618752-3157985")
	String accountNumber,

	@NotBlank(message = "입금 계좌 비밀번호는 필수입니다.")
	@Schema(description = "입금 계좌 비밀번호",example = "965618")
	String accountPassword,

	@NotNull(message = "입금 금액은 필수입니다.")
	@Schema(description = "입금 금액", example = "10000")
	BigDecimal amount
) { }
