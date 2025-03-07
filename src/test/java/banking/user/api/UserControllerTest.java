package banking.user.api;

import banking.common.jwt.TestJwtUtil;
import banking.user.dto.response.UserResponse;
import banking.user.dto.request.UserCreationRequest;
import banking.user.entity.Role;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {

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
        UserCreationRequest userCreationRequest = UserCreationRequest.builder()
            .name("test-name")
            .loginId("test-login-id-123")
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
        UserCreationRequest userCreationRequest1 = UserCreationRequest.builder()
            .name("test-name")
            .loginId("test-login-id-234")
            .password("test-password")
            .build();

        userService.register(userCreationRequest1);

        UserCreationRequest userCreationRequest2 = UserCreationRequest.builder()
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
        UserCreationRequest userCreationRequest = UserCreationRequest.builder()
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
        UserCreationRequest userCreationRequest = UserCreationRequest.builder()
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
    @DisplayName("[유저 조회 성공 테스트] Access Token 유효하면 유저 상세 정보 반환")
    public void find_user_details_succeed_test () throws Exception {
        // given
        UserCreationRequest userCreationRequest = UserCreationRequest.builder()
            .name("test-name")
            .loginId("test-login-id-345")
            .password("test-password")
            .build();
        UserResponse userResponse = userService.register(userCreationRequest);

        String accessToken = TestJwtUtil.generateTestAccessToken(userResponse.userId(), Role.USER);

        mockMvc.perform(
            get("/users")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userResponse.userId()))
            .andExpect(jsonPath("$.name").value(userResponse.name()))
            .andExpect(jsonPath("$.loginId").value(userResponse.loginId()))
            .andDo(print());
    }

    @Test
    @DisplayName("[유저 조회 실패 테스트] access 토큰이 유효하지 않으면 유저 조회 불가능")
    public void find_user_details_fail_test () throws Exception {
        // given
        UserCreationRequest userCreationRequest = UserCreationRequest.builder()
            .name("test-name")
            .loginId("test-login-id-345")
            .password("test-password")
            .build();
        UserResponse userResponse = userService.register(userCreationRequest);

        String accessToken = TestJwtUtil.generateTestAccessToken(userResponse.userId(), Role.USER);

        // when & then
        mockMvc.perform(
            get("/users")
                .header("Authorization", "Bearer " + accessToken + "abc")
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."))
            .andDo(print());
    }
}
