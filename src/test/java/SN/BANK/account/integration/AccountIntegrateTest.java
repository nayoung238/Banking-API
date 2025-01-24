package SN.BANK.account.integration;

//import SN.BANK.account.dto.request.CreateAccountRequest;
//import SN.BANK.account.dto.response.CreateAccountResponse;
//import SN.BANK.account.service.AccountService;
//import SN.BANK.account.enums.Currency;
//import SN.BANK.users.entity.Users;
//import SN.BANK.users.repository.UsersRepository;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.hamcrest.Matchers;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockHttpSession;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Transactional
//@ActiveProfiles("test")
//class AccountIntegrateTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @Autowired
//    AccountService accountService;
//
//    @Autowired
//    UsersRepository usersRepository;
//
//    @Autowired
//    ObjectMapper objectMapper;
//
//    MockHttpSession session;
//
//    @BeforeEach
//    void setUp() {
//        session = new MockHttpSession();
//    }
//
//    @Test
//    @DisplayName("계좌 개설 통합 테스트")
//    void createAccount() throws Exception {
//        // given
//        Users user = Users.builder()
//                .name("테스트이름")
//                .loginId("test1234")
//                .password("test1234")
//                .build();
//
//        Users savedUser = usersRepository.save(user);
//
//        CreateAccountRequest createAccountRequest = CreateAccountRequest.builder()
//                .password("1234")
//                .currency(Currency.KRW)
//                .accountName("Test Account")
//                .build();
//
//        session.setAttribute("user", savedUser.getId());
//
//        // when
//        mockMvc.perform(post("/accounts")
//                        .session(session)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(createAccountRequest)))
//                // then
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.accountId").isNotEmpty())
//                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
//                .andExpect(jsonPath("$.currency").value(Currency.KRW.name()))
//                .andExpect(jsonPath("$.createdAt").exists())
//                .andDo(print());
//    }
//
//    @Test
//    @DisplayName("전체 계좌 조회 테스트")
//    void findAllAccount() throws Exception {
//        // given
//        Users user = Users.builder()
//                .name("테스트이름")
//                .loginId("test1234")
//                .password("test1234")
//                .build();
//
//        Users savedUser = usersRepository.save(user);
//
//        CreateAccountRequest createAccountRequest1 = CreateAccountRequest.builder()
//                .accountName("Test Account")
//                .password("1234")
//                .currency(Currency.KRW)
//                .build();
//
//        CreateAccountRequest createAccountRequest2 = CreateAccountRequest.builder()
//                .accountName("Test Account")
//                .password("1234")
//                .currency(Currency.KRW)
//                .build();
//
//        CreateAccountResponse account1 = accountService.createAccount(savedUser.getId(), createAccountRequest1);
//        CreateAccountResponse account2 = accountService.createAccount(savedUser.getId(), createAccountRequest2);
//
//        session.setAttribute("user", savedUser.getId());
//
//        // when
//        mockMvc.perform(get("/accounts")
//                        .session(session)
//                        .contentType(MediaType.APPLICATION_JSON))
//                // then
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2))
//                .andDo(print());
//    }

//    @Test
//    @DisplayName("계좌 단일 조회 테스트")
//    void findAccount() throws Exception {
//        // given
//        Users user = Users.builder()
//                .name("테스트이름")
//                .loginId("test1234")
//                .password("test1234")
//                .build();
//
//        Users savedUser = usersRepository.save(user);
//
//        CreateAccountRequest createAccountRequest = CreateAccountRequest.builder()
//                .accountName("Test Account")
//                .password("1234")
//                .currency(Currency.KRW)
//                .build();
//
//        CreateAccountResponse account = accountService.createAccount(savedUser.getId(), createAccountRequest);
//
//        session.setAttribute("user", savedUser.getId());
//
//        // when
//        mockMvc.perform(get("/accounts/{id}", account.accountId())
//                        .session(session)
//                        .contentType(MediaType.APPLICATION_JSON))
//                // then
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accountNumber").value(Matchers.hasLength(14)))
//                .andExpect(jsonPath("$.accountName").value("Test Account"))
//                .andDo(print());
//    }
//}