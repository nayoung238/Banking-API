package banking.account.api;

import banking.account.dto.request.AccountCreationRequestDto;
import banking.account.dto.response.AccountResponseDto;
import banking.account.entity.Account;
import banking.account.repository.AccountRepository;
import banking.account.service.AccountService;
import banking.account.enums.Currency;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.fixture.dto.UserCreationRequestDtoFixture;
import banking.user.dto.request.UserCreationRequestDto;
import banking.user.dto.response.UserResponseDto;
import banking.user.repository.UserRepository;
import banking.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountService accountService;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    ObjectMapper objectMapper;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("[계좌 개설 성공 테스트] 계좌 개설 성공 시 계좌 정보 반환")
    void create_account_succeed_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_1.createUserCreationRequestDto();
        UserResponseDto userResponse = userService.register(userCreationRequest);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", userResponse.userId());

        AccountCreationRequestDto request = AccountCreationRequestDto.builder()
            .password("62324")
            .currency(Currency.KRW)
            .accountName("Test Account")
            .build();

        // when & then
        mockMvc.perform(
            post("/accounts")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountId").isNumber())
            .andExpect(jsonPath("$.accountNumber").isNotEmpty())
            .andExpect(jsonPath("$.currency").value(request.currency().toString()))
            .andExpect(jsonPath("$.balance").value(comparesEqualTo(BigDecimal.ZERO.intValue())))
            .andExpect(jsonPath("$.accountName").value(request.accountName()))
            .andDo(print());

        session.invalidate();
    }

    @Test
    @DisplayName("[계좌 조회 성공 테스트] 단일 계좌 조회 시 계좌 정보 반환")
    void find_single_account_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_1.createUserCreationRequestDto();
        UserResponseDto userResponse = userService.register(userCreationRequest);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", userResponse.userId());

        AccountCreationRequestDto request = AccountCreationRequestDto.builder()
            .password("62324")
            .currency(Currency.KRW)
            .accountName("Test Account")
            .build();

        AccountResponseDto response = accountService.createAccount(userResponse.userId(), request);
        Account account = accountRepository.findByAccountNumber(response.accountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // when & then
        mockMvc.perform(get("/accounts/{id}", account.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(account.getId()))
            .andExpect(jsonPath("$.accountNumber").value(account.getAccountNumber()))
            .andExpect(jsonPath("$.currency").value(request.currency().toString()))
            .andDo(print());

        session.invalidate();
    }

    @Test
    @DisplayName("[계좌 조회 성공 테스트] 전체 계좌 조회 시 모든 계좌 정보 반환")
    void find_all_account_test () throws Exception {
        // given
        UserCreationRequestDto userCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_1.createUserCreationRequestDto();
        UserResponseDto userResponse = userService.register(userCreationRequest);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", userResponse.userId());

        AccountCreationRequestDto requestKrwAccount = AccountCreationRequestDto.builder()
            .password("62324")
            .currency(Currency.KRW)
            .accountName("Test KRW Account")
            .build();

        AccountCreationRequestDto requestUsdAccount = AccountCreationRequestDto.builder()
            .password("95224")
            .currency(Currency.USD)
            .accountName("Test USD Account")
            .build();

        accountService.createAccount(userResponse.userId(), requestKrwAccount);
        accountService.createAccount(userResponse.userId(), requestUsdAccount);

        // when & then
        mockMvc.perform(get("/accounts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].currency").value(Matchers.containsInAnyOrder("KRW", "USD")))
            .andDo(print());

        session.invalidate();
    }
}