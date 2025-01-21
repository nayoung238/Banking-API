package SN.BANK.transaction.integration;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.enums.Currency;
import SN.BANK.transaction.dto.request.TransactionFindDetailRequest;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.enums.TransactionType;
import SN.BANK.transaction.service.DepositService;
import SN.BANK.transaction.service.TransactionRecoverService;
import SN.BANK.transaction.service.TransactionService;
import SN.BANK.transaction.service.WithdrawService;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    DepositService depositService;

    @Autowired
    WithdrawService withdrawService;

    @Autowired
    TransactionRecoverService recoverService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    ObjectMapper objectMapper;

    MockHttpSession session;

    Users sender;
    Users receiver;
    Account senderAccount;
    Account receiverAccount;

    @BeforeEach
    void setUp() {

        sender = Users.builder()
                .name("테스터1")
                .loginId("test1234")
                .password("test1234")
                .build();

        receiver = Users.builder()
                .name("테스터2")
                .loginId("test4321")
                .password("test4321")
                .build();

        senderAccount = Account.builder()
                .user(sender)
                .password("1234")
                .accountNumber("11111111111111")
                .money(BigDecimal.valueOf(10000.00))
                .accountName("test account1")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();

        receiverAccount = Account.builder()
                .user(receiver)
                .password("4321")
                .accountNumber("22222222222222")
                .money(BigDecimal.valueOf(10000.00))
                .accountName("test account2")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();

        sender = usersRepository.save(sender);
        receiver = usersRepository.save(receiver);
        senderAccount = accountRepository.save(senderAccount);
        receiverAccount = accountRepository.save(receiverAccount);

        session = new MockHttpSession();
    }

    @Test
    @DisplayName("이체 성공 테스트")
    void transfer() throws Exception {

        session.setAttribute("user", sender.getId());

        BigDecimal amount = BigDecimal.valueOf(5000.00);
        BigDecimal balance = senderAccount.getMoney().subtract(amount);

        TransactionRequest transactionRequest =
                TransactionRequest.builder()
                        .accountPassword("1234")
                        .senderAccountId(senderAccount.getId())
                        .receiverAccountId(receiverAccount.getId())
                        .amount(amount)
                        .build();

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session)
                        .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value(TransactionType.WITHDRAWAL.name()))
                .andExpect(jsonPath("$.senderAccountId").value(senderAccount.getId()))
                .andExpect(jsonPath("$.receiverAccountId").value(receiverAccount.getId()))
                .andExpect(jsonPath("$.amount").value(amount))
                .andExpect(jsonPath("$.balance").value(balance))
                .andDo(print());

    }

    @Test
    @DisplayName("receiveFrom 호출 시 예외 발생 - 송금 계좌 잔액 롤백 검증")
    void testRollbackOnReceiveFromError() {

        // Given
        BigDecimal amount = BigDecimal.valueOf(5000.00);
        BigDecimal originalBalance = senderAccount.getMoney();

        TransactionRequest txRequest = TransactionRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();


        // When
        // 송금 처리
        Account updatedSenderAccount = withdrawService.sendTo(sender.getId(), txRequest);

        try {
            transactionTemplate.execute(status -> {
                // 수신 처리
                depositService.receiveFrom(txRequest, updatedSenderAccount, BigDecimal.ONE, amount);

                // 의도적으로 예외 발생 -> 롤백 트리거
                throw new RuntimeException("Fail Transaction");
            });
        } catch (RuntimeException e) {
            recoverService.rollbackSenderAccount(txRequest);
        }

        // Then
        // 데이터가 원래 상태로 롤백되었는지 검증
        assertEquals(originalBalance, senderAccount.getMoney());
    }

    @Test
    @DisplayName("모든 이체 내역 조회 테스트")
    void findAllTransaction() throws Exception {

        session.setAttribute("user", sender.getId());

        BigDecimal amount = BigDecimal.valueOf(5000.00);

        TransactionRequest transactionRequest1 =
                TransactionRequest.builder()
                        .accountPassword("1234")
                        .senderAccountId(senderAccount.getId())
                        .receiverAccountId(receiverAccount.getId())
                        .amount(amount)
                        .build();

        TransactionRequest transactionRequest2 =
                TransactionRequest.builder()
                        .accountPassword("4321")
                        .senderAccountId(receiverAccount.getId())
                        .receiverAccountId(senderAccount.getId())
                        .amount(amount)
                        .build();

        transactionService.createTransaction(sender.getId(), transactionRequest1);
        transactionService.createTransaction(receiver.getId(), transactionRequest2);

        mockMvc.perform(get("/transfer/history/{accountId}", senderAccount.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // 거래내역 2개
                .andExpect(jsonPath("$[*].accountNumber", everyItem(is(senderAccount.getAccountNumber()))))
                .andExpect(jsonPath("$[*].balance", Matchers.containsInAnyOrder(5000.00, 10000.00)))
                .andDo(print());
    }

    @Test
    @DisplayName("이체 내역 단건 조회 테스트")
    void findTransaction() throws Exception {

        session.setAttribute("user", sender.getId());

        BigDecimal amount = BigDecimal.valueOf(5000.00);
        BigDecimal balance = senderAccount.getMoney().subtract(amount);

        TransactionRequest transactionRequest =
                TransactionRequest.builder()
                        .accountPassword("1234")
                        .senderAccountId(senderAccount.getId())
                        .receiverAccountId(receiverAccount.getId())
                        .amount(amount)
                        .build();

        TransactionResponse tx = transactionService.createTransaction(sender.getId(), transactionRequest);

        TransactionFindDetailRequest txFindDetailRequest = TransactionFindDetailRequest.builder()
                .accountId(senderAccount.getId())
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
