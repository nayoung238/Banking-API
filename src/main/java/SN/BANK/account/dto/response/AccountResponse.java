package SN.BANK.account.dto.response;

import SN.BANK.account.entity.Account;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AccountResponse {

    private String accountNumber;
    private BigDecimal money;
    private String accountName;

    public static AccountResponse of(Account account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .money(account.getMoney())
                .accountName(account.getAccountName())
                .build();
    }

}
