package banking.account.service;

import banking.account.dto.request.AccountCreationRequest;
import banking.account.dto.response.AccountDetailResponse;
import banking.account.repository.AccountRepository;
import banking.account.entity.Account;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.account.enums.Currency;
import banking.fixture.testEntity.UserFixture;
import banking.transfer.entity.Transfer;
import banking.user.entity.User;
import banking.user.repository.UserRepository;
import banking.user.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class AccountServiceUnitTest {

    @InjectMocks
    AccountService accountService;

    @Mock
    AccountRepository accountRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    UserService userService;

    @Test
    @DisplayName("[계좌 접근 실패 테스트] 소유자만 락으로 계좌 접근")
    void should_lock_account_for_owner_only () {
        // given
        final long userId = 1L;
        final long accountId = 43L;
        final String accountPassword = "password1234";

        when(accountRepository.existsByIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accountService.findAccountWithLock(userId, accountId, accountPassword))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customException = (CustomException) ex;
                assertEquals(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS, customException.getErrorCode());
                assertEquals(HttpStatus.FORBIDDEN, customException.getErrorCode().getStatus());
                assertEquals("해당 계좌에 대한 접근 권한이 없습니다.", customException.getErrorCode().getMessage());
            });

        verify(accountRepository, times(0)).findByIdWithLock(any());
    }

    @Test
    @DisplayName("[계좌 접근 성공 테스트] 거래 당사자만 상대 계좌에 락 사용 가능")
    void should_allow_access_to_related_user_with_lock_success_test () {
        // given
        final long requesterAccountId = 43L;
        final long payeeAccountId = 21L;
        Transfer mockTransfer = Transfer.builder()
            .withdrawalAccountId(requesterAccountId)
            .depositAccountId(payeeAccountId)
            .build();

        when(accountRepository.findByIdWithLock(payeeAccountId)).thenReturn(Optional.of(new Account()));

        // when
        accountService.findAccountWithLock(payeeAccountId, mockTransfer);

        // verify
        verify(accountRepository, times(1)).findByIdWithLock(payeeAccountId);
    }

    @Test
    @DisplayName("[계좌 접근 실패 테스트] 거래 당사자가 아니면 상대 계좌 접근 불가")
    void should_allow_access_to_related_user_with_lock_failure_test () {
        // given
        final long requesterAccountId = 43L;
        final long payeeAccountId = 21L;
        Transfer mockTransfer = Transfer.builder()
            .withdrawalAccountId(requesterAccountId)
            .depositAccountId(payeeAccountId)
            .build();

        final long requestedAccountId = 1L;

        // when & verify
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accountService.findAccountWithLock(requestedAccountId, mockTransfer))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customException = (CustomException) ex;
                assertEquals(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS, customException.getErrorCode());
                assertEquals(HttpStatus.FORBIDDEN, customException.getErrorCode().getStatus());
                assertEquals("해당 계좌에 대한 접근 권한이 없습니다.", customException.getErrorCode().getMessage());
            });

        verify(accountRepository, times(0)).findByIdWithLock(payeeAccountId);
    }

    @Test
    @DisplayName("[계좌 조회 실패 테스트] 계좌가 없는 경우 NOT_FOUND_ACCOUNT 에러 코드 예외 발생")
    void not_found_account_test() {
        // given
        when(accountRepository.findById(any())).thenThrow(new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> accountService.findAccount(1L, 2L))
            .isInstanceOf(CustomException.class)
            .satisfies(ex -> {
                CustomException customException = (CustomException) ex;
                assertEquals(ErrorCode.NOT_FOUND_ACCOUNT, customException.getErrorCode());
                assertEquals(HttpStatus.NOT_FOUND, customException.getErrorCode().getStatus());
                assertEquals("존재하지 않는 계좌입니다.", customException.getErrorCode().getMessage());
            });
    }

    @Test
    @DisplayName("[계좌번호 유효성 테스트] 고유한 15자리 계좌번호 검증")
    void generate_account_number_test () {
        // given (중복 계좌 없다고 가정)
        User user = UserFixture.USER_FIXTURE_1.createUser();
        when(userRepository.findById(anyLong())).thenReturn(Optional.ofNullable(user));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
		when(accountRepository.save(any(Account.class))).thenReturn(null);

        AccountCreationRequest request = AccountCreationRequest.builder()
            .password("123432")
            .currency(Currency.KRW)
            .accountName("test-account-name")
            .build();

        // when
		assert user != null;
		AccountDetailResponse response = accountService.createAccount(user.getId(), request);

        // then
        assertNotNull(response.accountNumber());
        assertEquals(15, response.accountNumber().length()); // 7자리 - 7자리
        assertTrue(response.accountNumber().matches("\\d{7}-\\d{7}")); // 정규식으로 형식 검증
        verify(accountRepository, atLeastOnce()).existsByAccountNumber(anyString());
    }

    // TODO: 비즈니스 로직 수정 필요
//    @Test
//    @DisplayName("[계좌번호 생성 로직 테스트] 계좌번호 생성 재시도 -> 최대 5번만 진행")
//    void generate_unique_account_number_test () {
//        // given
//        Users users = UserFixture.USER_FIXTURE_1.createUser();
//
//        when(userService.findUserEntity(anyLong())).thenReturn(users);
//        when(accountRepository.save(any())).thenThrow(DataIntegrityViolationException.class);
//
//        AccountCreationRequestDto request = AccountCreationRequestDto.builder()
//            .password("62324")
//            .currency(Currency.KRW)
//            .accountName("Test Account")
//            .build();
//
//        // when
//        accountService.createAccount(1L, request);
//
//        // then
//        verify(accountRepository, times(5)).save(any(Account.class));
//    }
}