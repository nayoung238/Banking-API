package SN.BANK.exchangeRate;

import SN.BANK.account.enums.Currency;

import java.math.BigDecimal;

public interface ExchangeRateOpenApiInterface {

	BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency);
}
