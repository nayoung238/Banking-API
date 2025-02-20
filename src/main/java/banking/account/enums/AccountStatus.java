package banking.account.enums;

import lombok.Getter;

@Getter
public enum AccountStatus {

	ACTIVE("활성 상태"),
	INACTIVE("비활성 상태"),
	SUSPENDED("정지 상태"),
	CLOSED("해지 상태"),
	RESTRICTED("제한 상태"),
	PENDING("대기 상태");

	private final String description;

	AccountStatus(String description) {
		this.description = description;
	}

}