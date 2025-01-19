package SN.BANK.exchangeRate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class ExchangeRateResponseDto {

    @JsonProperty("country")
    private List<ExchangeRateCountry> country;

    @Getter
    @Setter
    public static class ExchangeRateCountry {
        @JsonProperty("value")
        private String value;

        @JsonProperty("currencyUnit")
        private String currencyUnit;

    }
}

