package banking.payment.api;

import banking.account.dto.request.DepositRequest;
import banking.account.entity.Account;
import banking.account.enums.AccountStatus;
import banking.account.enums.Currency;
import banking.account.repository.AccountRepository;
import banking.account.service.AccountBalanceService;
import banking.common.data.EncryptionFacade;
import banking.common.jwt.TestJwtUtil;
import banking.payment.dto.request.PaymentRefundRequest;
import banking.payment.dto.request.PaymentRequest;
import banking.payment.dto.response.PaymentView;
import banking.payment.enums.PaymentStatus;
import banking.payment.service.PaymentService;
import banking.user.entity.Role;
import banking.user.entity.User;
import banking.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AccountBalanceService accountBalanceService;

    @Autowired
    PaymentService paymentService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	AccountRepository accountRepository;

    @Test
    @DisplayName("[결제 성공 테스트] 결제 성공 시 결제 상세 내역 반환")
    void payment_succeed_test () throws Exception {
        // given1 - 출금 계좌에 입금
        User withdrawalAccountUser = createUser("login-id-123", "user-1");
        Account withdrawalAccount = createAccount(withdrawalAccountUser, Currency.KRW);

        final BigDecimal depositAmount = BigDecimal.valueOf(10000);
        deposit(withdrawalAccount, depositAmount);

        // given2 - 입금 계좌 생성
        User depositAccountUser = createUser("login-id-321", "user-2");
        Account depositAccount = createAccount(depositAccountUser, Currency.KRW);

        // @Decrypt 주석 처리함
//        String encryptedDepositAccountNumber = encryptionFacade.encrypt(depositAccount.getAccountNumber());
//        String encryptedPassword = encryptionFacade.encrypt(withdrawalAccount.getPassword());

        // given3 - 계좌 요청 DTO 생성
        final BigDecimal paymentAmount = BigDecimal.valueOf(3000);
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .withdrawalAccountId(withdrawalAccount.getId())
            .withdrawalAccountPassword(withdrawalAccount.getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(paymentAmount)
            .build();

        // When & then
        mockMvc.perform(
            post("/payment")
                .content(objectMapper.writeValueAsString(paymentRequest))
                .header("Authorization", "Bearer " + TestJwtUtil.generateTestAccessToken(withdrawalAccountUser.getId(), Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.paymentStatus").value(anyOf(
                is(PaymentStatus.PAYMENT_PENDING.toString()),
                is(PaymentStatus.PAYMENT_COMPLETED.toString()),
                is(PaymentStatus.PAYMENT_CANCELLED.toString())
            )))
            .andExpect(jsonPath("$.withdrawalAccountNumber").value(withdrawalAccount.getAccountNumber()))
            .andExpect(jsonPath("$.payeeName").value(depositAccountUser.getName()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(paymentAmount.doubleValue())))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.doubleValue())))
            .andExpect(jsonPath("$.currency").value(depositAccount.getCurrency() + "/" + withdrawalAccount.getCurrency()))
            .andDo(print());
    }

    @Test
    @DisplayName("[결제 취소 성공 테스트] 결제 취소 시 입금 계좌번호와 환불 금액 반환")
    void refund_succeed_test () {
        // given1 - 출금 계좌에 입금
        User withdrawalAccountUser = createUser("login-id-456", "user-1");
        Account withdrawalAccount = createAccount(withdrawalAccountUser, Currency.KRW);

        final BigDecimal depositAmount = BigDecimal.valueOf(10000);
        deposit(withdrawalAccount, depositAmount);

        // given2 - 입금 계좌 생성
        User depositAccountUser = createUser("login-id-654", "user-2");
        Account depositAccount = createAccount(depositAccountUser, Currency.KRW);

        // given3 - 결제 요청 및 처리
        final BigDecimal paymentAmount = BigDecimal.valueOf(3000);
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .withdrawalAccountId(withdrawalAccount.getId())
            .withdrawalAccountPassword(withdrawalAccount.getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(paymentAmount)
            .build();
        PaymentView paymentView = paymentService.processPayment(withdrawalAccountUser.getId(), paymentRequest);

        // given4 - 결제 취소 요청 DTO 생성
        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
            .paymentId(paymentView.paymentId())
            .withdrawalAccountPassword(withdrawalAccount.getPassword())
            .build();

        // When & Then
        await()
            .atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                mockMvc.perform(
                        post("/payment/cancel")
                            .content(objectMapper.writeValueAsString(refundRequest))
                            .header("Authorization", "Bearer " + TestJwtUtil.generateTestAccessToken(withdrawalAccountUser.getId(), Role.USER))
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.depositAccountNumber").value(withdrawalAccount.getAccountNumber()))
                    .andExpect(jsonPath("$.refundAmount").value(comparesEqualTo(paymentAmount.intValue())))
                    .andDo(print());
            });
    }

    @Test
    @DisplayName("[결제 조회 성공 테스트] 특정 결제 내역 조회 시 상세 내역 반환")
    void find_payment_details_test () throws Exception {
        // given1 - 출금 계좌에 입금
        User withdrawalAccountUser = createUser("login-id-789", "user-1");
        Account withdrawalAccount = createAccount(withdrawalAccountUser, Currency.KRW);

        final BigDecimal depositAmount = BigDecimal.valueOf(10000);
        deposit(withdrawalAccount, depositAmount);

        // given2 - 입금 계좌 생성
        User depositAccountUser = createUser("login-id-987", "user-2");
        Account depositAccount = createAccount(depositAccountUser, Currency.KRW);

        // given3 - 결제 요청 및 처리
        final BigDecimal paymentAmount = BigDecimal.valueOf(3000);
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .withdrawalAccountId(withdrawalAccount.getId())
            .withdrawalAccountPassword(withdrawalAccount.getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(paymentAmount)
            .build();
        PaymentView paymentView = paymentService.processPayment(withdrawalAccountUser.getId(), paymentRequest);

        // When & Then
        mockMvc.perform(
            get("/payment/{paymentId}", paymentView.paymentId())
                .header("Authorization", "Bearer " + TestJwtUtil.generateTestAccessToken(withdrawalAccountUser.getId(), Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.paymentStatus").value(anyOf(
                is(PaymentStatus.PAYMENT_PENDING.toString()),
                is(PaymentStatus.PAYMENT_COMPLETED.toString()),
                is(PaymentStatus.PAYMENT_CANCELLED.toString())
            )))
            .andExpect(jsonPath("$.withdrawalAccountNumber").value(withdrawalAccount.getAccountNumber()))
            .andExpect(jsonPath("$.payeeName").value(depositAccountUser.getName()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(paymentAmount.doubleValue())))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.doubleValue())))
            .andExpect(jsonPath("$.currency").value(depositAccount.getCurrency() + "/" + withdrawalAccount.getCurrency()))
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