package banking.transfer.dto.response;

import banking.account.dto.response.AccountPublicInfoResponse;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Schema(description = "이체 응답 DTO")
@Builder
public record TransferDetailResponse (

    @Schema(description = "이체 DB PK", example = "1")
    Long transferId,

    @Schema(description = "출금 계좌번호", example = "5792214-80232581")
    String withdrawalAccountNumber,

    @Schema(description = "출금처", example = "토스뱅크")
    String senderName,

    @Schema(description = "입금 계좌번호", example = "2197810-05875125")
    String depositAccountNumber,

    @Schema(description = "입금처", example = "네이버")
    String receiverName,

    @Schema(description = "이체 타입", example = "DEPOSIT, WITHDRAWAL")
    TransferType transferType,

    @Schema(description = "적용 환율", example = "1355.25")
    BigDecimal exchangeRate,

    @Schema(description = "통화", example = "KRW/USD")
    String currency,

    @Schema(description = "거래액", example = "20000")
    BigDecimal amount,

    @Schema(description = "거래 후 잔액", example = "8000")
    BigDecimal balancePostTransaction,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Schema(description = "거래일", example = "2024.06.11 15:20")
    LocalDateTime createdAt

) {
    public static TransferDetailResponse of(Transfer transfer,
                                            AccountPublicInfoResponse withdrawalAccountPublicInfo,
                                            AccountPublicInfoResponse depositAccountPublicInfo) {
        return TransferDetailResponse.builder()
            .transferId(transfer.getId())
            .withdrawalAccountNumber(withdrawalAccountPublicInfo.accountNumber())
            .senderName(withdrawalAccountPublicInfo.ownerName())
            .depositAccountNumber(depositAccountPublicInfo.accountNumber())
            .receiverName(depositAccountPublicInfo.ownerName())
            .transferType(transfer.getTransferType())
            .exchangeRate(stripZeros(transfer.getExchangeRate()))
            .currency(transfer.getCurrency())
            .createdAt(transfer.getCreatedAt())
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
