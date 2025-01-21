package SN.BANK.exchangeRate.manana;

import SN.BANK.account.enums.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExchangeRateMananaServiceTest {

	@Autowired
	ExchangeRateMananaService exchangeRateMananaService;

	@Test
	void USD_KRW_test() {
		BigDecimal exchangeRate = exchangeRateMananaService.getExchangeRate(Currency.USD, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());

		System.out.println("[Manana] " +  Currency.USD + " " + exchangeRate);
	}

	@Test
	void EUR_KRW_test() {
		BigDecimal exchangeRate = exchangeRateMananaService.getExchangeRate(Currency.EUR, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());

		System.out.println("[Manana] " +  Currency.EUR + " " + exchangeRate);
	}
}