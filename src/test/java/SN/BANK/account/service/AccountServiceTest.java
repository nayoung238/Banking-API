package SN.BANK.account.service;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.entity.Account;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.domain.enums.Currency;
import SN.BANK.users.entity.Users;
import SN.BANK.users.repository.UsersRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    UsersRepository usersRepository;

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
        CreateAccountRequest createAccountRequest =
                CreateAccountRequest.builder()
                        .password("1234")
                        .currency(Currency.KRW)
                        .build();

        when(usersRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(any(String.class))).thenReturn(false);
        // save 메서드 호출 시 전달된 객체를 그대로 반환하도록.
        when(accountRepository.save(any(Account.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        Account createdAccount = accountService.createAccount(1L, createAccountRequest);

        // then
        assertNotNull(createdAccount);
        assertAll(
                () -> assertEquals("SN은행-계좌", createdAccount.getAccountName()),
                () -> assertEquals("test1234", createdAccount.getUser().getLoginId()),
                () -> assertEquals(14, createdAccount.getAccountNumber().length())
        );
    }

    @Test
    @DisplayName("사용자의 모든 계좌를 조회할 수 있다.")
    void findAllAccount() {

        // given
        Long userId = 1L;
        Long accountId = 123L;

        Account account1 = Account.builder()
                .user(user)
                .build();

        Account account2 = Account.builder()
                .user(user)
                .build();

        List<Account> accounts = List.of(account1, account2);

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findByUser(user)).thenReturn(accounts);

        // when
        List<Account> findAccounts = accountService.findAllAccounts(userId);

        // then
        assertNotNull(findAccounts);
        assertEquals(2, findAccounts.size());
        assertEquals(accounts, findAccounts);
    }

    @Test
    @DisplayName("계좌를 조회할 수 있다.")
    void findAccount() {

        // given
        Long userId = 1L;
        Long accountId = 123L;

        Account account = Account.builder()
                .user(user)
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // when
        Account findAccount = accountService.findAccount(userId, accountId);

        // then
        assertNotNull(findAccount);
        assertEquals(account, findAccount);
    }

    @Test
    @DisplayName("계좌 조회 시, 유효하지 않은 계좌인 경우 에러를 던진다.")
    void findAccount_NOT_FOUND_ACCOUNT() {

        // given
        Long userId = 1L;
        Long accountId = 123L;

        Users anotherUser = Users.builder()
                .name("테스터")
                .loginId("test1111")
                .password("1111")
                .build();

        Account account = Account.builder()
                .user(anotherUser)
                .build();

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // when
        CustomException exception =
                assertThrows(CustomException.class, () -> accountService.findAccount(userId, accountId));

        // then
        assertEquals(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 조회 시, 계좌 접근 권한이 없는 사용자인 경우 에러를 던진다.")
    void findAccount_UNAUTHORIZED_ACCOUNT_ACCESS() {

        // given
        Long userId = 1L;
        Long accountId = 123L;

        when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
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