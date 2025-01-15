package SN.BANK.payment.dto;

import SN.BANK.payment.entity.PaymentList;
import SN.BANK.domain.enums.Currency;
import SN.BANK.domain.enums.PaymentTag;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PaymentListResponseDto {

    private Long id;
    private LocalDateTime paidAt;
    private PaymentTag paymentTag;
    private Long depositId;
    private Long withdrawId;
    private BigDecimal amount;
    private BigDecimal exchangeRate;
    private Currency currency;

    public static PaymentListResponseDto of(PaymentList paymentList){
        return new PaymentListResponseDto(
                paymentList.getId(),
                paymentList.getPaidAt(),
                paymentList.getPaymentTag(),
                paymentList.getDepositId(),
                paymentList.getWithdrawId(),
                paymentList.getAmount(),
                paymentList.getExchangeRate(),
                paymentList.getCurrency()
        );

    }
}
