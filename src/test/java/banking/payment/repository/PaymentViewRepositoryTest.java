package banking.payment.repository;

import banking.account.entity.Account;
import banking.account.enums.AccountStatus;
import banking.account.enums.Currency;
import banking.account.repository.AccountRepository;
import banking.payment.dto.response.PaymentView;
import banking.payment.entity.Payment;
import banking.payment.enums.PaymentStatus;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import banking.user.entity.Role;
import banking.user.entity.User;
import banking.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@ActiveProfiles("test")
class PaymentViewRepositoryTest {

	@Autowired
	UserRepository userRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	TransferRepository transferRepository;

	@Autowired
	PaymentRepository paymentRepository;

	@Autowired
	PaymentViewRepository paymentViewRepository;

	@DisplayName("[PaymentView 생성 성공 테스트] 결제 조회 시 Payment View 반환")
	@Test
	void payment_view_success_test() {
		// given
		User payer = createUser("payer-user-1", "payer-login-id-1");
		User payee = createUser("payee-user-1", "payee-login-id-1");
		userRepository.saveAll(List.of(payer, payee));

		Account payerAccount = createAccount(payer, Currency.KRW, BigDecimal.valueOf(20000L));
		Account payeeAccount = createAccount(payee, Currency.KRW, BigDecimal.valueOf(10000L));
		accountRepository.saveAll(List.of(payerAccount, payeeAccount));

		BigDecimal amount = payerAccount.getBalance().divide(BigDecimal.valueOf(2));
		Transfer withdrawalTransfer = createTransfer(payerAccount, payeeAccount, amount);
		transferRepository.save(withdrawalTransfer);

		Payment payment = Payment.builder()
			.payerId(payer.getId())
			.payeeId(payee.getId())
			.transferId(withdrawalTransfer.getId())
			.paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
			.build();

		paymentRepository.save(payment);

		// when
		Optional<PaymentView> responseOptional = paymentViewRepository.findByPaymentId(payment.getId());

		// then
		org.junit.jupiter.api.Assertions.assertTrue(responseOptional.isPresent());
		PaymentView paymentView = responseOptional.get();
		assertAll(
			() -> assertThat(paymentView.paymentId()).isGreaterThan(0L),
			() -> assertThat(paymentView.paymentStatus()).isEqualTo(payment.getPaymentStatus()),
			() -> assertThat(paymentView.withdrawalAccountNumber()).isEqualTo(payerAccount.getAccountNumber()),
			() -> assertThat(paymentView.payeeName()).isEqualTo(payee.getName()),
			() -> assertThat(paymentView.amount()).isEqualByComparingTo(withdrawalTransfer.getAmount()),
			() -> assertThat(paymentView.exchangeRate()).isEqualByComparingTo(withdrawalTransfer.getExchangeRate()),
			() -> assertThat(paymentView.currency()).isEqualTo(withdrawalTransfer.getCurrency())
		);
	}

	@DisplayName("[PaymentView 생성 성공 테스트] payee 사용자가 탈퇴해도 Payment View 반환 가능")
	@Test
	void payment_view_success_test_when_payee_null () {
		// given
		User payer = createUser("payer-user-2", "payer-login-id-2");
		User payee = createUser("payee-user-2", "payee-login-id-2");
		userRepository.saveAll(List.of(payer, payee));

		Account payerAccount = createAccount(payer, Currency.KRW, BigDecimal.valueOf(20000L));
		Account payeeAccount = createAccount(payee, Currency.KRW, BigDecimal.valueOf(10000L));
		accountRepository.saveAll(List.of(payerAccount, payeeAccount));

		BigDecimal amount = payerAccount.getBalance().divide(BigDecimal.valueOf(2));
		Transfer withdrawalTransfer = createTransfer(payerAccount, payeeAccount, amount);
		transferRepository.save(withdrawalTransfer);

		Payment payment = Payment.builder()
			.payerId(payer.getId())
			.payeeId(payee.getId())
			.transferId(withdrawalTransfer.getId())
			.paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
			.build();

		paymentRepository.save(payment);

		accountRepository.delete(payeeAccount);
		userRepository.delete(payee);

		// when
		Optional<PaymentView> responseOptional = paymentViewRepository.findByPaymentId(payment.getId());

		// then
		org.junit.jupiter.api.Assertions.assertTrue(responseOptional.isPresent());
		PaymentView paymentView = responseOptional.get();
		assertAll(
			() -> assertThat(paymentView.paymentId()).isGreaterThan(0L),
			() -> assertThat(paymentView.paymentStatus()).isEqualTo(payment.getPaymentStatus()),
			() -> assertThat(paymentView.withdrawalAccountNumber()).isEqualTo(payerAccount.getAccountNumber()),
			() -> assertThat(paymentView.payeeName()).isEqualTo("탈퇴한 사용자"),
			() -> assertThat(paymentView.amount()).isEqualByComparingTo(withdrawalTransfer.getAmount()),
			() -> assertThat(paymentView.exchangeRate()).isEqualByComparingTo(withdrawalTransfer.getExchangeRate()),
			() -> assertThat(paymentView.currency()).isEqualTo(withdrawalTransfer.getCurrency())
		);
	}


