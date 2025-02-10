package banking.fixture.testEntity;

import banking.users.entity.Users;

public enum UserFixture {

	USER_FIXTURE_1 (
		1L,
		"김민수",
		"minsu01",
		"password123"
	),
	USER_FIXTURE_2 (
		2L,
		"박지연",
		"jiyeon02",
		"password456"
	),
	USER_FIXTURE_3 (
		3L,
		"이영자",
		"youngji03",
		"password789"
	),
	USER_FIXTURE_4 (
		4L,
		"최준혁",
		"junhyuk04",
		"password1234"
	),
	USER_FIXTURE_5 (
		5L,
		"정은지",
		"eunji05",
		"asdfgh5678"
	);

	private final Long id;
	private final String name;
	private final String loginId;
	private final String password;

	UserFixture(Long id, String name, String loginId, String password) {
		this.id = id;
		this.name = name;
		this.loginId = loginId;
		this.password = password;
	}

	public Users createUser() {
		return Users.builder()
			.id(id)
			.name(name)
			.loginId(loginId)
			.password(password)
			.build();
	}
}
