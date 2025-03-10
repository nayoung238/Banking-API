package banking.payment.dto.response;

import banking.payment.entity.Payment;
import banking.payment.enums.PaymentStatus;
import banking.transfer.dto.response.PaymentTransferDetailResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Schema(description = "결제 응답 DTO")
@Builder
public record PaymentView (

    @Schema(description = "결제 DB PK", example = "1")
    Long paymentId,

    @Schema(description = "결제 상태", example = "PAYMENT_COMPLETED")
    PaymentStatus paymentStatus,

    @Schema(description = "결제하는 계좌 번호", example = "5792214-80232581")
    String withdrawalAccountNumber,

    @Schema(description = "수취인 이름", example = "jisoo")
    String payeeName,

    @Schema(description = "결제 금액", example = "50000")
    BigDecimal amount,

    @Schema(description = "적용 환율", example = "1355.25")
    BigDecimal exchangeRate,

    @Schema(description = "통화", example = "KRW/USD")
    String currency,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    @Schema(description = "결제일", example = "2024.06.11 15:20")
    LocalDateTime paidAt
) {
    public static PaymentView of(Payment payment, PaymentTransferDetailResponse transferResponse,
                                 String withdrawalAccountNumber, String payeeName) {
        return PaymentView.builder()
            .paymentId(payment.getId())
            .paymentStatus(payment.getPaymentStatus())
            .withdrawalAccountNumber(withdrawalAccountNumber)
            .payeeName(payeeName)
            .amount(stripZeros(transferResponse.amount()))
            .exchangeRate(stripZeros(transferResponse.exchangeRate()))
            .currency(transferResponse.currency())
            .paidAt(transferResponse.createdAt())
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