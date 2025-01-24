package SN.BANK.account.dto.response;

import SN.BANK.account.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Schema(description = "계좌 정보 응답Dto")
public class AccountResponse {
    @Schema(description = "계좌 번호",example = "56187523157985")
    private String accountNumber;
    @Schema(description = "잔액",example = "10000")
    private BigDecimal money;
    @Schema(description = "계좌 이름", example = "청년저축통장")
    private String accountName;

    @Builder
    public AccountResponse(Account account) {
        this.accountNumber = account.getAccountNumber();
        this.money = account.getMoney();
        this.accountName = account.getAccountName();
    }
}
