package banking.transfer.service;

import banking.account.entity.Account;
import banking.account.enums.AccountStatus;
import banking.account.enums.Currency;
import banking.account.repository.AccountRepository;
import banking.kafka.config.TopicConfig;
import banking.kafka.dto.TransferFailedEvent;
import banking.kafka.service.KafkaProducerService;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import banking.user.entity.User;
import banking.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
	brokerProperties = "listeners=PLAINTEXT://localhost:9092",
	ports = 9092,
	partitions = 1,
	topics = TopicConfig.TRANSFER_FAILED_TOPIC)
class TransferCompensationServiceTest {

	@Autowired
	KafkaProducerService kafkaProducerService;

	@Autowired
	TransferRepository transferRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	UserRepository userRepository;

//	@Test
	@DisplayName("[보상 트랜잭션 성공 테스트] 카프카 이벤트 소비 후 보상 트랜잭션 시작")
	void compensation_transaction_start_test () {
		// given
		User user = User.builder()
			.name("name")
			.loginId("loginId")
			.password("password")
			.build();

		userRepository.save(user);

		final BigDecimal initialAmount = BigDecimal.ZERO;
		Account baseAccount = Account.builder()
			.user(user)
			.accountNumber("32427-463521")
			.password("password")
			.currency(Currency.KRW)
			.balance(initialAmount)
			.accountStatus(AccountStatus.ACTIVE)
			.build();

		accountRepository.save(baseAccount);

		Transfer baseTransfer = Transfer.builder()
			.transferGroupId("dsl30jtl42")
			.transferOwnerId(user.getId())
			.transferType(TransferType.WITHDRAWAL)
			.withdrawalAccountId(1L)
			.depositAccountId(2L)
			.currency("KRW/KRW")
			.exchangeRate(BigDecimal.ONE)
			.amount(BigDecimal.TEN)
			.balancePostTransaction(BigDecimal.TEN)
			.build();

		transferRepository.save(baseTransfer);

		TransferFailedEvent failedEvent = TransferFailedEvent.builder()
			.transferType(TransferType.DEPOSIT)
			.transferGroupId(baseTransfer.getTransferGroupId())
			.build();

		// when
		kafkaProducerService.send(TopicConfig.TRANSFER_FAILED_TOPIC, failedEvent);

		// then
		await()
			.atMost(15, TimeUnit.SECONDS)
			.pollInterval(500, TimeUnit.MILLISECONDS)
			.untilAsserted(() -> {
				Optional<Transfer> refundedTransferOptional = transferRepository.findByTransferGroupIdAndTransferType(baseTransfer.getTransferGroupId(), TransferType.REFUNDED);
				assertTrue(refundedTransferOptional.isPresent());
				Transfer refundedTransfer = refundedTransferOptional.get();
				assertEquals(refundedTransfer.getTransferGroupId(), baseTransfer.getTransferGroupId());
				assertEquals(refundedTransfer.getTransferOwnerId(), baseTransfer.getTransferOwnerId());
				assertEquals(refundedTransfer.getTransferType(), TransferType.REFUNDED);
				assertEquals(0, refundedTransfer.getAmount().compareTo(baseTransfer.getAmount()));
				assertEquals(0, refundedTransfer.getBalancePostTransaction().compareTo(initialAmount.add(baseTransfer.getAmount())));
			});
	}
}