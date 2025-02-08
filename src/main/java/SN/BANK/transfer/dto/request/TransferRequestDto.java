package SN.BANK.transfer.dto.request;

import SN.BANK.payment.dto.request.PaymentRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "거래 요청 Dto")
public record TransferRequestDto(

    @NotNull
    @Schema(description = "송금 계좌번호", example = "5792214-80232581")
    String withdrawalAccountNumber,

    @NotNull
    @Schema(description = "송금 계좌 비밀번호", example = "12345")
    String withdrawalAccountPassword,

    @NotNull
    @Schema(description = "입금 계좌번호", example = "2197810-05875125")
    String depositAccountNumber,

    @NotNull
    @DecimalMin(value = "0.01", message = "금액은 0보다 커야합니다.")
    @Schema(description = "거래액", example = "50000")
    BigDecimal amount // Deposit Account 통화 기준
) {
    public static TransferRequestDto of(PaymentRequestDto paymentRequestDto) {
        return TransferRequestDto.builder()
            .withdrawalAccountNumber(paymentRequestDto.withdrawAccountNumber())
            .withdrawalAccountPassword(paymentRequestDto.withdrawalAccountPassword())
            .depositAccountNumber(paymentRequestDto.depositAccountNumber())
            .amount(paymentRequestDto.amount())
            .build();
    }
}
