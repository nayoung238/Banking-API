package SN.BANK.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "결제 요청 Dto")
public record PaymentRequestDto (

    @NotBlank
    @Schema(description = "결제 계좌번호", example = "5792214-80232581")
    String withdrawalAccountNumber,

    @NotBlank
    @Schema(description = "결제계좌 비밀번호", example = "12345")
    String withdrawalAccountPassword,

    @NotBlank
    @Schema(description = "입금 계좌번호", example = "2197810-05875125")
    String depositAccountNumber,

    @NotNull
    @Positive(message = "금액은 0보다 커야 합니다.")
    @Schema(description = "결제액", example = "50000")
    BigDecimal amount  // Deposit Account 통화 기준
) { }
