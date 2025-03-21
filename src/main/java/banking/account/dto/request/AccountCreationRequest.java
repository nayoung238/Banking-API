package banking.account.dto.request;

import banking.account.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "계좌 생성 요청 DTO")
public record AccountCreationRequest (

    @NotEmpty
    @Schema(description = "비밀번호", example = "12345")
    String password,

    @NotNull
    @Schema(description = "통화", example = "KRW")
    Currency currency,

    @Schema(description = "계좌 이름", example = "청년저축통장")
    String accountName
) { }
