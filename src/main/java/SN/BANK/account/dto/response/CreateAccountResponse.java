package SN.BANK.account.dto.response;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "계좌 생성 응답Dto")
public class CreateAccountResponse {
    @Schema(description = "계좌의 데이터베이스 id 값",example = "1")
    private Long accountId;
    @Schema(description = "계좌 이름", example = "청년저축통장")
    private String accountName;
    @Schema(description = "계좌 번호",example = "56187523157985")
    private String accountNumber;
    @Schema(description = "통화",example = "KRW")
    private Currency currency;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Schema(description = "생성일",example = "2020.05.11 12:30")
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
