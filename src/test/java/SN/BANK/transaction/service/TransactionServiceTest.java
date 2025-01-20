package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.account.enums.Currency;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import SN.BANK.transaction.repository.TransactionRepository;
import SN.BANK.users.entity.Users;
import SN.BANK.users.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class TransactionServiceTest {

    @Mock
    AccountService accountService;

    @Mock
    UsersService usersService;

    @InjectMocks
    TransactionService transactionService;

    @Mock
    TransactionRepository transactionRepository;

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
    @DisplayName("사용자는 이체할 수 있다.")
    void createTransaction() {

        // given
        Long userId = 1L;

        TransactionRequest transactionRequest = TransactionRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(BigDecimal.valueOf(2970))
                .build();

        when(accountService.findValidAccount(1L)).thenReturn(senderAccount);
        when(accountService.findValidAccount(2L)).thenReturn(receiverAccount);
        when(usersService.validateUser(userId)).thenReturn(sender);
        when(transactionRepository.existsByGroupId(any())).thenReturn(false);
        when(transactionRepository.save(any(TransactionEntity.class)))
                .thenAnswer(invocation -> {
                    TransactionEntity entity = invocation.getArgument(0);
                    ReflectionTestUtils.setField(entity, "id", 1L);
                    return entity;
                });

        // when
        TransactionResponse response = transactionService.createTransaction(userId, transactionRequest);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getSenderAccountId());
        assertEquals(2L, response.getReceiverAccountId());
        assertEquals(TransactionType.WITHDRAWAL, response.getTransactionType());
        assertEquals(response.getSenderName(), "테스터1");
        assertEquals(response.getReceiverName(), "테스터2");
        assertEquals(BigDecimal.valueOf(2970), response.getAmount());
        assertEquals(BigDecimal.valueOf(7030), response.getBalance());
    }

    @Test
    @DisplayName("비밀번호 불일치로 인한 거래 실패 테스트")
    void createTransaction_INALID_PASSWOD() {
        // given
        Long userId = 1L;

        TransactionRequest transactionRequest = TransactionRequest.builder()
                .accountPassword("0000") // 다른 비밀번호
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(BigDecimal.valueOf(5000))
                .build();

        when(accountService.findValidAccount(1L)).thenReturn(senderAccount);
        when(accountService.findValidAccount(2L)).thenReturn(receiverAccount);
        when(usersService.validateUser(userId)).thenReturn(sender);

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transactionService.createTransaction(userId, transactionRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INVALID_PASSWORD, customException.getErrorCode());
    }

    @Test
    @DisplayName("잔액 부족으로 인한 거래 실패 테스트")
    void createTransaction_INSUFFICIENT_MONEY() {
        // given
        Long userId = 1L;

        TransactionRequest transactionRequest = TransactionRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .amount(BigDecimal.valueOf(10001)) // 이체액을 계좌 잔액보다 크게 잡음
                .build();

        when(accountService.findValidAccount(1L)).thenReturn(senderAccount);
        when(accountService.findValidAccount(2L)).thenReturn(receiverAccount);
        when(usersService.validateUser(userId)).thenReturn(sender);

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transactionService.createTransaction(userId, transactionRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INSUFFICIENT_MONEY, customException.getErrorCode());
    }

    @Test
    @DisplayName("송신 계좌와 수신 계좌가 동일한 경우 거래 실패 테스트")
    void transfer_INVALID_createTransaction() {
        // given
        Long userId = 1L;

        TransactionRequest transactionRequest = TransactionRequest.builder()
                .accountPassword("1234")
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(senderAccount.getId())
                .amount(BigDecimal.valueOf(5000))
                .build();

        when(accountService.findValidAccount(1L)).thenReturn(senderAccount);
        when(usersService.validateUser(userId)).thenReturn(sender);

        // when
        CustomException customException =
                assertThrows(CustomException.class, () -> transactionService.createTransaction(userId, transactionRequest));

        // then
        assertNotNull(customException);
        assertEquals(ErrorCode.INVALID_TRANSFER, customException.getErrorCode());
    }

    @Test
    @DisplayName("입금액은 계좌 잔액보다 많을 수 없다.")
    void isGreaterThanAmount() {
        assertTrue(transactionService.isGreaterThanAmount(senderAccount, BigDecimal.valueOf(10000)));
        assertFalse(transactionService.isGreaterThanAmount(senderAccount, BigDecimal.valueOf(10001)));
    }
}