package SN.BANK.exchangeRate.naver;

import SN.BANK.account.enums.Currency;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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

		System.out.println("[Naver] USD/KRW " + exchangeRate);
	}

	@Test
	void EUR_KRW_test() {
		BigDecimal exchangeRate = exchangeRateNaverService.getExchangeRate(Currency.EUR, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());

		System.out.println("[Naver] EUR/KRW " + exchangeRate);
	}

	@Test
	void quote_currency_status_check_test() {
		Assertions.assertThatThrownBy(() -> exchangeRateNaverService.getExchangeRate(Currency.KRW, Currency.USD))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException) ex;
				assertEquals(ErrorCode.INVALID_QUOTE_CURRENCY_ERROR, customException.getErrorCode());
				assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
				assertEquals("Quote Currency는 한국 원화(KRW) 단위여야 합니다.", customException.getErrorCode().getMessage());
			});
	}
}