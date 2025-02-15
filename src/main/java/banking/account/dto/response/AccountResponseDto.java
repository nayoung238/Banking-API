package banking.account.dto.response;

import banking.account.entity.Account;
import banking.account.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Builder
@Schema(description = "계좌 정보 응답 DTO")
public record AccountResponseDto (

    @Schema(description = "계좌 DB PK", example = "1")
    Long accountId,

    @Schema(description = "계좌 번호", example = "5618752-3157985")
    String accountNumber,

    @Schema(description = "통화", example = "KRW")
    Currency currency,

    @Schema(description = "잔액", example = "10000")
    BigDecimal balance,

    @Schema(description = "계좌 이름", example = "청년저축통장")
    String accountName
) {

    public static AccountResponseDto of(Account account) {
        return AccountResponseDto.builder()
            .accountId(account.getId())
            .accountNumber(account.getAccountNumber())
            .currency(account.getCurrency())
            .balance(stripZeros(account.getBalance()))
            .accountName(account.getAccountName())
            .build();
    }

    private static BigDecimal stripZeros(BigDecimal value) {
        BigDecimal strippedValue = value.stripTrailingZeros();

        if (strippedValue.scale() <= 0) {
            return strippedValue.setScale(0, RoundingMode.DOWN);
        }
        return strippedValue;
    }
}
