package banking.payment.dto.response;

import banking.payment.entity.Payment;
import banking.payment.enums.PaymentStatus;
import banking.transfer.entity.Transfer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Schema(description = "결제 응답 DTO")
@Builder
public record PaymentResponseDto (

    @Schema(description = "결제 DB PK", example = "1")
    Long paymentId,

    @Schema(description = "결제 상태", example = "PAYMENT_COMPLETED")
    PaymentStatus paymentStatus,

    @Schema(description = "결제하는 계좌 번호", example = "5792214-80232581")
    String withdrawAccountNumber,

    @Schema(description = "수취인 이름", example = "홍길동")
    String payeeName,

    @Schema(description = "결제 금액", example = "50000")
    BigDecimal amount,

    @Schema(description = "적용 환율", example = "1355.25")
    BigDecimal exchangeRate,

    @Schema(description = "통화", example = "KRW/USD")
    String currency,

    @Schema(description = "결제일", example = "2019.05.20")
    LocalDateTime paidAt
) {
    public static PaymentResponseDto of(Payment payment, Transfer transfer, String withdrawAccountNumber, String payeeName) {
        return PaymentResponseDto.builder()
            .paymentId(payment.getId())
            .paymentStatus(payment.getPaymentStatus())
            .withdrawAccountNumber(withdrawAccountNumber)
            .payeeName(payeeName)
            .amount(stripZeros(transfer.getAmount()))
            .exchangeRate(stripZeros(transfer.getExchangeRate()))
            .currency(transfer.getCurrency())
            .paidAt(transfer.getCreatedAt())
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