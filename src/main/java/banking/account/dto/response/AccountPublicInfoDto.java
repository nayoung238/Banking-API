package banking.account.dto.response;

import banking.account.entity.Account;
import banking.account.enums.Currency;
import lombok.Builder;

@Builder
public record AccountPublicInfoDto(

	String ownerName,
	String accountNumber,
	String accountName,
	Currency currency
) {

	public static AccountPublicInfoDto of(Account account) {
		return AccountPublicInfoDto.builder()
			.ownerName(account.getUser().getName())
			.accountNumber(account.getAccountNumber())
			.accountName(account.getAccountName())
			.currency(account.getCurrency())
			.build();
	}
}
