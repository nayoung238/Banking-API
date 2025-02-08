package SN.BANK.transfer.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.exchangeRate.ExchangeRateService;
import SN.BANK.transfer.dto.request.TransferRequest;
import SN.BANK.transfer.dto.response.TransferResponse;
import SN.BANK.transfer.enums.TransferType;
import SN.BANK.transfer.repository.TransferRepository;
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
import static org.mockito.Mockito.*;


@ExtendWith(SpringExtension.class)
class TransferServiceTest {

    @Mock
    TransferRepository transferRepository;

    @Mock
    AccountService accountService;

    @Mock
    ExchangeRateService exchangeRateService;

    @InjectMocks
    TransferService transferService;

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
                .id(1L)
                .user(sender)
                .password("1234")
                .accountNumber("11111111111111")
                .money(BigDecimal.valueOf(10000.00))
                .accountName("test account1")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();

        receiverAccount = Account.builder()
                .id(2L)
                .user(receiver)
                .password("1234")
                .accountNumber("22222222222222")
                .money(BigDecimal.valueOf(10000.00))
                .accountName("test account2")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();
    }

    @Test
    @DisplayName("사용자는 이체할 수 있다.")
    void createTransaction() {

        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(5000.00);
        BigDecimal expectedSenderBalance = senderAccount.getMoney().subtract(amount);
        BigDecimal expectedReceiverBalance = receiverAccount.getMoney().add(amount);

        TransferRequest transferRequest = TransferRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();

        when(accountService.getAccountWithLock(1L)).thenReturn(senderAccount);
        when(accountService.getAccountWithLock(2L)).thenReturn(receiverAccount);
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(BigDecimal.ONE);

        // when
        TransferResponse response = transferService.createTransfer(userId, transferRequest);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getSenderAccountId());
        assertEquals(2L, response.getReceiverAccountId());
        assertEquals(TransferType.WITHDRAWAL, response.getTransferType());
        assertEquals(response.getSenderName(), "테스터1");
        assertEquals(response.getReceiverName(), "테스터2");
        assertEquals(expectedSenderBalance, response.getBalance());
        assertEquals(expectedReceiverBalance, receiverAccount.getMoney());

        verify(accountService, times(1)).getAccountWithLock(senderAccount.getId());
        verify(accountService, times(1)).getAccountWithLock(receiverAccount.getId());
        verify(accountService, times(1)).validateAccountBalance(senderAccount, amount);
        verify(accountService, times(1)).validateAccountPassword(senderAccount, "1234");
    }

    @Test
    @DisplayName("본인 계좌가 아닌 경우 에러 발생")
    void createTransaction_UNAUTHORIZED_ACCOUNT_ACCESS() {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(5000.00);

        TransferRequest transferRequest = TransferRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();

        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(BigDecimal.ONE);
        doThrow(new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS))
                .when(accountService).getAccountWithLock(any());

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transferService.createTransfer(userId, transferRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS, customException.getErrorCode());
        verify(accountService, times(1)).getAccountWithLock(any());
    }

    @Test
    @DisplayName("비밀번호 불일치로 인한 거래 실패 테스트")
    void createTransaction_INALID_PASSWOD() {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(5000.00);
        String incorrectPassword = "0000"; // 다른 비밀번호

        TransferRequest transferRequest = TransferRequest.builder()
                .accountPassword(incorrectPassword) // 다른 비밀번호
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();

        when(accountService.getAccountWithLock(1L)).thenReturn(senderAccount);
        when(accountService.getAccountWithLock(2L)).thenReturn(receiverAccount);
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(BigDecimal.ONE);
        doThrow(new CustomException(ErrorCode.INVALID_PASSWORD))
                .when(accountService).validateAccountPassword(senderAccount, incorrectPassword);

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transferService.createTransfer(userId, transferRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INVALID_PASSWORD, customException.getErrorCode());
        verify(accountService, times(1)).validateAccountPassword(senderAccount, incorrectPassword);
    }

    @Test
    @DisplayName("잔액 부족으로 인한 거래 실패 테스트")
    void createTransaction_INSUFFICIENT_BALANCE() {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(50000.00);

        TransferRequest transferRequest = TransferRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();

        when(accountService.getAccountWithLock(1L)).thenReturn(senderAccount);
        when(accountService.getAccountWithLock(2L)).thenReturn(receiverAccount);
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(BigDecimal.ONE);
        doThrow(new CustomException(ErrorCode.INSUFFICIENT_BALANCE))
                .when(accountService).validateAccountBalance(senderAccount, amount);

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transferService.createTransfer(userId, transferRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, customException.getErrorCode());
        verify(accountService, times(1)).validateAccountBalance(senderAccount, amount);
    }

    @Test
    @DisplayName("송신 계좌와 수신 계좌가 동일한 경우 거래 실패 테스트")
    void createTransaction_INVALID_TRANSFER() {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(5000.00);

        TransferRequest transferRequest = TransferRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(senderAccount.getId())
                .amount(amount)
                .build();

        when(accountService.getAccountWithLock(1L)).thenReturn(senderAccount);
        when(accountService.getAccountWithLock(2L)).thenReturn(receiverAccount);
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(BigDecimal.ONE);
        doThrow(new CustomException(ErrorCode.INVALID_TRANSFER))
                .when(accountService).validateNotSelfTransfer(any(), any());

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transferService.createTransfer(userId, transferRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INVALID_TRANSFER, customException.getErrorCode());
        verify(accountService, times(1)).validateNotSelfTransfer(any(), any());
    }

    @Test
    @DisplayName("환율 오류 예외 테스트")
    void createTransactionInvalidExchangeRate() {
        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(5000.00);

        TransferRequest request = TransferRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();

        when(accountService.getAccountWithLock(senderAccount.getId())).thenReturn(senderAccount);
        when(accountService.getAccountWithLock(receiverAccount.getId())).thenReturn(receiverAccount);
        when(exchangeRateService.getExchangeRate(senderAccount.getCurrency(), receiverAccount.getCurrency()))
                .thenReturn(BigDecimal.ZERO); // 잘못된 환율

        // when
        CustomException exception =
                assertThrows(CustomException.class, () -> transferService.createTransfer(userId, request));

        // then
        assertEquals(ErrorCode.INVALID_EXCHANGE_RATE, exception.getErrorCode());
        verify(exchangeRateService, times(1)).getExchangeRate(senderAccount.getCurrency(), receiverAccount.getCurrency());
    }

}