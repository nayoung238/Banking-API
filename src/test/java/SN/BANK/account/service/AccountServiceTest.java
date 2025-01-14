package SN.BANK.account.service;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.entity.Account;
import SN.BANK.domain.Users;
import SN.BANK.domain.enums.Currency;
import SN.BANK.user.repository.UsersRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

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

    @Test
    @DisplayName("계좌 개설 성공 테스트")
    void createAccount() {

        // given
        CreateAccountRequest createAccountRequest =
                CreateAccountRequest.builder()
                        .password("1234")
                        .currency(Currency.KRW)
                        .build();

        Users user = new Users("테스트이름", "test1234", "test1234");

        when(usersRepository.findById(eq(1L))).thenReturn(Optional.of(user));
        when(accountRepository.existsByAccountNumber(any(String.class))).thenReturn(false);
        // save 메서드 호출 시 전달된 객체를 그대로 반환하도록.
        when(accountRepository.save(any(Account.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        // when
        Account createdAccount = accountService.createAccount(1L, createAccountRequest);

        // then
        Assertions.assertNotNull(createdAccount);
        Assertions.assertAll(
                () -> Assertions.assertEquals(createdAccount.getAccountName(), "SN은행-계좌"),
                () -> Assertions.assertEquals(createdAccount.getUser().getLoginId(), "test1234"),
                () -> Assertions.assertEquals(createdAccount.getAccountNumber().length(), 14)
        );
    }

    @Test
    @DisplayName("14자의 숫자로 이루어진 계좌번호를 생성할 수 있다.")
    void generateAccountNumber_success() {
        String accountNumber = accountService.generateAccountNumber();

        Assertions.assertNotNull(accountNumber);
        Assertions.assertEquals(accountNumber.length(), 14);
        Assertions.assertTrue(accountNumber.matches("\\d{14}"));
    }

}