package SN.BANK.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Schema(description = "환불 요청 Dto")
@Builder
public record PaymentRefundRequestDto (

    @NotNull
    @Schema(description = "결제 DB ID", example = "1")
    Long paymentId,

    @NotNull
    @Schema(description = "결제 계좌 비밀번호", example = "12345")
    String password
) { }
