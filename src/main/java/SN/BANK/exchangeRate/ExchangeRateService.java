package SN.BANK.exchangeRate;

import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.domain.enums.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate;

    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency)) { //둘다 원화일 경우
            return BigDecimal.ONE;
        }

        Currency baseCurrency;
        boolean isInverse = false; // 역수 계산 여부 플래그

        if (fromCurrency.equals(Currency.대한민국)) {
            baseCurrency = toCurrency; // 원화 -> 외화
        } else if (toCurrency.equals(Currency.대한민국)) {
            baseCurrency = fromCurrency; // 외화 -> 원화
            isInverse = true; // 역수 계산 필요
        } else {
            throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL); // 외화 -> 외화
        }

        // Naver API URL
        String apiUrl = "https://m.search.naver.com/p/csearch/content/qapirender.nhn";

        // API 요청 파라미터 구성
        String url = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", "calculator")
                .queryParam("pkid", 141)
                .queryParam("q", "환율")
                .queryParam("where", "m")
                .queryParam("u1", "keb")
                .queryParam("u2", 1)
                .queryParam("u3", baseCurrency.getCurrency()) // 기준 통화
                .queryParam("u4", "KRW")  //변환 통화
                .queryParam("u6", "standardUnit")
                .queryParam("u7", "0")
                .queryParam("u8", "down")
                .toUriString();

        // API 호출 및 응답 처리
        try {
            ExchangeRateResponseDto response = restTemplate.getForObject(url, ExchangeRateResponseDto.class);

            if (response != null && response.getCountry().size() == 2) {
                BigDecimal exchangeRate = new BigDecimal(response.getCountry().get(1).getValue().replace(",", ""));
                return isInverse ? BigDecimal.ONE.divide(exchangeRate, 10, BigDecimal.ROUND_HALF_UP) : exchangeRate;
            } else {
                throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL);
        }
    }
}
