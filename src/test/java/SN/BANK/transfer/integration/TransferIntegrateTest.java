package SN.BANK.transfer.integration;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.enums.Currency;
import SN.BANK.payment.enums.PaymentTag;
import SN.BANK.transfer.dto.request.TransferRequest;
import SN.BANK.users.entity.Users;
import SN.BANK.users.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
//    MockHttpSession session;

    Users user1;
    Users user2;
    Account account1;
    Account account2;

    @BeforeEach
    void setUp() {
//        session = new MockHttpSession();

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

    }

    @Test
    void transfer() throws Exception {

        TransferRequest transferRequest =
                TransferRequest.builder()
                        .fromAccountId(account1.getId())
                        .accountPassword("1234")
                        .toAccountId(account2.getId())
                        .amount(BigDecimal.valueOf(5000))
                        .currency(Currency.KRW)
                        .build();

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentTag").value(PaymentTag.이체.name()))
                .andExpect(jsonPath("$.depositId").value(account1.getId()))
                .andExpect(jsonPath("$.withdrawId").value(account2.getId()))
                .andExpect(jsonPath("$.balance").value(5000))
                .andDo(print());
    }


}
