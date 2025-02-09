package SN.BANK.transfer.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.exchangeRate.ExchangeRateService;
import SN.BANK.fixture.testEntity.AccountFixture;
import SN.BANK.fixture.testEntity.UserFixture;
import SN.BANK.transfer.dto.request.TransferRequestDto;
import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.repository.TransferRepository;
import SN.BANK.users.entity.Users;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class TransferServiceUnitTest {

    @InjectMocks
    TransferService transferService;

    @Mock
    TransferRepository transferRepository;

    @Mock
    AccountRepository accountRepository;

    @Mock
    ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("[이체 성공 테스트] executeTransfer 메서드 검증")
    void execute_transfer_succeed_test () {
        // given
        Users user = UserFixture.USER_FIXTURE_1.createUser();
        Account withdrawalAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);
        Account depositAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_2.createAccount(user);

        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(withdrawalAccount.getAccountNumber())
            .withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(BigDecimal.valueOf(1000))
            .build();

        when(accountRepository.findByAccountNumberWithPessimisticLock(withdrawalAccount.getAccountNumber())).thenReturn(Optional.of(withdrawalAccount));
        when(accountRepository.findByAccountNumber(depositAccount.getAccountNumber())).thenReturn(Optional.of(depositAccount));
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(BigDecimal.ONE);
        when(accountRepository.findByIdWithPessimisticLock(depositAccount.getId())).thenReturn(Optional.of(depositAccount));

        TransferService.TransferResultHandler<Transfer> resultHandler = (transfer, wAc, dAc) -> transfer;

        // when
        Transfer result = transferService.executeTransfer(user.getId(), transferRequest, resultHandler);

        // then
        assertNotNull(result);
        verify(accountRepository, times(1)).findByAccountNumberWithPessimisticLock(withdrawalAccount.getAccountNumber());
        verify(accountRepository, times(1)).findByAccountNumber(depositAccount.getAccountNumber());
        verify(exchangeRateService, times(1)).getExchangeRate(depositAccount.getCurrency(), withdrawalAccount.getCurrency());
    }

    @Test
    @DisplayName("[이체 실패 테스트] 이체를 요청한 사용자가 송금 계좌 오너가 아니면 UNAUTHORIZED_ACCOUNT_ACCESS 에러 코드 예외 반환")
    void user_is_not_owner_test () {
        // given
        Users user = UserFixture.USER_FIXTURE_1.createUser();
        Account withdrawalAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);
        Account depositAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_2.createAccount(user);

        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(withdrawalAccount.getAccountNumber())
            .withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(BigDecimal.valueOf(1000))
            .build();

        when(accountRepository.findByAccountNumberWithPessimisticLock(withdrawalAccount.getAccountNumber())).thenReturn(Optional.of(withdrawalAccount));

        // when & then
        Assertions.assertThatThrownBy(() -> transferService.transfer(user.getId() + 1, transferRequest))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customException = (CustomException) ex;
                assertEquals(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS, customException.getErrorCode());
                assertEquals(HttpStatus.FORBIDDEN, customException.getErrorCode().getStatus());
                assertEquals("해당 계좌에 대한 접근 권한이 없습니다.", customException.getErrorCode().getMessage());
            });

        verify(accountRepository, times(1)).findByAccountNumberWithPessimisticLock(withdrawalAccount.getAccountNumber());
    }

    @Test
    @DisplayName("[이체 실패 테스트] 비밀번호 일치하지 않으면 INVALID_PASSWORD 에러 코드 예외 발생")
    void invalid_password_test () {
        // given
        Users user = UserFixture.USER_FIXTURE_1.createUser();
        Account withdrawalAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);
        Account depositAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_2.createAccount(user);

        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(withdrawalAccount.getAccountNumber())
            .withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword() + "1234")
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(BigDecimal.valueOf(1000))
            .build();

        when(accountRepository.findByAccountNumberWithPessimisticLock(withdrawalAccount.getAccountNumber())).thenReturn(Optional.of(withdrawalAccount));

        // when & then
        Assertions.assertThatThrownBy(() -> transferService.transfer(user.getId(), transferRequest))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customException = (CustomException) ex;
                assertEquals(ErrorCode.INVALID_PASSWORD, customException.getErrorCode());
                assertEquals(HttpStatus.UNAUTHORIZED, customException.getErrorCode().getStatus());
                assertEquals("비밀번호가 일치하지 않습니다.", customException.getErrorCode().getMessage());
            });

        verify(accountRepository, times(1)).findByAccountNumberWithPessimisticLock(withdrawalAccount.getAccountNumber());
    }

    @Test
    @DisplayName("[이체 실패 테스트] 잔액 부족 시 이체 실패")
    void insufficient_balance_test () {
        // given
        Users user = UserFixture.USER_FIXTURE_1.createUser();
        Account withdrawalAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);
        Account depositAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_2.createAccount(user);

        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(withdrawalAccount.getAccountNumber())
            .withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(withdrawalAccount.getBalance().multiply(BigDecimal.valueOf(2)))
            .build();

        when(accountRepository.findByAccountNumberWithPessimisticLock(withdrawalAccount.getAccountNumber())).thenReturn(Optional.of(withdrawalAccount));
        when(accountRepository.findByAccountNumber(depositAccount.getAccountNumber())).thenReturn(Optional.of(depositAccount));
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(BigDecimal.ONE);

        // when & then
        Assertions.assertThatThrownBy(() -> transferService.transfer(user.getId(), transferRequest))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customException = (CustomException) ex;
                assertEquals(ErrorCode.INSUFFICIENT_BALANCE, customException.getErrorCode());
                assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
                assertEquals("잔액이 부족합니다.", customException.getErrorCode().getMessage());
            });

        verify(accountRepository, times(1)).findByAccountNumberWithPessimisticLock(withdrawalAccount.getAccountNumber());
        verify(accountRepository, times(1)).findByAccountNumber(depositAccount.getAccountNumber());
        verify(exchangeRateService, times(1)).getExchangeRate(depositAccount.getCurrency(), withdrawalAccount.getCurrency());
        verify(transferRepository, times(0)).save(any(Transfer.class));
    }

    @Test
    @DisplayName("[이체 실패 테스트] 송신 계좌와 수신 계좌가 동일한 경우 이체 실패")
    void transfer_fail_when_same_account () {
        // given
        Users user = UserFixture.USER_FIXTURE_1.createUser();
        Account account = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);

        TransferRequestDto transferRequest = TransferRequestDto.builder()
            .withdrawalAccountNumber(account.getAccountNumber())
            .withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
            .depositAccountNumber(account.getAccountNumber())
            .amount(BigDecimal.valueOf(2000))
            .build();

        // when & then
        Assertions.assertThatThrownBy(() -> transferService.transfer(user.getId(), transferRequest))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customException = (CustomException) ex;
                assertEquals(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED, customException.getErrorCode());
                assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
                assertEquals("같은 계좌 간 이체는 불가합니다.", customException.getErrorCode().getMessage());
            });
    }
}