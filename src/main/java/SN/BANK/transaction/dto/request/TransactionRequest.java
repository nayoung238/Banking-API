package SN.BANK.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Schema(description = "거래 요청 Dto")
public class TransactionRequest {

    @NotNull
    @Schema(description = "송금계좌 비밀번호", example = "12345")
    private String accountPassword;

    @NotNull
    @Schema(description = "송금하는 계좌 번호", example = "579221480232581")
    private Long senderAccountId;

    @NotNull
    @Schema(description = "입금받는 계좌 번호", example = "219781005875125")
    private Long receiverAccountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야합니다.")
    @Schema(description = "거래액", example = "50000")
    private BigDecimal amount; // 원화 -> 외화일 시, 외화 기준으로 보내야 함.

    @Builder
    public TransactionRequest(String accountPassword, Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
        this.accountPassword = accountPassword;
        this.senderAccountId = senderAccountId;
        this.receiverAccountId = receiverAccountId;
        this.amount = amount;
    }
}
