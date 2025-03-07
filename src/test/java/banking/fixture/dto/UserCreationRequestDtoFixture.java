package banking.fixture.dto;

import banking.user.dto.request.UserCreationRequest;

public enum UserCreationRequestDtoFixture {

	USER_CREATION_REQUEST_DTO_FIXTURE_1 (
		"김민수",
		"minsu01",
		"password123"
	),
	USER_CREATION_REQUEST_DTO_FIXTURE_2 (
		"박지연",
		"jiyeon02",
		"password456"
	);

	private String name;
	private String loginId;
	private String password;

	UserCreationRequestDtoFixture(String name, String loginId, String password) {
		this.name = name;
		this.loginId = loginId;
		this.password = password;
	}

	public UserCreationRequest createUserCreationRequestDto() {
		return UserCreationRequest.builder()
			.name(name)
			.loginId(loginId)
			.password(password)
			.build();
	}
}
