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


    @NotBlank
    private String withdrawAccountNumber;
    @NotBlank
    private String depositAccountNumber;
    @NotNull
    @Positive(message = "금액은 0보다 커야 합니다.")
    private BigDecimal amount;  //상대 계좌 통화기준
    @NotBlank
    private String password;
}
