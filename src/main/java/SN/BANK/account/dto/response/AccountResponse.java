package SN.BANK.account.dto.response;

import SN.BANK.account.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "계좌 정보 응답Dto")
public record AccountResponse (
    @Schema(description = "계좌 번호",example = "56187523157985")
    String accountNumber,

    @Schema(description = "잔액",example = "10000")
    BigDecimal money,

    @Schema(description = "계좌 이름", example = "청년저축통장")
    String accountName
) {
    public static AccountResponse of(Account account) {
        return AccountResponse.builder()
            .accountNumber(account.getAccountNumber())
            .money(account.getMoney())
            .accountNumber(account.getAccountNumber())
            .build();
    }
}
