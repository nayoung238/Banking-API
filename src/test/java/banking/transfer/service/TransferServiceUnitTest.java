package banking.transfer.service;

import banking.account.dto.response.AccountPublicInfoResponse;
import banking.account.entity.Account;
import banking.account.service.AccountService;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.exchangeRate.ExchangeRateService;
import banking.fixture.testEntity.AccountFixture;
import banking.fixture.testEntity.UserFixture;
import banking.transfer.dto.request.TransferRequest;
import banking.transfer.entity.Transfer;
import banking.transfer.repository.TransferRepository;
import banking.user.entity.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;

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
    DepositAsyncService depositAsyncService;

    @Mock
    AccountService accountService;

    @Mock
    ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("[이체 성공 테스트] executeTransfer 메서드 검증")
    void execute_transfer_succeed_test () {
        // given
        User user = UserFixture.USER_FIXTURE_1.createUser();
        Account withdrawalAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);
        Account depositAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_2.createAccount(user);

        AccountPublicInfoResponse mockDepositAccountPublicInfo = AccountPublicInfoResponse.of(depositAccount);

        TransferRequest transferRequest = TransferRequest.builder()
            .withdrawalAccountId(withdrawalAccount.getId())
            .withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
            .depositAccountNumber(depositAccount.getAccountNumber())
            .amount(BigDecimal.valueOf(1000))
            .build();

        when(accountService.findAccountWithLock(anyLong(), anyLong(), anyString())).thenReturn(withdrawalAccount);
        when(accountService.findAccountPublicInfo(anyString())).thenReturn(mockDepositAccountPublicInfo);
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(BigDecimal.ONE);
        when(accountService.findAccountWithLock(anyLong(), any(Transfer.class))).thenReturn(depositAccount);

        TransferService.TransferResultHandler<Transfer> resultHandler = (transfer, wAc, dAc) -> transfer;

        // when
        Transfer result = transferService.executeTransfer(user.getId(), transferRequest, resultHandler);

        // then
        assertNotNull(result);
        verify(accountService, times(1)).findAccountWithLock(anyLong(), anyLong(), anyString());
        verify(accountService, times(1)).findAccountPublicInfo(depositAccount.getAccountNumber());
        verify(exchangeRateService, times(1)).getExchangeRate(depositAccount.getCurrency(), withdrawalAccount.getCurrency());
        verify(depositAsyncService, times(1)).processDepositAsync(any(Transfer.class));
    }

    @Test
    @DisplayName("[이체 실패 테스트] 송신 계좌와 수신 계좌 동일하면 이체 실패")
    void transfer_fail_when_same_account () {
        // given
        User user = UserFixture.USER_FIXTURE_1.createUser();
        Account account = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);

        TransferRequest transferRequest = TransferRequest.builder()
            .withdrawalAccountId(account.getId())
            .withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
            .depositAccountNumber(account.getAccountNumber())
            .amount(BigDecimal.valueOf(2000))
            .build();

        when(accountService.findAccountWithLock(anyLong(), anyLong(), anyString())).thenReturn(account);

        // when & then
        Assertions.assertThatThrownBy(() -> transferService.transfer(user.getId(), transferRequest))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customException = (CustomException) ex;
                assertEquals(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED, customException.getErrorCode());
                assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
                assertEquals("같은 계좌 간 거래는 불가합니다.", customException.getErrorCode().getMessage());
            });

        verify(transferRepository, times(0)).save(any(Transfer.class));
    }
}