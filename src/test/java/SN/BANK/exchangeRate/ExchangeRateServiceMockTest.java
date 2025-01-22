package SN.BANK.exchangeRate;

import SN.BANK.account.enums.Currency;
import SN.BANK.exchangeRate.naver.ExchangeRateNaverService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceMockTest {

	@InjectMocks
	ExchangeRateService exchangeRateService;

	@Mock
	ExchangeRateNaverService exchangeRateNaverService;

	@DisplayName("같은 currency 업데이트 작업은 1개의 스레드만 가능하며, currency가 다른 경우 동시에 환율 업데이트 로직을 수행해도 된다.")
	@Test
	void currency_only_reentrant_lock_test() throws InterruptedException {
		Mockito.when(exchangeRateNaverService.getExchangeRate(any(), any()))
			.thenReturn(new BigDecimal(1));

		// 멀티스레드 환경 설정
		int threadCount = 20;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		List<Future<Void>> futures = new ArrayList<>();

		// USD/KRW 작업 생성
		for(int i = 0; i < threadCount; i++) {
			futures.add(executorService.submit(() -> {
				try {
				exchangeRateService.getExchangeRate(Currency.KRW, Currency.USD);
				} finally {
					latch.countDown();
				}
				return null;
			}));
		}

		// EUR/KRW 작업 생성
		for(int i = 0; i < threadCount; i++) {
			futures.add(executorService.submit(() -> {
				try {
					exchangeRateService.getExchangeRate(Currency.KRW, Currency.EUR);
				} finally {
					latch.countDown();
				}
				return null;
			}));
		}

		// 모든 작업 실행
		for (Future<Void> future : futures) {
			try {
				future.get(); // 각 future 실행 및 예외 대기
			} catch (ExecutionException e) {
				fail("Task execution failed: " + e.getMessage());
			}
		}
		latch.await(5, TimeUnit.SECONDS);
		executorService.shutdown();

		// USD/KRW 환율 업데이트 1회 작업 확인
		verify(exchangeRateNaverService, times(1))
			.getExchangeRate(Currency.USD, Currency.KRW);

		// EUR/KRW 환율 업데이트 1회 작업 확인
		verify(exchangeRateNaverService, times(1))
			.getExchangeRate(Currency.EUR, Currency.KRW);
	}
}