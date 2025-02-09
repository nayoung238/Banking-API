package SN.BANK.payment.controller;

import SN.BANK.account.dto.request.AccountCreationRequestDto;
import SN.BANK.account.dto.request.DepositRequestDto;
import SN.BANK.account.dto.response.AccountResponseDto;
import SN.BANK.account.service.AccountBalanceService;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.data.EncryptionFacade;
import SN.BANK.fixture.dto.AccountCreationRequestDtoFixture;
import SN.BANK.fixture.dto.UserCreationRequestDtoFixture;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.payment.dto.response.PaymentResponseDto;
import SN.BANK.payment.enums.PaymentStatus;
import SN.BANK.payment.repository.PaymentRepository;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.payment.service.PaymentService;
import SN.BANK.transfer.repository.TransferRepository;
import SN.BANK.users.dto.UserCreationRequestDto;
import SN.BANK.users.dto.UserResponseDto;
import SN.BANK.users.repository.UsersRepository;
import SN.BANK.users.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UsersService userService;

    @Autowired
    UsersRepository userRepository;

    @Autowired
    AccountService accountService;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AccountBalanceService accountBalanceService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    EncryptionFacade encryptionFacade;

	@Autowired
	TransferRepository transferRepository;

    @AfterEach
    void afterEach() {
        transferRepository.deleteAll();
        paymentRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("[결제 성공 테스트] 결제 성공 시 결제 상세 내역 반환")
    void payment_succeed_test () throws Exception {
        // given1 - 유저 및 계좌 생성
        UserCreationRequestDto senderCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_1.createUserCreationRequestDto();
        UserResponseDto senderUserResponse = userService.register(senderCreationRequest);

        UserCreationRequestDto receiverCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_2.createUserCreationRequestDto();
        UserResponseDto receiverUserResponse = userService.register(receiverCreationRequest);

        AccountCreationRequestDto senderAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto();
        AccountResponseDto senderAccountResponse = accountService.createAccount(senderUserResponse.userId(), senderAccountRequest);

        AccountCreationRequestDto receiverAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_2.createAccountCreationRequestDto();
        AccountResponseDto receiverAccountResponse = accountService.createAccount(receiverUserResponse.userId(), receiverAccountRequest);

        // given2 - 송금 계좌에 입금
        final BigDecimal currentBalance = BigDecimal.valueOf(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderAccountResponse.accountNumber())
            .accountPassword(senderAccountRequest.password())
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given3 - 계좌번호 및 비밀번호 암호화
        String encryptedWithdrawAccountNumber = encryptionFacade.encrypt(senderAccountResponse.accountNumber());
        String encryptedDepositAccountNumber = encryptionFacade.encrypt(receiverAccountResponse.accountNumber());
        String encryptedPassword = encryptionFacade.encrypt(AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto().password());

        // given4 - 계좌 요청 DTO 생성
        final BigDecimal withdrawalBalance = BigDecimal.valueOf(10000);
        PaymentRequestDto request = PaymentRequestDto.builder()
            .withdrawalAccountNumber(encryptedWithdrawAccountNumber)
            .withdrawalAccountPassword(encryptedPassword)
            .depositAccountNumber(encryptedDepositAccountNumber)
            .amount(withdrawalBalance)
            .build();

        // When & then
        mockMvc.perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.paymentStatus").value(anyOf(
                is(PaymentStatus.PAYMENT_PENDING.toString()),
                is(PaymentStatus.PAYMENT_COMPLETED.toString()),
                is(PaymentStatus.PAYMENT_CANCELLED.toString())
            )))
            .andExpect(jsonPath("$.withdrawAccountNumber").value(senderAccountResponse.accountNumber()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(withdrawalBalance.intValue())))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(receiverAccountResponse.currency() + "/" + senderAccountResponse.currency()))
            .andDo(print());

    }

    @Test
    @DisplayName("[결제 취소 성공 테스트] 결제 취소 시 입금 계좌번호와 환불 금액 반환")
    void refund_succeed_test () throws Exception {
        // given1 - 유저 및 계좌 생성
        UserCreationRequestDto senderCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_1.createUserCreationRequestDto();
        UserResponseDto senderUserResponse = userService.register(senderCreationRequest);

        UserCreationRequestDto receiverCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_2.createUserCreationRequestDto();
        UserResponseDto receiverUserResponse = userService.register(receiverCreationRequest);

        AccountCreationRequestDto senderAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto();
        AccountResponseDto senderAccountResponse = accountService.createAccount(senderUserResponse.userId(), senderAccountRequest);

        AccountCreationRequestDto receiverAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_2.createAccountCreationRequestDto();
        AccountResponseDto receiverAccountResponse = accountService.createAccount(receiverUserResponse.userId(), receiverAccountRequest);

        // given2 - 송금 계좌에 입금
        final BigDecimal currentBalance = BigDecimal.valueOf(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderAccountResponse.accountNumber())
            .accountPassword(senderAccountRequest.password())
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given3 - 결제 요청 및 처리
        final BigDecimal paymentAmount = BigDecimal.valueOf(2000);
        PaymentRequestDto paymentRequest = PaymentRequestDto.builder()
            .withdrawalAccountNumber(senderAccountResponse.accountNumber())
            .withdrawalAccountPassword(senderAccountRequest.password())
            .depositAccountNumber(receiverAccountResponse.accountNumber())
            .amount(paymentAmount)
            .build();
        PaymentResponseDto paymentResponse = paymentService.processPayment(paymentRequest);

        // given4 - 결제 취소 요청 DTO 생성
        PaymentRefundRequestDto refundRequest = PaymentRefundRequestDto.builder()
            .paymentId(paymentResponse.paymentId())
            .withdrawalAccountPassword(encryptionFacade.encrypt(AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto().password()))
            .build();

        // When & Then
        mockMvc.perform(
            post("/payment/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refundRequest))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.depositAccountNumber").value(senderAccountResponse.accountNumber()))
            .andExpect(jsonPath("$.refundAmount").value(comparesEqualTo(paymentAmount.intValue())))
            .andDo(print());
    }

    @Test
    @DisplayName("[결제 조회 성공 테스트] 특정 결제 내역 조회 시 상세 내역 반환")
    void find_payment_details_test () throws Exception {
        // given1 - 유저 및 계좌 생성
        UserCreationRequestDto senderCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_1.createUserCreationRequestDto();
        UserResponseDto senderUserResponse = userService.register(senderCreationRequest);

        UserCreationRequestDto receiverCreationRequest = UserCreationRequestDtoFixture.USER_CREATION_REQUEST_DTO_FIXTURE_2.createUserCreationRequestDto();
        UserResponseDto receiverUserResponse = userService.register(receiverCreationRequest);

        AccountCreationRequestDto senderAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_1.createAccountCreationRequestDto();
        AccountResponseDto senderAccountResponse = accountService.createAccount(senderUserResponse.userId(), senderAccountRequest);

        AccountCreationRequestDto receiverAccountRequest = AccountCreationRequestDtoFixture.ACCOUNT_FIXTURE_KRW_2.createAccountCreationRequestDto();
        AccountResponseDto receiverAccountResponse = accountService.createAccount(receiverUserResponse.userId(), receiverAccountRequest);

        // given2 - 송금 계좌에 입금
        final BigDecimal currentBalance = BigDecimal.valueOf(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderAccountResponse.accountNumber())
            .accountPassword(senderAccountRequest.password())
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given3 - 결제 요청 및 처리
        final BigDecimal paymentAmount = new BigDecimal(2000);
        PaymentRequestDto paymentRequest = PaymentRequestDto.builder()
            .withdrawalAccountNumber(senderAccountResponse.accountNumber())
            .withdrawalAccountPassword(senderAccountRequest.password())
            .depositAccountNumber(receiverAccountResponse.accountNumber())
            .amount(paymentAmount)
            .build();
        PaymentResponseDto paymentResponse = paymentService.processPayment(paymentRequest);

        // When & Then
        mockMvc.perform(
            get("/payment/{paymentId}", paymentResponse.paymentId())
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").isNumber())
            .andExpect(jsonPath("$.paymentStatus").value(anyOf(
                is(PaymentStatus.PAYMENT_PENDING.toString()),
                is(PaymentStatus.PAYMENT_COMPLETED.toString()),
                is(PaymentStatus.PAYMENT_CANCELLED.toString())
            )))
            .andExpect(jsonPath("$.withdrawAccountNumber").value(senderAccountResponse.accountNumber()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(paymentAmount.intValue())))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(receiverAccountResponse.currency() + "/" + senderAccountResponse.currency()))
            .andDo(print());
    }
}