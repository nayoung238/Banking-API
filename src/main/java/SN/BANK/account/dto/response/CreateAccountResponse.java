package SN.BANK.account.dto.response;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "계좌 생성 응답Dto")
public record CreateAccountResponse (
    @Schema(description = "계좌의 데이터베이스 id 값",example = "1")
    Long accountId,

    @Schema(description = "계좌 이름", example = "청년저축통장")
    String accountName,

    @Schema(description = "계좌 번호",example = "56187523157985")
    String accountNumber,

    @Schema(description = "통화",example = "KRW")
    Currency currency,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Schema(description = "생성일",example = "2020.05.11 12:30")
    LocalDateTime createdAt
) {
    public static CreateAccountResponse of(Account account) {
        return CreateAccountResponse.builder()
            .accountId(account.getId())
            .accountName(account.getAccountName())
            .accountNumber(account.getAccountNumber())
            .currency(account.getCurrency())
            .currency(account.getCurrency())
            .build();
    }
}
