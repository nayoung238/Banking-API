package SN.BANK.payment.service;

import SN.BANK.domain.enums.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ExchangeRateService {

    public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return new BigDecimal(1); // 같은 통화면 환율은 1
        }
        else return new BigDecimal(1); // TODO : 환율 가져오기
    }
}
