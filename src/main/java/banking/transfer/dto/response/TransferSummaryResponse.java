package banking.transfer.dto.response;

import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Schema(description = "이체 조회 목록용 응답 DTO")
@Builder
public record TransferSummaryResponse (

    @Schema(description = "이체 DB PK", example = "1")
    Long transferId,

    @Schema(description = "거래 타입", example = "DEPOSIT")
    TransferType transferType,

    @Schema(description = "거래 상대 이름", example = "카카오 선물하기")
    String peerName,

    @Schema(description = "거래일", example = "2025.06.11 15:20")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    LocalDateTime transactedAt,

    @Schema(description = "거래액", example = "2000")
    BigDecimal amount,

    @Schema(description = "거래 후 잔액", example = "8000")
    BigDecimal balancePostTransaction
) {

    public static TransferSummaryResponse of(Transfer transfer, String peerName) {
        return TransferSummaryResponse.builder()
            .transferId(transfer.getId())
            .transferType(transfer.getTransferType())
            .peerName(peerName)
            .transactedAt(transfer.getCreatedAt())
            .amount(stripZeros(transfer.getAmount()))
            .balancePostTransaction(stripZeros(transfer.getBalancePostTransaction()))
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
