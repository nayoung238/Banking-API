package banking.exchangeRate;

import banking.account.enums.Currency;

import java.math.BigDecimal;

public sealed interface ExchangeRateOpenApiInterface
	permits ExchangeRateNaverService, ExchangeRateMananaService, ExchangeRateGoogleFinanceScraper {

	BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency);
}
