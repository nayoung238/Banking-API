package SN.BANK.payment.dto.response;

import SN.BANK.domain.enums.PaymentStatus;
import SN.BANK.payment.entity.PaymentList;
import SN.BANK.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentListResponseDto {

    private Long id;
    private String withdrawAccountNumber;
    private String depositAccountNumber;
    private String amount;
    private LocalDateTime paidAt;
    private Currency currency;
    private String exchangeRate;
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
