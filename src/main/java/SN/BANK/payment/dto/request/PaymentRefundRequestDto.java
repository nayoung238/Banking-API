package SN.BANK.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "환불 요청 Dto")
public class PaymentRefundRequestDto {

    @NotNull
    @Schema(description = "결제의 데이터베이스 id 값", example = "1")
    private Long paymentId;

    @NotNull
    @Schema(description = "결제한 계좌의 비밀번호", example = "12345")
    private String password;

}
