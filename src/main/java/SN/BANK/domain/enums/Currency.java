package SN.BANK.domain.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Currency {

    대한민국("KRW"),
    미국("USD"),
    유럽("EUR");

    private final String currency;

}
