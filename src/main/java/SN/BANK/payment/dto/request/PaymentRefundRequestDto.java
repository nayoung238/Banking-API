package SN.BANK.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "환불 요청 Dto")
public record PaymentRefundRequestDto (
    @NotNull
    @Schema(description = "결제의 데이터베이스 id 값", example = "1")
    Long paymentId,

    @NotNull
    @Schema(description = "결제한 계좌의 비밀번호", example = "12345")
    String password
) { }
