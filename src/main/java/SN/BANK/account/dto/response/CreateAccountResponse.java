package SN.BANK.account.dto.response;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CreateAccountResponse {

    private Long accountId;
    private String accountName;
    private String accountNumber;
    private Currency currency;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    private LocalDateTime createdAt;

    @Builder
    public CreateAccountResponse(Account account) {
        this.accountId = account.getId();
        this.accountName = account.getAccountName();
        this.accountNumber = account.getAccountNumber();
        this.currency = account.getCurrency();
        this.createdAt = account.getCreatedAt();
    }
}
