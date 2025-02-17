package banking.exchangeRate;

import banking.account.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ExchangeRateServiceMockTest {

	@InjectMocks
	ExchangeRateService exchangeRateService;

	@Mock
	ExchangeRateNaverService exchangeRateNaverService;

	@Mock
	ExchangeRateMananaService exchangeRateMananaService;

	@Mock
	ExchangeRateGoogleFinanceScraper exchangeRateGoogleFinanceScraper;

	@DisplayName("[환율 조회 성공 테스트] 같은 통화 업데이트 작업은 1개의 스레드만 가능, 그렇지 않으면 환율 업데이트 로직 동시 수행 가능")
//	@Test
	void exclusive_update_for_same_currency_test() throws InterruptedException {
		when(exchangeRateNaverService.getExchangeRate(any(), any()))
			.thenReturn(BigDecimal.ONE);

		when(exchangeRateMananaService.getExchangeRate(any(Currency.class), any(Currency.class)))
			.thenReturn(BigDecimal.ONE);

		when(exchangeRateGoogleFinanceScraper.getExchangeRate(any(Currency.class), any(Currency.class)))
			.thenReturn(BigDecimal.ONE);

		// 멀티스레드 환경 설정
		int threadCount = 20;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount * 2);
		CountDownLatch latch = new CountDownLatch(threadCount * 2);

		List<Future<?>> futures = new ArrayList<>();

		// KRW/USD 작업 생성
		for(int i = 0; i < threadCount; i++) {
			futures.add(executorService.submit(() -> {
				try {
					exchangeRateService.getExchangeRate(Currency.KRW, Currency.USD);
				} finally {
					latch.countDown();
				}
			}));
		}

		// EUR/KRW 작업 생성
		for(int i = 0; i < threadCount; i++) {
			futures.add(executorService.submit(() -> {
				try {
					exchangeRateService.getExchangeRate(Currency.EUR, Currency.KRW);
				} finally {
					latch.countDown();
				}
			}));
		}

		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (ExecutionException e) {
				fail("Task execution failed: " + e.getMessage());
			}
		}
		latch.await(20, TimeUnit.SECONDS);
		executorService.shutdown();

		// KRW/USD 환율 업데이트 1회 작업 확인
		verify(exchangeRateNaverService, times(1))
			.getExchangeRate(Currency.KRW, Currency.USD);

		// EUR/KRW 환율 업데이트 1회 작업 확인
		verify(exchangeRateNaverService, times(1))
			.getExchangeRate(Currency.EUR, Currency.KRW);
	}
}