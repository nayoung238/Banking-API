package SN.BANK.account.service;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.account.dto.response.AccountResponse;
import SN.BANK.account.dto.response.CreateAccountResponse;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.entity.Account;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.account.enums.Currency;
import SN.BANK.users.entity.Users;
import SN.BANK.users.service.UsersService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(SpringExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    UsersService usersService;

    @InjectMocks
    AccountService accountService;

    Users user;

    @BeforeEach
    void setUp() {
        user = new Users("테스트이름", "test1234", "test1234");
    }

    @Test
    @DisplayName("계좌를 개설할 수 있다.")
    void createAccount() {
        // given
        Long userId = 1L;

        CreateAccountRequest createAccountRequest =
                CreateAccountRequest.builder()
                        .password("1234")
                        .currency(Currency.KRW)
                        .build();

        when(usersService.validateUser(userId)).thenReturn(user);
        when(accountRepository.existsByAccountNumber(any(String.class))).thenReturn(false);
        // save 메서드 호출 시 전달된 객체를 그대로 반환하도록.
        when(accountRepository.save(any(Account.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        CreateAccountResponse createdAccount = accountService.createAccount(userId, createAccountRequest);

        // then
        assertNotNull(createdAccount);
        assertAll(
                () -> assertEquals("SN은행-계좌", createdAccount.accountName()),
                () -> assertEquals(14, createdAccount.accountNumber().length()),
                () -> assertEquals(Currency.KRW, createdAccount.currency())
        );
    }

    @Test
    @DisplayName("사용자의 모든 계좌를 조회할 수 있다.")
    void findAllAccount() {
        // given
        Long userId = 1L;

        Account account1 = Account.builder()
                .user(user)
                .build();

        Account account2 = Account.builder()
                .user(user)
                .build();

        List<Account> accounts = List.of(account1, account2);

        when(usersService.validateUser(userId)).thenReturn(user);
        when(accountRepository.findByUser(user)).thenReturn(accounts);

        // when
        List<AccountResponse> findAccounts = accountService.findAllAccounts(userId);

        // then
        assertNotNull(findAccounts);
        assertEquals(2, findAccounts.size());
    }

    @Test
    @DisplayName("계좌를 조회할 수 있다.")
    void findAccount() {
        // given
        Long userId = 1L;
        Long accountId = 123L;
        BigDecimal balance = BigDecimal.valueOf(5000);

        Account account = Account.builder()
                .user(user)
                .money(balance)
                .build();

        when(usersService.validateUser(userId)).thenReturn(user);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        ReflectionTestUtils.setField(user, "id", userId);

        // when
        AccountResponse findAccount = accountService.findAccount(userId, accountId);

        // then
        assertNotNull(findAccount);
        assertEquals(account.getMoney(), findAccount.money());
    }

    @Test
    @DisplayName("계좌 조회 시, 유효하지 않은 계좌인 경우 에러를 던진다.")
    void findAccount_NOT_FOUND_ACCOUNT() {
        // given
        Long userId = 1L;
        Long accountId = 123L;

        doThrow(new CustomException(ErrorCode.NOT_FOUND_ACCOUNT))
                .when(accountRepository).findById(any());

        // when
        CustomException exception =
                assertThrows(CustomException.class, () -> accountService.findAccount(userId, accountId));

        // then
        assertEquals(ErrorCode.NOT_FOUND_ACCOUNT, exception.getErrorCode());
        verify(accountRepository, times(1)).findById(any());
    }

    @Test
    @DisplayName("계좌 조회 시, 계좌 접근 권한이 없는 사용자인 경우 에러를 던진다.")
    void findAccount_UNAUTHORIZED_ACCOUNT_ACCESS() {
        // given
        Long userId = 1L;
        Long accountId = 123L;

        when(usersService.validateUser(userId)).thenReturn(user);
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // when
        CustomException exception =
                assertThrows(CustomException.class, () -> accountService.findAccount(userId, accountId));

        // then
        assertEquals(ErrorCode.NOT_FOUND_ACCOUNT, exception.getErrorCode());
    }

    @Test
    @DisplayName("14자의 숫자로 이루어진 계좌번호를 생성할 수 있다.")
    void generateAccountNumber_success() {
        String accountNumber = accountService.generateAccountNumber();

        assertNotNull(accountNumber);
        assertEquals(14, accountNumber.length());
        assertTrue(accountNumber.matches("\\d{14}"));
    }

}