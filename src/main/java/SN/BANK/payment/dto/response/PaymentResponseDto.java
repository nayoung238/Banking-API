package SN.BANK.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "결제 응답Dto")
public class PaymentResponseDto {
    @Schema(description = "결제의 데이터베이스 id 값", example = "1")
    private Long paymentId;
}