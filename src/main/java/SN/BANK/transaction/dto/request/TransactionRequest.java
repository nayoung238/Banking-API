package SN.BANK.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TransactionRequest {

    @NotNull
    private String accountPassword;

    @NotNull
    private Long senderAccountId;

    @NotNull
    private Long receiverAccountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야합니다.")
    private BigDecimal amount;
}
