package SN.BANK.account.dto.response;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateAccountResponse {

    private Long accountId;
    private String accountName;
    private String accountNumber;
    private Currency currency;
    private LocalDateTime createdAt;

    public static CreateAccountResponse of(Account account) {
        return CreateAccountResponse.builder()
                .accountName(account.getAccountName())
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .build();
    }

}
