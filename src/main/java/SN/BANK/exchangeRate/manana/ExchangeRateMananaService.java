package SN.BANK.exchangeRate.manana;

import SN.BANK.account.enums.Currency;
import SN.BANK.exchangeRate.ExchangeRateOpenApiInterface;
import SN.BANK.exchangeRate.manana.dto.ExchangeRateMananaResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateMananaService implements ExchangeRateOpenApiInterface {

	private final ExchangeRateMananaClient exchangeRateMananaClient;

	@Override
	public BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		List<ExchangeRateMananaResponseDto> result = exchangeRateMananaClient.getExchangeRate(quoteCurrency, baseCurrency);
		return result.get(0).rate().setScale(2, RoundingMode.CEILING);
	}
}
