package SN.BANK.transaction.integration;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.enums.Currency;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.enums.TransactionType;
import SN.BANK.users.entity.Users;
import SN.BANK.users.repository.UsersRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class TransferIntegrateTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    AccountRepository accountRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    MockHttpSession session;

    Users user1;
    Users user2;
    Account account1;
    Account account2;

    @BeforeEach
    void setUp() {

        user1 = Users.builder()
                .name("테스트1")
                .loginId("test1234")
                .password("test1234")
                .build();

        user2 = Users.builder()
                .name("테스트2")
                .loginId("test4321")
                .password("test4321")
                .build();

        account1 = Account.builder()
                .user(user1)
                .password("1234")
                .accountNumber("11111111111111")
                .money(BigDecimal.valueOf(10000))
                .accountName("test account1")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();

        account2 = Account.builder()
                .user(user2)
                .password("1234")
                .accountNumber("22222222222222")
                .money(BigDecimal.valueOf(10000))
                .accountName("test account2")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();

        user1 = usersRepository.save(user1);
        user2 = usersRepository.save(user2);
        account1 = accountRepository.save(account1);
        account2 = accountRepository.save(account2);

        session = new MockHttpSession();
        session.setAttribute("user", user1.getId());
    }

    @Test
    @DisplayName("이체 성공 테스트")
    void transfer() throws Exception {

        BigDecimal amount = BigDecimal.valueOf(5000);
        BigDecimal balance = account1.getMoney().subtract(amount);

        TransactionRequest transactionRequest =
                TransactionRequest.builder()
                        .accountPassword("1234")
                        .senderAccountId(account1.getId())
                        .receiverAccountId(account2.getId())
                        .amount(amount)
                        .build();

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value(TransactionType.WITHDRAWAL.name()))
                .andExpect(jsonPath("$.senderAccountId").value(account1.getId()))
                .andExpect(jsonPath("$.receiverAccountId").value(account2.getId()))
                .andExpect(jsonPath("$.amount").value(amount))
                .andExpect(jsonPath("$.balance").value(balance))
                .andDo(print());

    }


}
