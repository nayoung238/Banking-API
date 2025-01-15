package SN.BANK.payment.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;


@Getter
@Builder
public class PaymentRequestDto {


    @NotNull
    private Long withdrawId;
    @NotNull
    private Long depositId;
    @NotNull
    @Positive(message = "금액은 0보다 커야 합니다.")
    private BigDecimal amount;
    @NotBlank
    private String password;
}
