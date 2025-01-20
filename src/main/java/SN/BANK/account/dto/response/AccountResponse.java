package SN.BANK.account.dto.response;

import SN.BANK.account.entity.Account;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AccountResponse {

    private String accountNumber;
    private BigDecimal money;
    private String accountName;

    @Builder
    public AccountResponse(Account account) {
        this.accountNumber = account.getAccountNumber();
        this.money = account.getMoney();
        this.accountName = account.getAccountName();
    }
}
