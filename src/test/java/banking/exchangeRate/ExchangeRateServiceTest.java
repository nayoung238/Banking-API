package banking.exchangeRate;

import banking.account.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class ExchangeRateServiceTest {

	@Autowired
	ExchangeRateService exchangeRateService;

	@DisplayName("송금인 계좌 단위가 USD인 경우 환율 데이터가 1 미만이 반환되어야 한다.")
	@Test
	void sender_USD_currency_test() {
		BigDecimal exchangeRate = exchangeRateService.getExchangeRate(Currency.USD, Currency.KRW);

		assertNotNull(exchangeRate);
		assertThat(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertThat(exchangeRate.compareTo(BigDecimal.ONE) < 1);

		System.out.println("KRW/USD 환율 결과: " + exchangeRate);
	}

	@DisplayName("송금인 계좌 단위가 EUR인 경우 환율 데이터가 1 미만이 반환되어야 한다.")
	@Test
	void sender_EUR_currency_test() {
		BigDecimal exchangeRate = exchangeRateService.getExchangeRate(Currency.EUR, Currency.KRW);

		assertNotNull(exchangeRate);
		assertThat(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertThat(exchangeRate.compareTo(BigDecimal.ONE) < 1);

		System.out.println("KRW/USD 환율 결과: " + exchangeRate);
	}

	@DisplayName("송금인 계좌 단위가 KRW 인 경우 환율 데이터가 1 이상이 반환되어야 한다.")
	@Test
	void KRW_USE_test() {
		BigDecimal exchangeRate = exchangeRateService.getExchangeRate(Currency.KRW, Currency.USD);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());

		System.out.println("USD/KRW 환율 결과: " + exchangeRate);
	}
}