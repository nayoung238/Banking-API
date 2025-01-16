package SN.BANK.transfer.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.payment.entity.PaymentList;
import SN.BANK.account.enums.Currency;
import SN.BANK.payment.enums.PaymentTag;
import SN.BANK.payment.repository.PaymentListRepository;
import SN.BANK.transfer.dto.request.TransferRequest;
import SN.BANK.transfer.dto.response.PaymentListResponse;
import SN.BANK.users.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class TransferServiceTest {

    @Mock
    AccountService accountService;

    @Mock
    PaymentListRepository paymentListRepository;

    @InjectMocks
    TransferService transferService;

    Users user1;
    Users user2;
    Account account1;
    Account account2;

    @BeforeEach
    void setUp() {
        user1 = Users.builder()
                .name("테스트이름")
                .loginId("test1234")
                .password("test1234")
                .build();

        user2 = Users.builder()
                .name("테스터")
                .loginId("test4321")
                .password("test4321")
                .build();

        account1 = Account.builder()
                .id(1L)
                .user(user1)
                .password("1234")
                .accountNumber("11111111111111")
                .money(BigDecimal.valueOf(10000))
                .accountName("test account1")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();

        account2 = Account.builder()
                .id(2L)
                .user(user2)
                .password("1234")
                .accountNumber("22222222222222")
                .money(BigDecimal.valueOf(10000))
                .accountName("test account2")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();
    }

    @Test
    @DisplayName("사용자는 이체할 수 있다.")
    void transfer() {

        // given
        TransferRequest transferRequest = TransferRequest.builder()
                .fromAccountId(1L)
                .accountPassword("1234")
                .toAccountId(2L)
                .amount(BigDecimal.valueOf(5000))
                .currency(Currency.KRW)
                .build();

        when(accountService.findValidAccount(1L)).thenReturn(account1);
        when(accountService.findValidAccount(2L)).thenReturn(account2);
        when(paymentListRepository.save(any(PaymentList.class))).thenAnswer(invocationOnMock ->
                invocationOnMock.getArgument(0));

        // when
        PaymentListResponse response = transferService.transfer(transferRequest);

        // then
        assertNotNull(response);
        assertEquals(PaymentTag.이체, response.getPaymentTag());
        assertEquals(1L, response.getDepositId());
        assertEquals(2L, response.getWithdrawId());
        assertEquals(BigDecimal.valueOf(5000), response.getBalance());
    }

    @Test
    @DisplayName("계좌 비밀번호가 틀리면 이체할 수 없다.")
    void transfer_INALID_PASSWOD() {
        // given
        TransferRequest transferRequest = TransferRequest.builder()
                .fromAccountId(1L)
                .accountPassword("0000") // 다른 비밀번호
                .toAccountId(2L)
                .amount(BigDecimal.valueOf(5000))
                .currency(Currency.KRW)
                .build();

        when(accountService.findValidAccount(1L)).thenReturn(account1);
        when(accountService.findValidAccount(2L)).thenReturn(account2);
        when(paymentListRepository.save(any(PaymentList.class))).thenAnswer(invocationOnMock ->
                invocationOnMock.getArgument(0));

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transferService.transfer(transferRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INVALID_PASSWORD, customException.getErrorCode());
    }

    @Test
    @DisplayName("계좌의 돈이 부족하면 이체할 수 없다.")
    void transfer_INSUFFICIENT_MONEY() {
        // given
        TransferRequest transferRequest = TransferRequest.builder()
                .fromAccountId(1L)
                .accountPassword("1234")
                .toAccountId(2L)
                .amount(BigDecimal.valueOf(10001)) // 이체액을 계좌 잔액보다 크게 잡음
                .currency(Currency.KRW)
                .build();

        when(accountService.findValidAccount(1L)).thenReturn(account1);
        when(accountService.findValidAccount(2L)).thenReturn(account2);
        when(paymentListRepository.save(any(PaymentList.class))).thenAnswer(invocationOnMock ->
                invocationOnMock.getArgument(0));

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transferService.transfer(transferRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INSUFFICIENT_MONEY, customException.getErrorCode());
    }

    @Test
    @DisplayName("송신 계좌와 수취 계좌가 같으면 이체할 수 없다.")
    void transfer_INVALID_TRANSFER() {
        // given
        TransferRequest transferRequest = TransferRequest.builder()
                .fromAccountId(2L)
                .accountPassword("1234")
                .toAccountId(2L)
                .amount(BigDecimal.valueOf(5000))
                .currency(Currency.KRW)
                .build();

        when(accountService.findValidAccount(2L)).thenReturn(account2);
        when(paymentListRepository.save(any(PaymentList.class))).thenAnswer(invocationOnMock ->
                invocationOnMock.getArgument(0));

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transferService.transfer(transferRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INVALID_TRANSFER, customException.getErrorCode());
    }

    @Test
    @DisplayName("입금액은 계좌 잔액보다 많을 수 없다.")
    void isGreaterThanAmount() {
        assertTrue(transferService.isGreaterThanAmount(account1, BigDecimal.valueOf(10000)));
        assertFalse(transferService.isGreaterThanAmount(account1, BigDecimal.valueOf(10001)));
    }
}