package banking.exchangeRate;

import banking.account.enums.Currency;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public final class ExchangeRateGoogleFinanceScraper implements ExchangeRateOpenApiInterface {

	private static final int TIMEOUT = 5000;

	@Override
	public BigDecimal getExchangeRate(Currency baseCurrency, Currency quoteCurrency) {
		String url = "https://www.google.com/finance/quote/" + baseCurrency + "-" + quoteCurrency;

		try {
			Document doc = Jsoup.connect(url)
				.userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
				.timeout(TIMEOUT)
				.get();

			Element titleElement = doc.selectFirst("div.YMlKec.fxKbKc");
			if (titleElement != null) {
				log.info("Successfully received exchange rate information: {} to {} is {}", baseCurrency, quoteCurrency, titleElement.text());
				if(baseCurrency.equals(Currency.KRW)) {
					return convertToBigDecimal(titleElement.text(), 5);
				}
				return convertToBigDecimal(titleElement.text(), 2);
			} else {
				log.warn("No exchange rate information found for: {} to {}", baseCurrency, quoteCurrency);
				throw new IllegalArgumentException("No exchange rate information found for: " + baseCurrency + " to " + quoteCurrency);
			}
		} catch (IOException e) {
			log.error("IO exception occurred: ", e);
			throw new RuntimeException("\"Failed to fetch exchange rate due to IO issue. ", e);
		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	private BigDecimal convertToBigDecimal(String text, int scale) {
		String exchangeRate = text.replace(",", "");
		return new BigDecimal(exchangeRate).setScale(scale, RoundingMode.CEILING);
	}
}
