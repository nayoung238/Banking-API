package banking.user.api;

import banking.user.dto.response.UserResponseDto;
import banking.user.dto.request.LoginRequestDto;
import banking.user.dto.request.UserCreationRequestDto;
import banking.user.repository.UserRepository;
import banking.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class UserControllerIntegrationTest {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @AfterEach
    void afterEach() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("[회원 가입 성공 테스트] 회원 가입 성공 시 유저 상세 정보 반환")
    public void register_user_succeed_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId("test-login-id")
            .password("test-password")
            .build();

        // when & then
       mockMvc.perform(
           post("/users")
               .content(objectMapper.writeValueAsString(userCreationRequest))
               .contentType(MediaType.APPLICATION_JSON)
           )
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.userId").isNumber())
           .andExpect(jsonPath("$.name").value(userCreationRequest.name()))
           .andExpect(jsonPath("$.loginId").value(userCreationRequest.loginId()))
           .andDo(print());
    }

    @Test
    @DisplayName("[회원 가입 실패 테스트] 로그인 아이디 중복 시 DUPLICATE_LOGIN_ID 에러 코드 예외 발")
    public void duplicate_login_id_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest1 = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId("test-login-id")
            .password("test-password")
            .build();

        userService.register(userCreationRequest1);

        UserCreationRequestDto userCreationRequest2 = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId(userCreationRequest1.loginId()) // 중복
            .password("test-password")
            .build();

        // when & then
        mockMvc.perform(
            post("/users")
                .content(objectMapper.writeValueAsString(userCreationRequest2))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$").value("이미 사용 중인 로그인 아이디입니다."))
            .andDo(print());
    }

    @Test
    @DisplayName("[회원 가입 실패 테스트] name 데이터 필수")
    public void name_valid_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDto.builder()
            .name(null)
            .loginId("test-id")
            .password("test-password")
            .build();

        // when & then
        mockMvc.perform(
            post("/users")
                .content(objectMapper.writeValueAsString(userCreationRequest))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$").value("이름은 필수입니다."))
            .andDo(print());
    }

    @Test
    @DisplayName("[회원 가입 실패 테스트] Login Id 데이터 필수")
    public void login_id_valid_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId(" ")
            .password("test-password")
            .build();

        // when & then
        mockMvc.perform(
                post("/users")
                    .content(objectMapper.writeValueAsString(userCreationRequest))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$").value("로그인 아이디는 필수입니다."))
            .andDo(print());
    }

    @Test
    @DisplayName("[로그인 성공 테스트] 로그인 성공 시 세션 연결")
    public void login_succeed_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId("test-login-id")
            .password("test-password")
            .build();
        UserResponseDto userResponse = userService.register(userCreationRequest);

        LoginRequestDto loginRequest = LoginRequestDto.builder()
            .loginId(userCreationRequest.loginId())
            .password(userCreationRequest.password())
            .build();

        MockHttpSession session = new MockHttpSession();

        // when & then
        mockMvc.perform(
            post("/users/login")
                .content(objectMapper.writeValueAsString(loginRequest))
                .contentType(MediaType.APPLICATION_JSON).session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("로그인 성공"))
            .andDo(print());

        Assertions.assertEquals(userResponse.userId(), (Long) session.getAttribute("user"));
        session.invalidate();
    }

    @Test
    @DisplayName("[로그인 실패 테스트] 아이디 일치하지 않으면 LOGIN_FAIL 에러 코드 예외 발생")
    public void login_incorrect_login_id_fail_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId("test-login-id")
            .password("test-password")
            .build();
        userService.register(userCreationRequest);

        LoginRequestDto loginRequest = LoginRequestDto.builder()
            .loginId(userCreationRequest.loginId() + "1234")
            .password(userCreationRequest.password())
            .build();

        MockHttpSession session = new MockHttpSession();

        // when & then
        mockMvc.perform(
            post("/users/login")
                .content(objectMapper.writeValueAsString(loginRequest))
                .contentType(MediaType.APPLICATION_JSON).session(session)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$").value("아이디 또는 비밀번호가 일치하지 않습니다."))
            .andDo(print());

        Assertions.assertNull(session.getAttribute("user"));
    }

    @Test
    @DisplayName("[로그인 실패 테스트] 비밀번호 일치하지 않으면 LOGIN_FAIL 에러 코드 예외 발생")
    public void login_incorrect_password_fail_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId("test-login-id")
            .password("test-password")
            .build();
        userService.register(userCreationRequest);

        LoginRequestDto loginRequest = LoginRequestDto.builder()
            .loginId(userCreationRequest.loginId())
            .password(userCreationRequest.password() + "1234")
            .build();

        MockHttpSession session = new MockHttpSession();

        // when & then
        mockMvc.perform(
                post("/users/login")
                    .content(objectMapper.writeValueAsString(loginRequest))
                    .contentType(MediaType.APPLICATION_JSON).session(session)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$").value("아이디 또는 비밀번호가 일치하지 않습니다."))
            .andDo(print());

        Assertions.assertNull(session.getAttribute("user"));
    }

    @Test
    @DisplayName("[유저 조회 성공 테스트] 로그인 후 유저 조회 시 상세 정보 반환")
    public void find_user_details_succeed_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId("test-login-id")
            .password("test-password")
            .build();
        UserResponseDto userResponse = userService.register(userCreationRequest);

        LoginRequestDto loginRequest = LoginRequestDto.builder()
            .loginId(userCreationRequest.loginId())
            .password(userCreationRequest.password())
            .build();

        MockHttpSession session = new MockHttpSession();

        // when & then
        mockMvc.perform(
            post("/users/login")
                .content(objectMapper.writeValueAsString(loginRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("로그인 성공"))
            .andDo(print());

        mockMvc.perform(
            get("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userResponse.userId()))
            .andExpect(jsonPath("$.name").value(userResponse.name()))
            .andExpect(jsonPath("$.loginId").value(userResponse.loginId()))
            .andDo(print());

        session.invalidate();
    }

    @Test
    @DisplayName("[유저 조회 실패 테스트] 세션 연결 없으면 조회 불가능")
    public void find_user_details_fail_test () throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(
            get("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$").value("인증이 실패하였습니다."))
            .andDo(print());
    }

    @Test
    @DisplayName("[로그아웃 성공 테스트] 세션 연결 시 로그아웃 가능")
    public void logout_succeed_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDto.builder()
            .name("test-name")
            .loginId("test-login-id")
            .password("test-password")
            .build();
        UserResponseDto userResponse = userService.register(userCreationRequest);

        LoginRequestDto loginRequest = LoginRequestDto.builder()
            .loginId(userCreationRequest.loginId())
            .password(userCreationRequest.password())
            .build();

        MockHttpSession session = new MockHttpSession();

        // when & then
        mockMvc.perform(
            post("/users/login").
                content(objectMapper.writeValueAsString(loginRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("로그인 성공"))
            .andDo(print());

        mockMvc.perform(
            get("/users/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("로그아웃 성공"))
            .andDo(print());

        mockMvc.perform(
            get("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$").value("인증이 실패하였습니다."))
            .andDo(print());
    }
}
