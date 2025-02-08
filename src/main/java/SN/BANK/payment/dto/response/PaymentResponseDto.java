package SN.BANK.payment.dto.response;

import SN.BANK.payment.entity.Payment;
import SN.BANK.payment.enums.PaymentStatus;
import SN.BANK.transfer.entity.Transfer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "결제 응답 DTO")
@Builder
public record PaymentResponseDto (

    @Schema(description = "결제 DB ID", example = "1")
    Long paymentId,

    @Schema(description = "결제 상태", example = "PAYMENT_COMPLETED")
    PaymentStatus paymentStatus,

    @Schema(description = "결제하는 계좌 번호", example = "5792214-80232581")
    String withdrawAccountNumber,

    @Schema(description = "입금자명", example = "홍길동")
    String receiverName,

    @Schema(description = "결제 금액", example = "50000")
    BigDecimal amount,

    @Schema(description = "적용 환율", example = "1355.25")
    BigDecimal exchangeRate,

    @Schema(description = "통화", example = "KRW/USD")
    String currency,

    @Schema(description = "결제일", example = "2019.05.20")
    LocalDateTime paidAt
) {
    public static PaymentResponseDto of(Payment payment, Transfer transfer, String withdrawAccountNumber, String receiverName) {
        return PaymentResponseDto.builder()
            .paymentId(payment.getId())
            .paymentStatus(payment.getPaymentStatus())
            .withdrawAccountNumber(withdrawAccountNumber)
            .receiverName(receiverName)
            .amount(stripZeros(transfer.getAmount()))
            .exchangeRate(stripZeros(transfer.getExchangeRate()))
            .currency(transfer.getCurrency())
            .paidAt(transfer.getCreatedAt())
            .build();
    }

    private static BigDecimal stripZeros(BigDecimal value) {
        return value.stripTrailingZeros();
    }
}