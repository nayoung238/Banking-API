package SN.BANK.integration;

import SN.BANK.users.entity.Users;
import SN.BANK.users.dto.LoginDto;
import SN.BANK.users.dto.UsersRequestDto;
import SN.BANK.users.repository.UsersRepository;
import SN.BANK.users.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class UsersTest {
    @Autowired
    UsersService usersService;
    @Autowired
    UsersRepository usersRepository;
    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();
    private MockHttpSession session;

    @BeforeEach
    void setup() {
        session = new MockHttpSession();
    }


    @Test
    @DisplayName("회원가입 성공 테스트")
    public void joinTest() throws Exception {
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId(loginId).password(password).build();
        String body = objectMapper.writeValueAsString(usersRequestDto);
        ResultActions resultActions =mockMvc.perform(post("/users").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andDo(print());
        Long userId = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).asLong();
        Users users = usersRepository.findById(userId).orElse(null);
        assertAll(
                ()->assertEquals(users.getName(),name),
                ()->assertEquals(users.getLoginId(),loginId)
        );
    }

    @Test
    @DisplayName("회원 가입 실패 테스트 - 중복 아이디")
    public void joinFailTest() throws Exception {
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId(loginId).password(password).build();
        String body = objectMapper.writeValueAsString(usersRequestDto);
        usersRepository.save(Users.builder().loginId(loginId).build());
        mockMvc.perform(post("/users").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("중복된 아이디입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    public void loginTest() throws Exception {
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId(loginId).password(password).build();
        usersService.join(usersRequestDto);
        LoginDto loginDto = LoginDto.builder().loginId(loginId).password(password).build();
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("로그인 성공"))
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 틀린 아이디")
    public void loginFailIdTest() throws Exception {
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId("wrongId").password(password).build();
        usersService.join(usersRequestDto);
        LoginDto loginDto = LoginDto.builder().loginId(loginId).password(password).build();
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("아이디 또는 비밀번호가 틀립니다."))
                .andDo(print());
    }
    @Test
    @DisplayName("로그인 실패 테스트 - 틀린 비밀번호")
    public void loginFailPasswordTest() throws Exception {
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId(loginId).password("wrongPassword").build();
        usersService.join(usersRequestDto);
        LoginDto loginDto = LoginDto.builder().loginId(loginId).password(password).build();
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("아이디 또는 비밀번호가 틀립니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("로그인 성공 테스트 후 유저 정보 호출 테스트")
    public void getUserTest() throws Exception {
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId(loginId).password(password).build();
        Long userId = usersService.join(usersRequestDto);
        LoginDto loginDto = LoginDto.builder().loginId(loginId).password(password).build();
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("로그인 성공"))
                .andDo(print());

        mockMvc.perform(get("/users").contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.loginId").value(loginId))
                .andDo(print());

    }

    @Test
    @DisplayName("유저 정보 호출 실패 테스트 - 존재하지 않는 id")
    public void getUserFailTest() throws Exception {
        mockMvc.perform(get("/users").contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("존재하지 않는 유저입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("로그아웃 테스트")
    public void logoutTest() throws Exception {
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId(loginId).password(password).build();
        Long userId = usersService.join(usersRequestDto);
        LoginDto loginDto = LoginDto.builder().loginId(loginId).password(password).build();
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/users/login").content(body).contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("로그인 성공"))
                .andDo(print());

        mockMvc.perform(get("/users").contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.loginId").value(loginId))
                .andDo(print());

        mockMvc.perform(get("/users/logout").content(body).contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("로그아웃 성공"))
                .andDo(print());

        mockMvc.perform(get("/users").contentType(MediaType.APPLICATION_JSON).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("존재하지 않는 유저입니다."))
                .andDo(print());
    }


}
