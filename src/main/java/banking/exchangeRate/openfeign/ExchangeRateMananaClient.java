package banking.exchangeRate.openfeign;

import banking.account.enums.Currency;
import banking.common.config.FeignClientConfig;
import banking.exchangeRate.dto.ExchangeRateMananaResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
	name = "manana-exchange-rate-client",
	url = "https://api.manana.kr/exchange",
	configuration = FeignClientConfig.class
)
public interface ExchangeRateMananaClient {

	@GetMapping("/rate.json")
	List<ExchangeRateMananaResponseDto> getExchangeRate(
		@RequestParam("base") Currency base,
		@RequestParam("code") Currency code
	);
}
