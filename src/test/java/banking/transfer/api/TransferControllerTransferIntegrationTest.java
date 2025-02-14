package banking.transfer.api;

import banking.account.dto.request.DepositRequestDto;
import banking.account.service.AccountBalanceService;
import banking.common.TransferIntegrationTestBase;
import banking.transfer.dto.request.TransferDetailsRequestDto;
import banking.transfer.dto.request.TransferRequestDto;
import banking.transfer.dto.response.TransferDetailsResponseDto;
import banking.transfer.enums.TransferType;
import banking.transfer.service.TransferService;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransferControllerTransferIntegrationTest extends TransferIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AccountBalanceService accountBalanceService;

    @Autowired
    TransferService transferService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("[이체 성공 테스트] KRW-KRW 계좌 간 이체")
    void transfer_succeed_test () throws Exception {
        // given1 - 송금 계좌에 입금
        final BigDecimal currentBalance = new BigDecimal(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderKrwAccount.accountNumber())
            .accountPassword(senderKrwAccountPassword)
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given2 - 이체 요청 DTO 생성
        final BigDecimal withdrawalAmount = new BigDecimal("2000.0");
        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(senderKrwAccount.accountNumber())
            .withdrawalAccountPassword(senderKrwAccountPassword)
            .depositAccountNumber(receiverKrwAccount.accountNumber())
            .amount(withdrawalAmount)
            .build();

        // when & then
        mockMvc.perform(
            post("/transfer")
                .content(objectMapper.writeValueAsString(transferRequest))
                .header("X-User-Id", senderUser.userId())
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").isNumber())
            .andExpect(jsonPath("$.withdrawalAccountNumber").value(transferRequest.withdrawalAccountNumber()))
            .andExpect(jsonPath("$.depositAccountNumber").value(transferRequest.depositAccountNumber()))
            .andExpect(jsonPath("$.transferType").value(TransferType.WITHDRAWAL.name()))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(receiverKrwAccount.currency() + "/" + senderKrwAccount.currency()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(transferRequest.amount().intValue())))
            .andExpect(jsonPath("$.balancePostTransaction").value(comparesEqualTo(currentBalance.subtract(withdrawalAmount).intValue())))
            .andDo(print());
    }

    @Test
    @DisplayName("[이체 실패 테스트] 잔액 부족 시 400 에러 반환")
    void transfer_insufficient_balance_test () throws Exception {
        // given1 - 송금 계좌에 입금
        final BigDecimal currentBalance = new BigDecimal(1000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderKrwAccount.accountNumber())
            .accountPassword(senderKrwAccountPassword)
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given2 - 이체 요청 DTO 생성
        final BigDecimal withdrawalAmount = currentBalance.multiply(new BigDecimal(2));
        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(senderKrwAccount.accountNumber())
            .withdrawalAccountPassword(senderKrwAccountPassword)
            .depositAccountNumber(receiverKrwAccount.accountNumber())
            .amount(withdrawalAmount)
            .build();

        // when & then
        mockMvc.perform(
            post("/transfer")
                .content(objectMapper.writeValueAsString(transferRequest))
                .header("X-User-Id", senderUser.userId())
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("잔액이 부족합니다.")))
            .andDo(print());
    }

    @Test
    @DisplayName("[조회 성공 테스트] 이체 내역 단건 조회")
    void findTransaction() throws Exception {
        // given1 - 송금 계좌에 입금
        final BigDecimal currentBalance = new BigDecimal(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderKrwAccount.accountNumber())
            .accountPassword(senderKrwAccountPassword)
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given2 - 이체 요청
        final BigDecimal withdrawalAmount = new BigDecimal(2000);
        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(senderKrwAccount.accountNumber())
            .withdrawalAccountPassword(senderKrwAccountPassword)
            .depositAccountNumber(receiverKrwAccount.accountNumber())
            .amount(withdrawalAmount)
            .build();

        TransferDetailsResponseDto transferDetailsResponse = transferService.transfer(senderUser.userId(), transferRequest);

        TransferDetailsRequestDto transferDetailsRequest = TransferDetailsRequestDto.builder()
            .accountId(senderKrwAccount.accountId())
            .transferId(transferDetailsResponse.transferId())
            .build();

        // when & then
        mockMvc.perform(
            get("/transfer")
                .content(objectMapper.writeValueAsString(transferDetailsRequest))
                .header("X-User-Id", senderUser.userId())
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").value(transferDetailsResponse.transferId()))
            .andExpect(jsonPath("$.withdrawalAccountNumber").value(transferRequest.withdrawalAccountNumber()))
            .andExpect(jsonPath("$.depositAccountNumber").value(transferRequest.depositAccountNumber()))
            .andExpect(jsonPath("$.transferType").value(TransferType.WITHDRAWAL.name()))
            .andExpect(jsonPath("$.exchangeRate").value(comparesEqualTo(BigDecimal.ONE.intValue())))
            .andExpect(jsonPath("$.currency").value(receiverKrwAccount.currency() + "/" + senderKrwAccount.currency()))
            .andExpect(jsonPath("$.amount").value(comparesEqualTo(transferRequest.amount().intValue())))
            .andExpect(jsonPath("$.balancePostTransaction").value(comparesEqualTo(currentBalance.subtract(withdrawalAmount).intValue())))
            .andDo(print());
    }

    @Test
    @DisplayName("[조회 성공 테스트] 모든 이체 내역 조회")
    void findAllTransaction() throws Exception {
        // given1 - 송금 계좌에 입금
        final BigDecimal currentBalance = new BigDecimal(10000);
        DepositRequestDto depositRequest = DepositRequestDto.builder()
            .accountNumber(senderKrwAccount.accountNumber())
            .accountPassword(senderKrwAccountPassword)
            .amount(currentBalance)
            .build();
        accountBalanceService.atmDeposit(depositRequest);

        // given2 - 이체 요청
        final BigDecimal withdrawalAmount = new BigDecimal(2000);
        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(senderKrwAccount.accountNumber())
            .withdrawalAccountPassword(senderKrwAccountPassword)
            .depositAccountNumber(receiverKrwAccount.accountNumber())
            .amount(withdrawalAmount)
            .build();

        transferService.transfer(senderUser.userId(), transferRequest);
        transferService.transfer(senderUser.userId(), transferRequest);
        transferService.transfer(senderUser.userId(), transferRequest);

        mockMvc.perform(
            get("/transfer/history/{accountId}", senderKrwAccount.accountId())
                .header("X-User-Id", senderUser.userId())
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[*].accountNumber", everyItem(comparesEqualTo(senderKrwAccount.accountNumber()))))
            .andExpect(jsonPath("$[*].amount", everyItem(comparesEqualTo(withdrawalAmount.intValue()))))
            .andExpect(jsonPath("$[*].balancePostTransaction", Matchers.containsInAnyOrder(
                comparesEqualTo(currentBalance.subtract(withdrawalAmount).intValue()),
                comparesEqualTo(currentBalance.subtract(withdrawalAmount.multiply(new BigDecimal(2))).intValue()),
                comparesEqualTo(currentBalance.subtract(withdrawalAmount.multiply(new BigDecimal(3))).intValue())
            )))
            .andDo(print());
    }
}
