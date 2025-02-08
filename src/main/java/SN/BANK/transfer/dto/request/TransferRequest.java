package SN.BANK.transfer.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "거래 요청 Dto")
public record TransferRequest(
    @NotNull
    @Schema(description = "송금계좌 비밀번호", example = "12345")
    String accountPassword,

    @NotNull
    @Schema(description = "송금하는 계좌 번호", example = "579221480232581")
    Long senderAccountId,

    @NotNull
    @Schema(description = "입금받는 계좌 번호", example = "219781005875125")
    Long receiverAccountId,

    @NotNull
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야합니다.")
    @Schema(description = "거래액", example = "50000")
    BigDecimal amount // 원화 -> 외화일 시, 외화 기준으로 보내야 함.
) {
}
