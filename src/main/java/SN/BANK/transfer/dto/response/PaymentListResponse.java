package SN.BANK.transfer.dto.response;

import SN.BANK.payment.entity.PaymentList;
import SN.BANK.payment.enums.PaymentTag;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentListResponse {

    private PaymentTag paymentTag;

    private Long depositId;

    private Long withdrawId;

    private BigDecimal amount; // 입금액

    private BigDecimal balance; // 이체(입금) 후 잔액

    public static PaymentListResponse of(PaymentList paymentList, BigDecimal balance) {

        return PaymentListResponse.builder()
                .paymentTag(paymentList.getPaymentTag())
                .depositId(paymentList.getDepositId())
                .withdrawId(paymentList.getWithdrawId())
                .amount(paymentList.getAmount())
                .balance(balance)
                .build();
    }

}
