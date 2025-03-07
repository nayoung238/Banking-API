package banking.fixture.dto;

import banking.account.dto.request.AccountCreationRequest;
import banking.account.enums.Currency;

public enum AccountCreationRequestDtoFixture {

	ACCOUNT_FIXTURE_KRW_1 (
		"45678",
		Currency.KRW,
		"test-krw-account-1"
	),
	ACCOUNT_FIXTURE_USD (
		"96456",
		Currency.USD,
		"test-usd-account"
	),
	ACCOUNT_FIXTURE_EUR (
		"12568",
		Currency.EUR,
		"test-eur-account"
	),
	ACCOUNT_FIXTURE_KRW_2 (
		"49179",
		Currency.KRW,
		"test-krw-account-2"
	),
	ACCOUNT_FIXTURE_JPY (
		"82345",
		Currency.JPY,
		"test-jpy-account"
	);

	private final String password;
	private final Currency currency;
	private final String accountName;

	AccountCreationRequestDtoFixture(String password, Currency currency, String accountName) {
		this.password = password;
		this.currency = currency;
		this.accountName = accountName;
	}

	public AccountCreationRequest createAccountCreationRequestDto() {
		return AccountCreationRequest.builder()
			.password(password)
			.currency(currency)
			.accountName(accountName)
			.build();
	}
}
