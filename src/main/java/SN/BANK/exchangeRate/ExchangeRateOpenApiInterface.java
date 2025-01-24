package SN.BANK.exchangeRate;

import SN.BANK.account.enums.Currency;

import java.math.BigDecimal;

public sealed interface ExchangeRateOpenApiInterface
	permits ExchangeRateNaverService, ExchangeRateMananaService, ExchangeRateGoogleFinanceScraper {

	BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency);
}
