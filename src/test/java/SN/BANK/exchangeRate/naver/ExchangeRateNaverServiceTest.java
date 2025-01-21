package SN.BANK.exchangeRate.naver;

import SN.BANK.account.enums.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExchangeRateNaverServiceTest {

	@Autowired
	ExchangeRateNaverService exchangeRateNaverService;

	@Test
	void USD_KRW_test() {
		BigDecimal exchangeRate = exchangeRateNaverService.getExchangeRate(Currency.USD, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());

		System.out.println("[Naver] " +  Currency.USD + " " + exchangeRate);
	}

	@Test
	void EUR_KRW_test() {
		BigDecimal exchangeRate = exchangeRateNaverService.getExchangeRate(Currency.EUR, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());

		System.out.println("[Naver] " +  Currency.EUR + " " + exchangeRate);
	}
}