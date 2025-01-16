package SN.BANK.transfer.dto.request;

import SN.BANK.account.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransferRequest {

    @NotNull
    private Long fromAccountId;

    @NotNull
    private String accountPassword;

    @NotNull
    private Long toAccountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야합니다.")
    private BigDecimal amount;

    @NotNull
    private Currency currency;
}
