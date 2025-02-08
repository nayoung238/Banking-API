package SN.BANK.transfer.dto.response;

import SN.BANK.account.entity.Account;
import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.enums.TransferType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "이체 응답 DTO")
@Builder
public record TransferDetailsResponseDto (

    @Schema(description = "거래 ID", example = "1")
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
    public static TransferDetailsResponseDto of(Transfer transfer, TransferType transferType,
                                                Account withdrawalAccount, Account depositAccount) {
        return TransferDetailsResponseDto.builder()
            .transferId(transfer.getId())
            .withdrawalAccountNumber(withdrawalAccount.getAccountNumber())
            .senderName(withdrawalAccount.getUser().getName())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .receiverName(depositAccount.getUser().getName())
            .transferType(transferType)
            .exchangeRate(stripZeros(transfer.getExchangeRate()))
            .currency(transfer.getCurrency())
            .createdAt(transfer.getCreatedAt())
            .amount(stripZeros(transfer.getTransferDetails().get(transferType).getAmount()))
            .balancePostTransaction(stripZeros(transfer.getTransferDetails().get(transferType).getBalancePostTransaction()))
            .build();
    }

    private static BigDecimal stripZeros(BigDecimal value) {
        return value.stripTrailingZeros();
    }
}