	@DisplayName("[PaymentView 생성 실패 테스트] Transfer 제거되면 Payment View 생성 불가")
	@Test
	void payment_view_failed_test_when_transfer_null() {
		// given
		User payer = createUser("payer-user-3", "payer-login-id-3");
		User payee = createUser("payee-user-3", "payee-login-id-3");
		userRepository.saveAll(List.of(payer, payee));

		Account payerAccount = createAccount(payer, Currency.KRW, BigDecimal.valueOf(20000L));
		Account payeeAccount = createAccount(payee, Currency.KRW, BigDecimal.valueOf(10000L));
		accountRepository.saveAll(List.of(payerAccount, payeeAccount));

		BigDecimal amount = payerAccount.getBalance().divide(BigDecimal.valueOf(2));
		Transfer withdrawalTransfer = createTransfer(payerAccount, payeeAccount, amount);
		transferRepository.save(withdrawalTransfer);

		Payment payment = Payment.builder()
			.payerId(payer.getId())
			.payeeId(payee.getId())
			.transferId(withdrawalTransfer.getId())
			.paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
			.build();

		paymentRepository.save(payment);
		transferRepository.delete(withdrawalTransfer);

		// when
		Optional<PaymentView> responseOptional = paymentViewRepository.findByPaymentId(payment.getId());

		// then
		org.junit.jupiter.api.Assertions.assertTrue(responseOptional.isEmpty());
	}

	@DisplayName("[PaymentView 생성 실패 테스트] Payer Account 제거되면 Payment View 생성 불가")
	@Test
	void payment_view_failed_test_when_payer_account_null() {
		// given
		User payer = createUser("payer-user-4", "payer-login-id-4");
		User payee = createUser("payee-user-4", "payee-login-id-4");
		userRepository.saveAll(List.of(payer, payee));

		Account payerAccount = createAccount(payer, Currency.KRW, BigDecimal.valueOf(20000L));
		Account payeeAccount = createAccount(payee, Currency.KRW, BigDecimal.valueOf(10000L));
		accountRepository.saveAll(List.of(payerAccount, payeeAccount));

		BigDecimal amount = payerAccount.getBalance().divide(BigDecimal.valueOf(2));
		Transfer withdrawalTransfer = createTransfer(payerAccount, payeeAccount, amount);
		transferRepository.save(withdrawalTransfer);

		Payment payment = Payment.builder()
			.payerId(payer.getId())
			.payeeId(payee.getId())
			.transferId(withdrawalTransfer.getId())
			.paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
			.build();

		paymentRepository.save(payment);
		accountRepository.delete(payerAccount);

		// when
		Optional<PaymentView> responseOptional = paymentViewRepository.findByPaymentId(payment.getId());

		// then
		org.junit.jupiter.api.Assertions.assertTrue(responseOptional.isEmpty());
	}

	@DisplayName("[PaymentView 생성 실패 테스트] 존재하지 않는 payment ID로 검색하면 Payment View 생성 불가")
	@Test
	void payment_view_failed_test_when_invalid_payment_id() {
		// given
		User payer = createUser("payer-user-5", "payer-login-id-5");
		User payee = createUser("payee-user-5", "payee-login-id-5");
		userRepository.saveAll(List.of(payer, payee));

		Account payerAccount = createAccount(payer, Currency.KRW, BigDecimal.valueOf(20000L));
		Account payeeAccount = createAccount(payee, Currency.KRW, BigDecimal.valueOf(10000L));
		accountRepository.saveAll(List.of(payerAccount, payeeAccount));

		BigDecimal amount = payerAccount.getBalance().divide(BigDecimal.valueOf(2));
		Transfer withdrawalTransfer = createTransfer(payerAccount, payeeAccount, amount);
		transferRepository.save(withdrawalTransfer);

		Payment payment = Payment.builder()
			.payerId(payer.getId())
			.payeeId(payee.getId())
			.transferId(withdrawalTransfer.getId())
			.paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
			.build();

		paymentRepository.save(payment);

		// when
		Optional<PaymentView> responseOptional = paymentViewRepository.findByPaymentId(payment.getId() + 1);

		// then
		org.junit.jupiter.api.Assertions.assertTrue(responseOptional.isEmpty());
	}

	private User createUser(String name, String loginId) {
		return User.builder()
			.name(name)
			.loginId(loginId)
			.password("password")
			.role(Role.USER)
			.build();
	}

	private Account createAccount(User user, Currency currency, BigDecimal balance) {
		return Account.builder()
			.user(user)
			.accountNumber(generateAccountNumber())
			.password("12345")
			.balance(balance)
			.currency(currency)
			.accountStatus(AccountStatus.ACTIVE)
			.build();
	}

	private String generateAccountNumber() {
		String accountNumber;
		do {
			accountNumber = String.valueOf(ThreadLocalRandom.current().nextLong(10000000000000L, 99999999999999L));
		} while (accountRepository.existsByAccountNumber(accountNumber));

		return accountNumber.substring(0, 7) + "-" + accountNumber.substring(7);
	}

	private Transfer createTransfer(Account payerAccount, Account payeeAccount, BigDecimal amount) {
		return Transfer.builder()
			.transferGroupId(createTransferGroupId(payerAccount.getAccountNumber(), payeeAccount.getAccountNumber()))
			.transferOwnerId(payerAccount.getUser().getId())
			.transferType(TransferType.WITHDRAWAL)
			.withdrawalAccountId(payerAccount.getId())
			.depositAccountId(payeeAccount.getId())
			.currency(payeeAccount.getCurrency() + "/" + payerAccount.getCurrency())
			.exchangeRate(BigDecimal.ONE)
			.amount(amount)
			.balancePostTransaction(payeeAccount.getBalance().subtract(amount))
			.build();
	}

	private String createTransferGroupId(String withdrawalAccountNumber, String depositAccountNumber) {
		String transactionId = withdrawalAccountNumber.substring(0, 3) + depositAccountNumber.substring(0, 3);
		String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
		return transactionId + uuidPart;
	}
}