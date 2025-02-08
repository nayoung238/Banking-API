package SN.BANK.common.aop;

import SN.BANK.common.data.DecryptionFacade;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DecryptionAspect {

	private final DecryptionFacade decryptionFacade;

	@Around("@annotation(Decrypt)")
	public Object decrypt(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof PaymentRequestDto request) {
				String decryptedWithdrawAccountNumber = decryptionFacade.decrypt(request.withdrawAccountNumber());
				String decryptedDepositAccountNumber = decryptionFacade.decrypt(request.depositAccountNumber());
				String decryptedPassword = decryptionFacade.decrypt(request.withdrawalAccountPassword());

				log.info("[/payment] WithdrawAccountNumber {} -> {}", request.withdrawAccountNumber(), decryptedWithdrawAccountNumber);
				log.info("[/payment] DecryptedDepositAccountNumber {} -> {}", request.depositAccountNumber(), decryptedDepositAccountNumber);
				log.info("[/payment] password {} -> {}", request.withdrawalAccountPassword(), decryptedPassword);

				args[i] = PaymentRequestDto.builder()
					.withdrawAccountNumber(decryptedWithdrawAccountNumber)
					.withdrawalAccountPassword(decryptedPassword)
					.depositAccountNumber(decryptedDepositAccountNumber)
					.amount(request.amount())
					.build();

			} else if (args[i] instanceof PaymentRefundRequestDto request) {
				String decryptedPassword = decryptionFacade.decrypt(request.password());
				args[i] = PaymentRefundRequestDto.builder()
					.paymentId(request.paymentId())
					.password(decryptedPassword)
					.build();

				log.info("[/payment/cancel] password {} -> {}", request.password(), decryptedPassword);
			}
		}
		return joinPoint.proceed(args);
	}
}
