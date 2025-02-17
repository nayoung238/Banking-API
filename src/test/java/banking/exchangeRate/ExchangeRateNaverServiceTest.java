package banking.exchangeRate;

import banking.account.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExchangeRateNaverServiceTest {

	@Autowired
	ExchangeRateNaverService exchangeRateNaverService;

	@DisplayName("[Naver 환율 성공 테스트] Base Currency가 KRW이면 환율 데이터 1 미만 반환")
	@Test
	void base_currency_krw_test() {
		BigDecimal exchangeRate = exchangeRateNaverService.getExchangeRate(Currency.KRW, Currency.USD);

		assertNotNull(exchangeRate);
		assertThat(exchangeRate.compareTo(BigDecimal.ZERO) > 0).isTrue();
		assertThat(exchangeRate.compareTo(BigDecimal.ONE) < 0).isTrue();
		assertEquals(5, exchangeRate.scale());

		System.out.println("[Naver] KRW/xxx 환율 결과: " + exchangeRate);
	}

	@DisplayName("[Naver 환율 성공 테스트] Quote Currency가 KRW이면 환율 데이터 1 이상 반환")
	@Test
	void quote_currency_krw_test() {
		BigDecimal exchangeRate = exchangeRateNaverService.getExchangeRate(Currency.EUR, Currency.KRW);

		assertNotNull(exchangeRate);
		assertThat(exchangeRate.compareTo(BigDecimal.ZERO) > 0).isTrue();
		assertThat(exchangeRate.compareTo(BigDecimal.ONE) < 0).isFalse();
		assertEquals(2, exchangeRate.scale());

		System.out.println("[Naver] xxx/KRW 환율 결과: " + exchangeRate);
	}
}