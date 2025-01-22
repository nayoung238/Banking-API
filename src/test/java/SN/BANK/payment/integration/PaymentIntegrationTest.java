package SN.BANK.payment.integration;

import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.payment.entity.PaymentList;
import SN.BANK.payment.enums.PaymentStatus;
import SN.BANK.payment.repository.PaymentListRepository;
import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.account.repository.AccountRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PaymentIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UsersRepository usersRepository;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    PaymentListRepository paymentListRepository;



    Users user;
    Account withdrawAccount;
    Account depositAccount;

    @BeforeEach
    void setUp() {


        // 사용자 생성
        user = Users.builder()
                .name("테스트 사용자")
                .loginId("testUser")
                .password("password123")
                .build();
        usersRepository.save(user);

        // 출금 계좌 생성
        withdrawAccount = Account.builder()
                .user(user)
                .password("1234")
                .accountNumber("11111111111111")
                .money(BigDecimal.valueOf(10000000))
                .currency(Currency.KRW)
                .accountName("출금 계좌")
                .build();
        accountRepository.save(withdrawAccount);

        // 입금 계좌 생성
        depositAccount = Account.builder()
                .user(user)
                .password("1234")
                .accountNumber("22222222222222")
                .money(BigDecimal.valueOf(5000))
                .currency(Currency.USD)
                .accountName("입금 계좌")
                .build();
        accountRepository.save(depositAccount);

    }

    @Test
    @DisplayName("결제 성공 테스트")
    void makePayment() throws Exception {
        // Given
        PaymentRequestDto request = PaymentRequestDto.builder()
                .withdrawAccountNumber(withdrawAccount.getAccountNumber())
                .depositAccountNumber(depositAccount.getAccountNumber())
                .amount(BigDecimal.valueOf(3000))
                .password("1234")
                .build();

        // When
        mockMvc.perform(post("/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    // Convert response content to Long and verify
                    String responseContent = result.getResponse().getContentAsString();
                    Long paymentId = Long.valueOf(responseContent);
                    assertThat(paymentId).isNotNull();
                })
                .andDo(print());

        // Then
        PaymentList payment = paymentListRepository.findAll().get(0);
        assertThat(payment.getWithdrawAccountNumber()).isEqualTo(withdrawAccount.getAccountNumber());
        assertThat(payment.getDepositAccountNumber()).isEqualTo(depositAccount.getAccountNumber());
        assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    @DisplayName("결제취소 성공 테스트")
    void refundPayment() throws Exception {
        // Given
        PaymentList payment = PaymentList.builder()
                .withdrawAccountNumber(withdrawAccount.getAccountNumber())
                .depositAccountNumber(depositAccount.getAccountNumber())
                .amount(BigDecimal.valueOf(3000))
                .currency(Currency.KRW)
                .exchangeRate(BigDecimal.ONE)
                .paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
                .build();
        paymentListRepository.save(payment);

        PaymentRefundRequestDto request = new PaymentRefundRequestDto(payment.getId(), "1234");
        // When
        mockMvc.perform(post("/payment/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("결제취소 완료"))
                .andDo(print());

        // Then
        PaymentList updatedPayment = paymentListRepository.findById(payment.getId()).orElseThrow();
        assertThat(updatedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_CANCELLED);
    }

    @Test
    @DisplayName("특정 결제 내역 조회 테스트")
    void getPaymentDetail() throws Exception {
        // Given
        PaymentList payment = PaymentList.builder()
                .withdrawAccountNumber(withdrawAccount.getAccountNumber())
                .depositAccountNumber(depositAccount.getAccountNumber())
                .amount(BigDecimal.valueOf(3000))
                .currency(Currency.KRW)
                .exchangeRate(BigDecimal.ONE)
                .paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
                .paidAt(LocalDateTime.now())
                .build();
        paymentListRepository.save(payment);

        // When
        mockMvc.perform(get("/payment/history/{paymentId}", payment.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payment.getId()))
                .andExpect(jsonPath("$.withdrawAccountNumber").value(withdrawAccount.getAccountNumber()))
                .andExpect(jsonPath("$.depositAccountNumber").value(depositAccount.getAccountNumber()))
                .andExpect(jsonPath("$.amount").value(payment.getAmount().stripTrailingZeros().toPlainString()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PAYMENT_COMPLETED.name()))
                .andExpect(jsonPath("$.currency").value(Currency.KRW.name()))
                .andDo(print());
    }
}