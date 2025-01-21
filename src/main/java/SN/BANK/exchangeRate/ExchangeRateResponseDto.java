package SN.BANK.exchangeRate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;


@Getter
public class ExchangeRateResponseDto {

    @JsonProperty("country")
    private List<ExchangeRateCountry> country;

    @Getter
    public static class ExchangeRateCountry {
        @JsonProperty("value")
        private String value;

        @JsonProperty("currencyUnit")
        private String currencyUnit;

    }
}

