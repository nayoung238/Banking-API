package banking.transfer.service;

import banking.account.service.AccountBalanceService;
import banking.common.exception.CustomException;
import banking.kafka.dto.TransferFailedEvent;
import banking.kafka.service.KafkaProducerService;
import banking.transfer.entity.Transfer;
import banking.transfer.repository.TransferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class DepositAsyncServiceTest {

	@InjectMocks
	DepositAsyncService depositAsyncService;

	@Mock
	private AccountBalanceService accountBalanceService;

	@Mock
	private KafkaProducerService kafkaProducerService;

	@Mock
	private TransferRepository transferRepository;

	@Test
	@DisplayName("[async 입금 실패 테스트] 입금 실패 시 보상 트랜잭션 시작")
	void compensation_transaction_start_test() {
		// given
		Transfer mockTransfer = Transfer.builder()
			.depositAccountId(1L)
			.amount(BigDecimal.TEN)
			.exchangeRate(BigDecimal.ONE)
			.build();

		when(accountBalanceService.increaseBalanceWithLock(anyLong(), any(BigDecimal.class))).thenThrow(CustomException.class);
		doNothing().when(kafkaProducerService).send(anyString(), any(TransferFailedEvent.class));
		when(transferRepository.save(any(Transfer.class))).thenReturn(mockTransfer);

		// when
		depositAsyncService.processDepositAsync(mockTransfer);

		// then
		await()
			.atMost(5, TimeUnit.SECONDS)
			.pollInterval(500, TimeUnit.MILLISECONDS)
			.untilAsserted(() -> {
				verify(accountBalanceService, times(1))
					.increaseBalanceWithLock(eq(mockTransfer.getDepositAccountId()), any(BigDecimal.class));
				verify(transferRepository, never()).save(any(Transfer.class));
				verify(kafkaProducerService, times(1)).send(anyString(), any(TransferFailedEvent.class));
			});
	}

	@Test
	@DisplayName("[async 입금 성공 테스트] 입금 성공 시 보상 트랜잭션 생성되지 않음")
	void compensation_transaction_not_started_test() {
		// given
		Transfer mockTransfer = Transfer.builder()
			.depositAccountId(1L)
			.amount(BigDecimal.TEN)
			.exchangeRate(BigDecimal.ONE)
			.build();

		when(accountBalanceService.increaseBalanceWithLock(anyLong(), any(BigDecimal.class))).thenReturn(BigDecimal.TEN);

		// when
		depositAsyncService.processDepositAsync(mockTransfer);

		// then
		await()
			.atMost(5, TimeUnit.SECONDS)
			.pollInterval(500, TimeUnit.MILLISECONDS)
			.untilAsserted(() -> {
				verify(accountBalanceService, times(1))
					.increaseBalanceWithLock(eq(mockTransfer.getDepositAccountId()), any(BigDecimal.class));
				verify(transferRepository, times(1)).save(any(Transfer.class));
				verify(kafkaProducerService, never()).send(anyString(), any());
			});
	}
}