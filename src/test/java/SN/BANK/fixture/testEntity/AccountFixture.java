package SN.BANK.fixture.testEntity;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.users.entity.Users;

import java.math.BigDecimal;

public enum AccountFixture {

	ACCOUNT_FIXTURE_KRW_1 (
		1L,
		"10954324-3585369",
		"45678",
		new BigDecimal(10000000),
		Currency.KRW
	),
	ACCOUNT_FIXTURE_USD (
		2L,
		"3250324-23423532",
		"96456",
		new BigDecimal(1000),
		Currency.USD
	),
	ACCOUNT_FIXTURE_EUR (
		3L,
		"1347924-38796623",
		"12568",
		new BigDecimal(10000),
		Currency.EUR
	),
	ACCOUNT_FIXTURE_KRW_2 (
		4L,
		"326434-323543769",
		"49179",
		new BigDecimal(100000),
		Currency.KRW
	),
	ACCOUNT_FIXTURE_JPY (
		5L,
		"234924-3813323",
		"82345",
		new BigDecimal(10000),
		Currency.JPY
	);

	private final Long id;
	private final String accountNumber;
	private final String password;
	private final BigDecimal balance;
	private final Currency currency;

	AccountFixture(Long id, String accountNumber, String password, BigDecimal balance, Currency currency) {
		this.id = id;
		this.accountNumber = accountNumber;
		this.password = password;
		this.balance = balance;
		this.currency = currency;
	}

	public Account createAccount(Users user) {
		return Account.builder()
			.id(id)
			.user(user)
			.accountNumber(accountNumber)
			.password(password)
			.balance(balance)
			.currency(currency)
			.build();
	}
}
