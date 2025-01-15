package SN.BANK.account.Integrate;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.domain.Users;
import SN.BANK.domain.enums.Currency;
import SN.BANK.user.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountIntegrateTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsersRepository usersRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("계좌 개설 통합 테스트")
    void createAccount() throws Exception {

        // given
        Users user = Users.builder()
                .name("테스트이름")
                .loginId("test1234")
                .password("test1234")
                .build();

        Users savedUser = usersRepository.save(user);

        CreateAccountRequest createAccountRequest = CreateAccountRequest.builder()
                .accountName("Test Account")
                .password("1234")
                .currency(Currency.KRW)
                .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", savedUser.getId());

        // when
        mockMvc.perform(post("/accounts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAccountRequest)))
                // then
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(1L))
                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                .andExpect(jsonPath("$.currency").value(Currency.KRW.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andDo(print());

    }

}