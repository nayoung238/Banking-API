package SN.BANK.transfer.dto.response;

import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.enums.TransferType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "이체 응답 DTO")
@Builder
public record TransferResponseDto(

    @Schema(description = "거래 ID", example = "1")
    Long transferId,

    @Schema(description = "출금 계좌번호", example = "5792214-80232581")
    String withdrawalAccountNumber,

    @Schema(description = "출금자명", example = "홍길동")
    String senderName,

    @Schema(description = "출금 계좌번호", example = "2197810-05875125")
    String depositAccountNumber,

    @Schema(description = "입금자명", example = "전우치")
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
    BigDecimal balance,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Schema(description = "거래일", example = "2024.06.11 15:20")
    LocalDateTime createdAt

) {
    public static TransferResponseDto of(Transfer transfer, TransferType transferType,
                                         String withdrawalAccountNumber, String senderName,
                                         String depositAccountNumber, String receiverName) {
        return TransferResponseDto.builder()
            .transferId(transfer.getId())
            .withdrawalAccountNumber(withdrawalAccountNumber)
            .senderName(senderName)
            .depositAccountNumber(depositAccountNumber)
            .receiverName(receiverName)
            .transferType(transferType)
            .exchangeRate(stripZeros(transfer.getExchangeRate()))
            .currency(transfer.getCurrency())
            .createdAt(transfer.getCreatedAt())
            .amount(stripZeros(transfer.getAmount()))
            .balance(stripZeros(transfer.getBalance()))
            .build();
    }

    private static BigDecimal stripZeros(BigDecimal value) {
        return value.stripTrailingZeros();
    }
}
