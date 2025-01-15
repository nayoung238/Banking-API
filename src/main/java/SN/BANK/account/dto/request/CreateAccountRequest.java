package SN.BANK.account.dto.request;

import SN.BANK.domain.enums.Currency;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateAccountRequest {

    @NotEmpty
    private String password;

    @NotNull
    private Currency currency;

    private String accountName;
}
