package banking.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Schema(description = "결제 취소 요청 DTO")
@Builder
public record PaymentRefundRequestDto (

    @NotNull
    @Schema(description = "결제 DB ID", example = "1")
    Long paymentId,

    @NotBlank
    @Schema(description = "결제 계좌 비밀번호", example = "12345")
    String withdrawalAccountPassword
) { }
