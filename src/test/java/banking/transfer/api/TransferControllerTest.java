package banking.transfer.api;

import banking.account.dto.request.DepositRequest;
import banking.account.entity.Account;
import banking.account.enums.AccountStatus;
import banking.account.enums.Currency;
import banking.account.repository.AccountRepository;
import banking.account.service.AccountBalanceService;
import banking.common.jwt.TestJwtUtil;
import banking.transfer.dto.request.TransferDetailsRequest;
import banking.transfer.dto.request.TransferRequest;
import banking.transfer.dto.response.TransferDetailResponse;
import banking.transfer.enums.TransferType;
import banking.transfer.service.TransferService;
import banking.user.entity.Role;
import banking.user.entity.User;
import banking.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountBalanceService accountBalanceService;

    @Autowired
    TransferService transferService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AccountRepository accountRepository;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("[이체 성공 테스트] KRW-KRW 계좌 간 이체")
    void transfer_succeed_test () throws Exception {
        // given1 - 출금 계좌에 입금
        User withdrawalAccountUser = createUser("login-id-135", "user-1");
        Account withdrawalAccount = createAccount(withdrawalAccountUser, Currency.KRW);

        final BigDecimal depositAmount = BigDecimal.valueOf(10000);
        deposit(withdrawalAccount, depositAmount);

        // given2 - 입금 계좌 생성
        User depositAccountUser = createUser("login-id-531", "user-2");
        Account depositAccount = createAccount(depositAccountUser, Currency.KRW);

        // given3 - 이체 요청 DTO 생성
        final BigDecimal transferAmount = new BigDecimal("2000.0");
        TransferRequest transferRequest = TransferRequest.builder()
            .withdrawalAccountId(withdrawalAccount.getId())
            .withdrawalAccountPassword(withdrawalAccount.getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(transferAmount)
            .build();

        // when & then
        mockMvc.perform(
            post("/transfer")
                .content(objectMapper.writeValueAsString(transferRequest))
                .header("Authorization", "Bearer " + TestJwtUtil.generateTestAccessToken(withdrawalAccountUser.getId(), Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").isNumber())
            .andExpect(jsonPath("$.withdrawalAccountNumber").value(withdrawalAccount.getAccountNumber()))
            .andExpect(jsonPath("$.depositAccountNumber").value(transferRequest.depositAccountNumber()))
            .andExpect(jsonPath("$.receiverName").value(depositAccountUser.getName()))
            .andExpect(jsonPath("$.transferType").value(TransferType.WITHDRAWAL.name()))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(depositAccount.getCurrency() + "/" + withdrawalAccount.getCurrency()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(transferRequest.amount().intValue())))
            .andExpect(jsonPath("$.balancePostTransaction").value(comparesEqualTo(depositAmount.subtract(transferAmount).intValue())))
            .andDo(print());
    }

    @Test
    @DisplayName("[이체 실패 테스트] 잔액 부족 시 400 에러 반환")
    void transfer_insufficient_balance_test () throws Exception {
        // given1 - 출금 계좌에 입금
        User withdrawalAccountUser = createUser("login-id-246", "user-1");
        Account withdrawalAccount = createAccount(withdrawalAccountUser, Currency.KRW);

        final BigDecimal depositAmount = BigDecimal.valueOf(10000);
        deposit(withdrawalAccount, depositAmount);

        // given2 - 입금 계좌 생성
        User depositAccountUser = createUser("login-id-642", "user-2");
        Account depositAccount = createAccount(depositAccountUser, Currency.KRW);

        // given3 - 이체 요청 DTO 생성
        final BigDecimal transferAmount = depositAmount.multiply(new BigDecimal(2));
        TransferRequest transferRequest = TransferRequest.builder()
            .withdrawalAccountId(withdrawalAccount.getId())
            .withdrawalAccountPassword(withdrawalAccount.getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(transferAmount)
            .build();

        // when & then
        mockMvc.perform(
            post("/transfer")
                .content(objectMapper.writeValueAsString(transferRequest))
                .header("Authorization", "Bearer " + TestJwtUtil.generateTestAccessToken(withdrawalAccountUser.getId(), Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("잔액이 부족합니다.")))
            .andDo(print());
    }

    @Test
    @DisplayName("[조회 성공 테스트] 이체 내역 단건 조회")
    void findTransaction() throws Exception {
        // given1 - 출금 계좌에 입금
        User withdrawalAccountUser = createUser("login-id-357", "user-1");
        Account withdrawalAccount = createAccount(withdrawalAccountUser, Currency.KRW);

        final BigDecimal depositAmount = BigDecimal.valueOf(10000);
        deposit(withdrawalAccount, depositAmount);

        // given2 - 입금 계좌 생성
        User depositAccountUser = createUser("login-id-753", "user-2");
        Account depositAccount = createAccount(depositAccountUser, Currency.KRW);

        // given3 - 이체 요청 DTO 생성
        final BigDecimal transferAmount = new BigDecimal("2000.0");
        TransferRequest transferRequest = TransferRequest.builder()
            .withdrawalAccountId(withdrawalAccount.getId())
            .withdrawalAccountPassword(withdrawalAccount.getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(transferAmount)
            .build();

        TransferDetailResponse transferDetailResponse = transferService.transfer(withdrawalAccountUser.getId(), transferRequest);

        TransferDetailsRequest transferDetailsRequest = TransferDetailsRequest.builder()
            .accountId(withdrawalAccount.getId())
            .transferId(transferDetailResponse.transferId())
            .build();

        // when & then
        mockMvc.perform(
            get("/transfer")
                .content(objectMapper.writeValueAsString(transferDetailsRequest))
                .header("Authorization", "Bearer " + TestJwtUtil.generateTestAccessToken(withdrawalAccountUser.getId(), Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").value(transferDetailResponse.transferId()))
            .andExpect(jsonPath("$.withdrawalAccountNumber").value(withdrawalAccount.getAccountNumber()))
            .andExpect(jsonPath("$.depositAccountNumber").value(transferRequest.depositAccountNumber()))
            .andExpect(jsonPath("$.receiverName").value(depositAccountUser.getName()))
            .andExpect(jsonPath("$.transferType").value(TransferType.WITHDRAWAL.name()))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(depositAccount.getCurrency() + "/" + withdrawalAccount.getCurrency()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(transferRequest.amount().intValue())))
            .andExpect(jsonPath("$.balancePostTransaction").value(comparesEqualTo(depositAmount.subtract(transferAmount).intValue())))
            .andDo(print());
    }

    @Test
    @DisplayName("[조회 성공 테스트] 모든 이체 내역 조회")
    void findAllTransaction() throws Exception {
        // given1 - 출금 계좌에 입금
        User withdrawalAccountUser = createUser("login-id-468", "user-1");
        Account withdrawalAccount = createAccount(withdrawalAccountUser, Currency.KRW);

        final BigDecimal depositAmount = BigDecimal.valueOf(10000);
        deposit(withdrawalAccount, depositAmount);

        // given2 - 입금 계좌 생성
        User depositAccountUser = createUser("login-id-864", "user-2");
        Account depositAccount = createAccount(depositAccountUser, Currency.KRW);

        // given3 - 이체 요청 DTO 생성
        final BigDecimal transferAmount = new BigDecimal("1000.0");
        TransferRequest transferRequest = TransferRequest.builder()
            .withdrawalAccountId(withdrawalAccount.getId())
            .withdrawalAccountPassword(withdrawalAccount.getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(transferAmount)
            .build();

        transferService.transfer(withdrawalAccountUser.getId(), transferRequest);
        transferService.transfer(withdrawalAccountUser.getId(), transferRequest);
        transferService.transfer(withdrawalAccountUser.getId(), transferRequest);

        mockMvc.perform(
            get("/transfer/history/{accountId}", withdrawalAccount.getId())
                .header("Authorization", "Bearer " + TestJwtUtil.generateTestAccessToken(withdrawalAccountUser.getId(), Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[*].peerName", everyItem(comparesEqualTo(depositAccountUser.getName()))))
            .andExpect(jsonPath("$[*].amount", everyItem(comparesEqualTo(transferAmount.intValue()))))
            .andExpect(jsonPath("$[*].balancePostTransaction", Matchers.containsInAnyOrder(
                comparesEqualTo(depositAmount.subtract(transferAmount).intValue()),
                comparesEqualTo(depositAmount.subtract(transferAmount.multiply(new BigDecimal(2))).intValue()),
                comparesEqualTo(depositAmount.subtract(transferAmount.multiply(new BigDecimal(3))).intValue())
            )))
            .andDo(print());
    }

    private User createUser(String loginId, String name) {
        User user = User.builder()
            .name(name)
            .loginId(loginId)
            .password("test-password")
            .role(Role.USER)
            .build();

        return userRepository.save(user);
    }

    private Account createAccount(User user, Currency currency) {
        Account account = Account.builder()
            .user(user)
            .accountNumber(generateUniqueAccountNumber())
            .password("test-password")
            .balance(BigDecimal.ZERO)
            .currency(currency)
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        return accountRepository.save(account);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.valueOf(ThreadLocalRandom.current().nextLong(10000000000000L, 99999999999999L));
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber.substring(0, 7) + "-" + accountNumber.substring(7);
    }

    private void deposit(Account account, BigDecimal amount) {
        DepositRequest depositRequest = DepositRequest.builder()
            .accountNumber(account.getAccountNumber())
            .accountPassword(account.getPassword())
            .amount(amount)
            .build();

        accountBalanceService.atmDeposit(depositRequest);
    }
}
