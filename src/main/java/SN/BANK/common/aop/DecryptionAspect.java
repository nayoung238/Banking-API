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
				String decryptedWithdrawAccountNumber = decryptionFacade.decrypt(request.getWithdrawAccountNumber());
				String decryptedDepositAccountNumber = decryptionFacade.decrypt(request.getDepositAccountNumber());
				String decryptedPassword = decryptionFacade.decrypt(request.getPassword());

				log.info("[/payment] WithdrawAccountNumber {} -> {}", request.getWithdrawAccountNumber(), decryptedWithdrawAccountNumber);
				log.info("[/payment] DecryptedDepositAccountNumber {} -> {}", request.getDepositAccountNumber(), decryptedDepositAccountNumber);
				log.info("[/payment] password {} -> {}", request.getPassword(), decryptedPassword);

				args[i] = new PaymentRequestDto(
					decryptedWithdrawAccountNumber,
					decryptedDepositAccountNumber,
					request.getAmount(),
					decryptedPassword);
			} else if (args[i] instanceof PaymentRefundRequestDto request) {
				String decryptedPassword = decryptionFacade.decrypt(request.getPassword());
				args[i] = new PaymentRefundRequestDto(request.getPaymentId(), decryptedPassword);

				log.info("[/payment/cancel] password {} -> {}", request.getPassword(), decryptedPassword);
			}
		}
		return joinPoint.proceed(args);
	}
}
