package banking.common.aop;

import banking.common.data.DecryptionFacade;
import banking.payment.dto.request.PaymentRefundRequest;
import banking.payment.dto.request.PaymentRequest;
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

	@Around("@annotation(banking.common.aop.Decrypt)")
	public Object decrypt(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof PaymentRequest request) {
				String decryptedDepositAccountNumber = decryptionFacade.decrypt(request.depositAccountNumber());
				String decryptedPassword = decryptionFacade.decrypt(request.withdrawalAccountPassword());

				log.info("[/payment] DecryptedDepositAccountNumber {} -> {}", request.depositAccountNumber(), decryptedDepositAccountNumber);
				log.info("[/payment] password {} -> {}", request.withdrawalAccountPassword(), decryptedPassword);

				args[i] = PaymentRequest.builder()
					.withdrawalAccountId(request.withdrawalAccountId())
					.withdrawalAccountPassword(decryptedPassword)
					.depositAccountNumber(decryptedDepositAccountNumber)
					.amount(request.amount())
					.build();

			} else if (args[i] instanceof PaymentRefundRequest request) {
				String decryptedPassword = decryptionFacade.decrypt(request.withdrawalAccountPassword());
				args[i] = PaymentRefundRequest.builder()
					.paymentId(request.paymentId())
					.withdrawalAccountPassword(decryptedPassword)
					.build();

				log.info("[/payment/cancel] password {} -> {}", request.withdrawalAccountPassword(), decryptedPassword);
			}
		}
		return joinPoint.proceed(args);
	}
}
