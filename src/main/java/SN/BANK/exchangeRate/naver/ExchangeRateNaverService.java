package SN.BANK.exchangeRate.naver;

import SN.BANK.account.enums.Currency;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.exchangeRate.ExchangeRateOpenApiInterface;
import SN.BANK.exchangeRate.naver.dto.ExchangeRateNaverResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ExchangeRateNaverService implements ExchangeRateOpenApiInterface {

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
		if(!quoteCurrency.equals(Currency.KRW)) {
			throw new CustomException(ErrorCode.INVALID_QUOTE_CURRENCY_ERROR);
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

		return convertToBigDecimal(result.country().get(1).value());
	}

	private BigDecimal convertToBigDecimal(String value) {
		value = value.replace(",", "");
		return new BigDecimal(value);
	}
}
