package banking.exchangeRate;

import banking.account.enums.Currency;
import banking.exchangeRate.dto.ExchangeRateNaverResponseDto;
import banking.exchangeRate.openfeign.ExchangeRateNaverClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public final class ExchangeRateNaverService implements ExchangeRateOpenApiInterface {

	private final ExchangeRateNaverClient exchangeRateNaverClient;

	@Value("${exchange-rate.naver.request-param.key}")
	private String key;

	@Value("${exchange-rate.naver.request-param.pkid}")
	private String pkid;

	@Value("${exchange-rate.naver.request-param.q}")
	private String query;

	@Value("${exchange-rate.naver.request-param.where}")
	private String where;

	@Value("${exchange-rate.naver.request-param.u1}")
	private String u1;

	@Value("${exchange-rate.naver.request-param.u6}")
	private String u6;

	@Value("${exchange-rate.naver.request-param.u7}")
	private String u7;

	@Value("${exchange-rate.naver.request-param.u8}")
	private String u8;

	@Value("${exchange-rate.naver.request-param.u2}")
	private String u2;

	@Override
	public BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		boolean isInverse = false;
		if(baseCurrency.equals(Currency.KRW)) {
			Currency temp = baseCurrency;
			baseCurrency = quoteCurrency;
			quoteCurrency = temp;
			isInverse = true;
		}

		ExchangeRateNaverResponseDto result = exchangeRateNaverClient.getExchangeRate(
			key,
			pkid,
			query,
			where,
			u1,
			u6,
			u7,
			baseCurrency,
			quoteCurrency,
			u8,
			u2
		);

		BigDecimal exchangeRate = convertToBigDecimal(result.country().get(1).value());
		if(isInverse) {
			exchangeRate = BigDecimal.ONE.divide(exchangeRate, 5, RoundingMode.DOWN);
		}
		return exchangeRate;
	}

	private BigDecimal convertToBigDecimal(String value) {
		value = value.replace(",", "");
		return new BigDecimal(value).setScale(2, RoundingMode.CEILING);
	}
}
