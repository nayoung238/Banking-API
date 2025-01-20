package SN.BANK.transaction.integration;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.enums.Currency;
import SN.BANK.transaction.dto.request.TransactionFindDetailRequest;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.enums.TransactionType;
import SN.BANK.transaction.service.TransactionService;
import SN.BANK.users.entity.Users;
import SN.BANK.users.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    TransactionService transactionService;

    @Autowired
    ObjectMapper objectMapper;

    MockHttpSession session;

    Users user1;
    Users user2;
    Account account1;
    Account account2;

    @BeforeEach
    void setUp() {

        user1 = Users.builder()
                .name("테스터1")
                .loginId("test1234")
                .password("test1234")
                .build();

        user2 = Users.builder()
                .name("테스터2")
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
                .password("4321")
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
    }

    @Test
    @DisplayName("이체 성공 테스트")
    void transfer() throws Exception {

        session.setAttribute("user", user1.getId());

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

    @Test
    @DisplayName("모든 이체 내역 조회 테스트")
    void findAllTransaction() throws Exception {

        session.setAttribute("user", user1.getId());

        BigDecimal amount = BigDecimal.valueOf(5000);

        TransactionRequest transactionRequest1 =
                TransactionRequest.builder()
                        .accountPassword("1234")
                        .senderAccountId(account1.getId())
                        .receiverAccountId(account2.getId())
                        .amount(amount)
                        .build();

        TransactionRequest transactionRequest2 =
                TransactionRequest.builder()
                        .accountPassword("4321")
                        .senderAccountId(account2.getId())
                        .receiverAccountId(account1.getId())
                        .amount(amount)
                        .build();

        transactionService.createTransaction(user1.getId(), transactionRequest1);
        transactionService.createTransaction(user2.getId(), transactionRequest2);

        mockMvc.perform(get("/transfer/history/{accountId}", account1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // 거래내역 2개
                .andExpect(jsonPath("$[*].accountNumber", everyItem(is(account1.getAccountNumber()))))
                .andExpect(jsonPath("$[*].balance", Matchers.containsInAnyOrder(5000, 10000)))
                .andDo(print());
    }

    @Test
    @DisplayName("이체 내역 단건 조회 테스트")
    void findTransaction() throws Exception {

        session.setAttribute("user", user1.getId());

        BigDecimal amount = BigDecimal.valueOf(5000);
        BigDecimal balance = account1.getMoney().subtract(amount);

        TransactionRequest transactionRequest =
                TransactionRequest.builder()
                        .accountPassword("1234")
                        .senderAccountId(account1.getId())
                        .receiverAccountId(account2.getId())
                        .amount(amount)
                        .build();

        TransactionResponse tx = transactionService.createTransaction(user1.getId(), transactionRequest);

        TransactionFindDetailRequest txFindDetailRequest = TransactionFindDetailRequest.builder()
                .accountId(account1.getId())
                .transactionId(tx.getTransactionId())
                .build();

        mockMvc.perform(get("/transfer/history/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session)
                        .content(objectMapper.writeValueAsString(txFindDetailRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value(TransactionType.WITHDRAWAL.name()))
                .andExpect(jsonPath("$.othersName").value("테스터2"))
                .andExpect(jsonPath("$.othersAccountNumber").value("22222222222222"))
                .andExpect(jsonPath("$.amount").value(amount))
                .andExpect(jsonPath("$.balance").value(balance))
                .andDo(print());
    }


}
