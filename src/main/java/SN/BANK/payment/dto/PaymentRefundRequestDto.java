package SN.BANK.payment.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class PaymentRefundRequestDto {

    @NotNull
    private Long depositId;

}
