package SN.BANK.payment.dto.response;

import SN.BANK.account.enums.Currency;
import SN.BANK.payment.entity.PaymentList;
import SN.BANK.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "결제조회 응답Dto")
public class PaymentListResponseDto {

    @Schema(description = "결제의 데이터베이스 id 값", example = "1")
    private Long id;
    @Schema(description = "결제하는 계좌 번호", example = "579221480232581")
    private String withdrawAccountNumber;
    @Schema(description = "입금받는 계좌 번호", example = "219781005875125")
    private String depositAccountNumber;
    @Schema(description = "결제액", example = "50000")
    private String amount;
    @Schema(description = "결제일", example = "2019.05.20")
    private LocalDateTime paidAt;
    @Schema(description = "통화", example = "KRW")
    private Currency currency;
    @Schema(description = "환율", example = "1355.25")
    private String exchangeRate;
    @Schema(description = "결제 상태", example = "PAYMENT_COMPLETED")
    private PaymentStatus paymentStatus;

    public static PaymentListResponseDto of(PaymentList paymentList){

        return new PaymentListResponseDto(
                paymentList.getId(),
                paymentList.getWithdrawAccountNumber(),
                paymentList.getDepositAccountNumber(),
                formatBigDecimal(paymentList.getAmount()),
                paymentList.getPaidAt(),
                paymentList.getCurrency(),
                formatBigDecimal(paymentList.getExchangeRate()),
                paymentList.getPaymentStatus()
        );
    }

    private static String formatBigDecimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
