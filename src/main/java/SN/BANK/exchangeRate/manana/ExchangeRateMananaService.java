package SN.BANK.exchangeRate.manana;

import SN.BANK.account.enums.Currency;
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
public class ExchangeRateMananaService {

	private final ExchangeRateMananaClient exchangeRateMananaClient;

	public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
		List<ExchangeRateMananaResponseDto> result = exchangeRateMananaClient.getExchangeRate(toCurrency, fromCurrency);
		return result.get(0).rate().setScale(2, RoundingMode.CEILING);
	}
}
