package SN.BANK.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRefundRequestDto {

    @NotNull
    private Long paymentId;

    @NotNull
    private String password;

}
