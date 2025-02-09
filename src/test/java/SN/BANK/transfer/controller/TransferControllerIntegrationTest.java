package SN.BANK.transfer.controller;

import SN.BANK.account.dto.request.AccountCreationRequestDto;
import SN.BANK.account.dto.request.DepositRequestDto;
import SN.BANK.account.dto.response.AccountResponseDto;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.service.AccountBalanceService;
import SN.BANK.account.service.AccountService;
import SN.BANK.fixture.dto.AccountCreationRequestDtoFixture;
import SN.BANK.fixture.dto.UserCreationRequestDtoFixture;
import SN.BANK.transfer.dto.request.TransferDetailsRequestDto;
import SN.BANK.transfer.dto.request.TransferRequestDto;
import SN.BANK.transfer.dto.response.TransferDetailsResponseDto;
import SN.BANK.transfer.enums.TransferType;
import SN.BANK.transfer.repository.TransferRepository;
import SN.BANK.transfer.service.TransferService;
import SN.BANK.users.dto.UserCreationRequestDto;
import SN.BANK.users.dto.UserResponseDto;
import SN.BANK.users.repository.UsersRepository;
import SN.BANK.users.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

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
    TransferService transferService;

    @Autowired
    TransferRepository transferRepository;

    @Autowired
    ObjectMapper objectMapper;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
        userRepository.deleteAll();
        transferRepository.deleteAll();
    }

    @Test
    @DisplayName("[이체 성공 테스트] KRW-KRW 계좌 간 이체")
    void transfer_succeed_test () throws Exception {
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
        final BigDecimal currentBalance = new BigDecimal(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderAccountResponse.accountNumber())
            .accountPassword(senderAccountRequest.password())
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given3 - 이체 요청 DTO 생성
        final BigDecimal withdrawalAmount = new BigDecimal("2000.0");
        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(senderAccountResponse.accountNumber())
            .withdrawalAccountPassword(senderAccountRequest.password())
            .depositAccountNumber(receiverAccountResponse.accountNumber())
            .amount(withdrawalAmount)
            .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", senderUserResponse.userId());

        // when & then
        mockMvc.perform(
            post("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
                .content(objectMapper.writeValueAsString(transferRequest))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").isNumber())
            .andExpect(jsonPath("$.withdrawalAccountNumber").value(transferRequest.withdrawalAccountNumber()))
            .andExpect(jsonPath("$.depositAccountNumber").value(transferRequest.depositAccountNumber()))
            .andExpect(jsonPath("$.transferType").value(TransferType.WITHDRAWAL.name()))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(receiverAccountResponse.currency() + "/" + senderAccountResponse.currency()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(transferRequest.amount().intValue())))
            .andExpect(jsonPath("$.balancePostTransaction").value(comparesEqualTo(currentBalance.subtract(withdrawalAmount).intValue())))
            .andDo(print());

        session.invalidate();
    }

    @Test
    @DisplayName("[이체 실패 테스트] 잔액 부족 시 400 에러 반환")
    void transfer_insufficient_balance_test () throws Exception {
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
        final BigDecimal currentBalance = new BigDecimal(1000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderAccountResponse.accountNumber())
            .accountPassword(senderAccountRequest.password())
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given3 - 이체 요청 DTO 생성
        final BigDecimal withdrawalAmount = currentBalance.multiply(new BigDecimal(2));
        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(senderAccountResponse.accountNumber())
            .withdrawalAccountPassword(senderAccountRequest.password())
            .depositAccountNumber(receiverAccountResponse.accountNumber())
            .amount(withdrawalAmount)
            .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", senderUserResponse.userId());

        // when & then
        mockMvc.perform(
            post("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
                .content(objectMapper.writeValueAsString(transferRequest))
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("잔액이 부족합니다.")))
            .andDo(print());

        session.invalidate();
    }

    @Test
    @DisplayName("[조회 성공 테스트] 이체 내역 단건 조회")
    void findTransaction() throws Exception {
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
        final BigDecimal currentBalance = new BigDecimal(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderAccountResponse.accountNumber())
            .accountPassword(senderAccountRequest.password())
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given3 - 이체 요청
        final BigDecimal withdrawalAmount = new BigDecimal(2000);
        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(senderAccountResponse.accountNumber())
            .withdrawalAccountPassword(senderAccountRequest.password())
            .depositAccountNumber(receiverAccountResponse.accountNumber())
            .amount(withdrawalAmount)
            .build();

        TransferDetailsResponseDto transferDetailsResponse = transferService.transfer(senderUserResponse.userId(), transferRequest);

        TransferDetailsRequestDto transferDetailsRequest = TransferDetailsRequestDto.builder()
            .accountId(senderAccountResponse.accountId())
            .transferId(transferDetailsResponse.transferId())
            .build();

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", senderUserResponse.userId());

        // when & then
        mockMvc.perform(
            get("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
                .content(objectMapper.writeValueAsString(transferDetailsRequest))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").value(transferDetailsResponse.transferId()))
            .andExpect(jsonPath("$.withdrawalAccountNumber").value(transferRequest.withdrawalAccountNumber()))
            .andExpect(jsonPath("$.depositAccountNumber").value(transferRequest.depositAccountNumber()))
            .andExpect(jsonPath("$.transferType").value(TransferType.WITHDRAWAL.name()))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(receiverAccountResponse.currency() + "/" + senderAccountResponse.currency()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(transferRequest.amount().intValue())))
            .andExpect(jsonPath("$.balancePostTransaction").value(comparesEqualTo(currentBalance.subtract(withdrawalAmount).intValue())))
            .andDo(print());

        session.invalidate();
    }

    @Test
    @DisplayName("[조회 성공 테스트] 모든 이체 내역 조회")
    void findAllTransaction() throws Exception {
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
        final BigDecimal currentBalance = new BigDecimal(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderAccountResponse.accountNumber())
            .accountPassword(senderAccountRequest.password())
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given3 - 이체 요청
        final BigDecimal withdrawalAmount = new BigDecimal(2000);
        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(senderAccountResponse.accountNumber())
            .withdrawalAccountPassword(senderAccountRequest.password())
            .depositAccountNumber(receiverAccountResponse.accountNumber())
            .amount(withdrawalAmount)
            .build();

        transferService.transfer(senderUserResponse.userId(), transferRequest);
        transferService.transfer(senderUserResponse.userId(), transferRequest);
        transferService.transfer(senderUserResponse.userId(), transferRequest);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", senderUserResponse.userId());

        mockMvc.perform(
            get("/transfer/history/{accountId}", senderAccountResponse.accountId())
                .contentType(MediaType.APPLICATION_JSON)
                .session(session)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[*].accountNumber", everyItem(comparesEqualTo(senderAccountResponse.accountNumber()))))
            .andExpect(jsonPath("$[*].amount", everyItem(comparesEqualTo(withdrawalAmount.intValue()))))
            .andExpect(jsonPath("$[*].balancePostTransaction", Matchers.containsInAnyOrder(
                comparesEqualTo(currentBalance.subtract(withdrawalAmount).intValue()),
                comparesEqualTo(currentBalance.subtract(withdrawalAmount.multiply(new BigDecimal(2))).intValue()),
                comparesEqualTo(currentBalance.subtract(withdrawalAmount.multiply(new BigDecimal(3))).intValue())
            )))
            .andDo(print());
    }
}
