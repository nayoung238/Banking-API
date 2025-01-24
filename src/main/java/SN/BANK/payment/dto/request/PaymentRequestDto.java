package SN.BANK.payment.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 요청 Dto")
public class PaymentRequestDto {


    @NotBlank
    @Schema(description = "결제하는 계좌 번호", example = "579221480232581")
    private String withdrawAccountNumber;
    @NotBlank
    @Schema(description = "입금받는 계좌 번호", example = "219781005875125")
    private String depositAccountNumber;
    @NotNull
    @Positive(message = "금액은 0보다 커야 합니다.")
    @Schema(description = "결제액", example = "50000")
    private BigDecimal amount;  //상대 계좌 통화기준
    @NotBlank
    @Schema(description = "결제계좌 비밀번호", example = "12345")
    private String password;
}
