package SN.BANK.account.dto.request;

import SN.BANK.account.enums.Currency;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateAccountRequest {

    @NotEmpty
    private String password;

    @NotNull
    private Currency currency;

    private String accountName;

    @Builder
    public CreateAccountRequest(String password, Currency currency, String accountName) {
        this.password = password;
        this.currency = currency;
        this.accountName = accountName;
    }
}
