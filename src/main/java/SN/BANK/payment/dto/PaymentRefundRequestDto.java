package SN.BANK.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class PaymentRefundRequestDto {

    @NotNull
    private Long depositId;

}
