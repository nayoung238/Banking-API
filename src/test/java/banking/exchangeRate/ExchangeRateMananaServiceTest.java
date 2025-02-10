package banking.exchangeRate;

import banking.account.enums.Currency;
import org.junit.jupiter.api.DisplayName;
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

	@DisplayName("Manana 서비스로 USD/KRW 요청 시 1 이상의 값을 반환해야 한다.")
	@Test
	void USD_KRW_test() {
		BigDecimal exchangeRate = exchangeRateMananaService.getExchangeRate(Currency.USD, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ONE) > 0);
		assertEquals(2, exchangeRate.scale());

		System.out.println("[Manana] USD/KRW " + exchangeRate);
	}

	@DisplayName("Manana 서비스로 EUR/KRW 요청 시 1 이상의 값을 반환해야 한다.")
	@Test
	void EUR_KRW_test() {
		BigDecimal exchangeRate = exchangeRateMananaService.getExchangeRate(Currency.EUR, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ONE) > 0);
		assertEquals(2, exchangeRate.scale());

		System.out.println("[Manana] EUR/KRW " + exchangeRate);
	}
}