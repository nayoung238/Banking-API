package banking.auth.api;

import banking.account.repository.AccountRepository;
import banking.auth.dto.request.LoginRequest;
import banking.user.dto.request.UserCreationRequest;
import banking.user.repository.UserRepository;
import banking.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@AfterEach
	void afterEach() {
		accountRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	@DisplayName("[로그인 성공 테스트] 로그인 성공 시 Access & Refresh Token 생성")
	public void login_succeed_test () throws Exception {
		// given
		UserCreationRequest userCreationRequest = UserCreationRequest.builder()
			.name("test-name")
			.loginId("test-login-id-890")
			.password("test-password")
			.build();

		userService.register(userCreationRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId(userCreationRequest.loginId())
			.password(userCreationRequest.password())
			.build();

		// when & then
		mockMvc.perform(
				post("/auth/login")
					.content(objectMapper.writeValueAsString(loginRequest))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").value("로그인 성공"))
			.andExpect(header().string("Authorization", notNullValue()))
			.andExpect(cookie().exists("refresh_token"))
			.andDo(print());
	}

	@Test
	@DisplayName("[로그인 실패 테스트] 아이디 일치하지 않으면 LOGIN_FAIL 에러 코드 예외 발생")
	public void login_incorrect_login_id_fail_test () throws Exception {
		// given
		UserCreationRequest userCreationRequest = UserCreationRequest.builder()
			.name("test-name")
			.loginId("test-login-id-789")
			.password("test-password")
			.build();

		userService.register(userCreationRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId(userCreationRequest.loginId() + "1234")
			.password(userCreationRequest.password())
			.build();

		// when & then
		mockMvc.perform(
				post("/auth/login")
					.content(objectMapper.writeValueAsString(loginRequest))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$").value("존재하지 않는 유저입니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("[로그인 실패 테스트] 비밀번호 일치하지 않으면 LOGIN_FAIL 에러 코드 예외 발생")
	public void login_incorrect_password_fail_test () throws Exception {
		// given
		UserCreationRequest userCreationRequest = UserCreationRequest.builder()
			.name("test-name")
			.loginId("test-login-id-257")
			.password("test-password")
			.build();

		userService.register(userCreationRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId(userCreationRequest.loginId())
			.password(userCreationRequest.password() + "1234")
			.build();

		// when & then
		mockMvc.perform(
				post("/auth/login")
					.content(objectMapper.writeValueAsString(loginRequest))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$").value("비밀번호가 일치하지 않습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("[로그아웃 성공 테스트] 로그아웃 시 refresh 토큰 만료")
	public void logout_succeed_test () throws Exception {
		// given
		UserCreationRequest userCreationRequest = UserCreationRequest.builder()
			.name("test-name")
			.loginId("test-login-id-2319")
			.password("test-password")
			.build();

		userService.register(userCreationRequest);

		LoginRequest loginRequest = LoginRequest.builder()
			.loginId(userCreationRequest.loginId())
			.password(userCreationRequest.password())
			.build();

		// when & then
		String accessToken = mockMvc.perform(
				post("/auth/login")
					.content(objectMapper.writeValueAsString(loginRequest))
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").value("로그인 성공"))
			.andExpect(header().string("Authorization", notNullValue()))
			.andExpect(cookie().exists("refresh_token"))
			.andDo(print())
			.andReturn()
			.getResponse()
			.getHeader("Authorization");

		mockMvc.perform(
				get("/auth/logout")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", accessToken)
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").value("로그아웃 성공"))
			.andExpect(header().doesNotExist("Authorization"))
			.andDo(print());
	}
}