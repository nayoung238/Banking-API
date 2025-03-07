package banking.payment.api;

import banking.account.dto.request.DepositRequest;
import banking.account.service.AccountBalanceService;
import banking.common.TransferIntegrationTestBase;
import banking.common.data.EncryptionFacade;
import banking.fixture.dto.AccountCreationRequestDtoFixture;
import banking.payment.dto.request.PaymentRefundRequest;
import banking.payment.dto.request.PaymentRequest;
import banking.payment.dto.response.PaymentResponse;
import banking.payment.enums.PaymentStatus;
import banking.payment.service.PaymentService;
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
class PaymentControllerTransferIntegrationTest extends TransferIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AccountBalanceService accountBalanceService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    EncryptionFacade encryptionFacade;

    @Test
    @DisplayName("[결제 성공 테스트] 결제 성공 시 결제 상세 내역 반환")
    void payment_succeed_test () throws Exception {
        // given1 - 송금 계좌에 입금
        final BigDecimal currentBalance = BigDecimal.valueOf(10000);
        DepositRequest depositRequest = DepositRequest.builder()
            .accountNumber(senderKrwAccount.accountNumber())
            .accountPassword(senderKrwAccountPassword)
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given2 - 계좌번호 및 비밀번호 암호화
        String encryptedDepositAccountNumber = encryptionFacade.encrypt(receiverKrwAccount.accountNumber());
        String encryptedPassword = encryptionFacade.encrypt(AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto().password());

        // given3 - 계좌 요청 DTO 생성
        final BigDecimal withdrawalBalance = BigDecimal.valueOf(10000);
        PaymentRequest request = PaymentRequest.builder()
            .withdrawalAccountId(senderKrwAccount.accountId())
            .withdrawalAccountPassword(encryptedPassword)
            .depositAccountNumber(encryptedDepositAccountNumber)
            .amount(withdrawalBalance)
            .build();

        // When & then
        mockMvc.perform(
            post("/payment")
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-Id", senderUser.userId())
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.paymentStatus").value(anyOf(
                is(PaymentStatus.PAYMENT_PENDING.toString()),
                is(PaymentStatus.PAYMENT_COMPLETED.toString()),
                is(PaymentStatus.PAYMENT_CANCELLED.toString())
            )))
            .andExpect(jsonPath("$.withdrawAccountNumber").value(senderKrwAccount.accountNumber()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(withdrawalBalance.intValue())))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(receiverKrwAccount.currency() + "/" + senderKrwAccount.currency()))
            .andDo(print());

    }

    @Test
    @DisplayName("[결제 취소 성공 테스트] 결제 취소 시 입금 계좌번호와 환불 금액 반환")
    void refund_succeed_test () {
        // given1 - 송금 계좌에 입금
        final BigDecimal currentBalance = BigDecimal.valueOf(10000);
        DepositRequest depositRequest = DepositRequest.builder()
            .accountNumber(senderKrwAccount.accountNumber())
            .accountPassword(senderKrwAccountPassword)
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given3 - 결제 요청 및 처리
        final BigDecimal paymentAmount = BigDecimal.valueOf(2000);
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .withdrawalAccountId(senderKrwAccount.accountId())
            .withdrawalAccountPassword(senderKrwAccountPassword)
            .depositAccountNumber(receiverKrwAccount.accountNumber())
            .amount(paymentAmount)
            .build();
        PaymentResponse paymentResponse = paymentService.processPayment(senderUser.userId(), paymentRequest);

        // given4 - 결제 취소 요청 DTO 생성
        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
            .paymentId(paymentResponse.paymentId())
            .withdrawalAccountPassword(encryptionFacade.encrypt(AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto().password()))
            .build();

        // When & Then
        await()
            .atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                mockMvc.perform(
                        post("/payment/cancel")
                            .content(objectMapper.writeValueAsString(refundRequest))
                            .header("X-User-Id", senderUser.userId())
                            .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.depositAccountNumber").value(senderKrwAccount.accountNumber()))
                    .andExpect(jsonPath("$.refundAmount").value(comparesEqualTo(paymentAmount.intValue())))
                    .andDo(print());
            });
    }

    @Test
    @DisplayName("[결제 조회 성공 테스트] 특정 결제 내역 조회 시 상세 내역 반환")
    void find_payment_details_test () throws Exception {
        // given1 - 송금 계좌에 입금
        final BigDecimal currentBalance = BigDecimal.valueOf(10000);
        DepositRequest depositRequest = DepositRequest.builder()
            .accountNumber(senderKrwAccount.accountNumber())
            .accountPassword(senderKrwAccountPassword)
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given2 - 결제 요청 및 처리
        final BigDecimal paymentAmount = new BigDecimal(2000);
        PaymentRequest paymentRequest = PaymentRequest.builder()
            .withdrawalAccountId(senderKrwAccount.accountId())
            .withdrawalAccountPassword(senderKrwAccountPassword)
            .depositAccountNumber(receiverKrwAccount.accountNumber())
            .amount(paymentAmount)
            .build();
        PaymentResponse paymentResponse = paymentService.processPayment(senderUser.userId(), paymentRequest);

        // When & Then
        mockMvc.perform(
            get("/payment/{paymentId}", paymentResponse.paymentId())
                .header("X-User-Id", senderUser.userId())
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.paymentStatus").value(anyOf(
                is(PaymentStatus.PAYMENT_PENDING.toString()),
                is(PaymentStatus.PAYMENT_COMPLETED.toString()),
                is(PaymentStatus.PAYMENT_CANCELLED.toString())
            )))
            .andExpect(jsonPath("$.withdrawAccountNumber").value(senderKrwAccount.accountNumber()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(paymentAmount.intValue())))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(receiverKrwAccount.currency() + "/" + senderKrwAccount.currency()))
            .andDo(print());
    }
}