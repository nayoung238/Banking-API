package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.transaction.dto.request.TransactionRequest;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
public class WithdrawServiceTest {

    @Mock
    AccountService accountService;

    @InjectMocks
    WithdrawService withdrawService;

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
                .money(BigDecimal.valueOf(10000))
                .accountName("test account1")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();

        receiverAccount = Account.builder()
                .id(2L)
                .user(receiver)
                .password("1234")
                .accountNumber("22222222222222")
                .money(BigDecimal.valueOf(10000))
                .accountName("test account2")
                .createdAt(LocalDateTime.now())
                .currency(Currency.KRW)
                .build();
    }

    @Test
    @DisplayName("송금 계좌 처리 - 잔액 감소")
    void sendSuccessTest() {

        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(1000.00);
        BigDecimal expectedBalance = senderAccount.getMoney().subtract(amount);

        TransactionRequest txRequest = TransactionRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();

        when(accountService.getAccountWithLock(senderAccount.getId())).thenReturn(senderAccount);
        doNothing().when(accountService).validAccountOwner(userId, senderAccount);
        doNothing().when(accountService).validAccountBalance(senderAccount, amount);

        Account actualSenderAccount = withdrawService.sendTo(userId, txRequest);

        assertNotNull(actualSenderAccount);
        assertEquals(expectedBalance, actualSenderAccount.getMoney());

        verify(accountService).getAccountWithLock(senderAccount.getId());
        verify(accountService).validAccountOwner(userId, senderAccount);
        verify(accountService).validAccountBalance(senderAccount, amount);
    }

    @Test
    @DisplayName("송금 계좌 예외 처리 - 잔액 부족")
    void sendFailTest() {

        // given
        Long userId = 1L;
        BigDecimal amount = BigDecimal.valueOf(20000.00);

        TransactionRequest txRequest = TransactionRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(amount)
                .build();

        when(accountService.getAccountWithLock(senderAccount.getId())).thenReturn(senderAccount);
        doThrow(new CustomException(ErrorCode.INSUFFICIENT_MONEY))
                .when(accountService).validAccountBalance(senderAccount, amount);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> {
            withdrawService.sendTo(userId, txRequest);
        });

        // then
        assertEquals(ErrorCode.INSUFFICIENT_MONEY, exception.getErrorCode());

        // 호출 검증
        verify(accountService).getAccountWithLock(senderAccount.getId());
        verify(accountService).validAccountBalance(senderAccount, amount);
    }

}
