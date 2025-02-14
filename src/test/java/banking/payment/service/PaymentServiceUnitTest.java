package banking.payment.service;

import banking.account.entity.Account;
import banking.account.repository.AccountRepository;
import banking.fixture.testEntity.AccountFixture;
import banking.fixture.testEntity.UserFixture;
import banking.payment.dto.request.PaymentRequestDto;
import banking.payment.enums.PaymentStatus;
import banking.payment.repository.PaymentRepository;
import banking.transfer.entity.Transfer;
import banking.transfer.repository.TransferRepository;
import banking.transfer.service.TransferService;
import banking.users.entity.Users;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.payment.dto.request.PaymentRefundRequestDto;
import banking.payment.entity.Payment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class PaymentServiceUnitTest {

    @InjectMocks
    PaymentService paymentService;

    @Mock
    AccountRepository accountRepository;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    TransferService transferService;

    @Mock
    TransferRepository transferRepository;

    @Test
    @DisplayName("[결제 성공 테스트] 결제 처리 시 TransferService에 의존")
    void payment_test () {
		// given
		Users user = UserFixture.USER_FIXTURE_1.createUser();
		Account withdrawalAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);
		Account depositAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_2.createAccount(user);

		PaymentRequestDto paymentRequest = PaymentRequestDto.builder()
			.withdrawalAccountNumber(withdrawalAccount.getAccountNumber())
			.withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
			.depositAccountNumber(depositAccount.getAccountNumber())
			.amount(BigDecimal.valueOf(200))
			.build();

		Transfer mockTransfer = Transfer.builder()
			.id(1L)
			.exchangeRate(BigDecimal.ONE)
			.amount(BigDecimal.valueOf(200))
			.build();

		Payment mockPayment = Payment.builder().id(1L).build();

		when(transferService.transfer(any())).thenReturn(mockTransfer);
		when(paymentRepository.findById(any())).thenReturn(Optional.ofNullable(mockPayment));
		when(transferRepository.findByTransferGroupIdAndTransferType(any(), any())).thenReturn(Optional.of(mockTransfer));
		when(accountRepository.findById(any())).thenReturn(Optional.of(withdrawalAccount));

		// when
		paymentService.processPayment(user.getId(), paymentRequest);

		// then
		verify(transferService, times(1)).transfer(any());
		verify(paymentRepository, times(1)).save(any(Payment.class));
	}


    @Test
    @DisplayName("[결제 실패 테스트] 송신 계좌와 수신 계좌 동일하면 결제 불가")
    void payment_fail_when_same_account () {
        // given
		Users user = UserFixture.USER_FIXTURE_1.createUser();
		Account account = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);

		PaymentRequestDto paymentRequest = PaymentRequestDto.builder()
			.withdrawalAccountNumber(account.getAccountNumber())
			.withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
			.depositAccountNumber(account.getAccountNumber())
			.amount(BigDecimal.valueOf(200))
			.build();

        // when & then
		Assertions.assertThatThrownBy(() -> paymentService.processPayment(user.getId(), paymentRequest))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException) ex;
				assertEquals(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED, customException.getErrorCode());
				assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
				assertEquals("같은 계좌 간 거래는 불가합니다.", customException.getErrorCode().getMessage());
			});
    }

    @Test
    @DisplayName("[결제 취소 실패 테스트] 이미 결제 취소된 상태는 처리하지 않음")
    void refund_payment_fail_when_already_cancelled () {
        // given
		Payment mockPayment = Payment.builder()
			.id(1L)
			.transferGroupId("173234Ad2D")
			.paymentStatus(PaymentStatus.PAYMENT_CANCELLED)
			.build();

		Transfer mockTransfer = Transfer.builder()
			.id(1L)
			.withdrawalAccountId(1L)
			.exchangeRate(BigDecimal.ONE)
			.build();

		Users user = UserFixture.USER_FIXTURE_1.createUser();
		Account withdrawalAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);

		when(paymentRepository.findById(anyLong())).thenReturn(Optional.ofNullable(mockPayment));
		when(transferRepository.findByTransferGroupIdAndTransferType(any(), any())).thenReturn(Optional.of(mockTransfer));
		when(accountRepository.findById(anyLong())).thenReturn(Optional.of(withdrawalAccount));

        PaymentRefundRequestDto refundRequest = PaymentRefundRequestDto.builder()
			.paymentId(1L)
			.withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
			.build();

        // when & then
		Assertions.assertThatThrownBy(() -> paymentService.refundPayment(user.getId(), refundRequest))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException) ex;
				assertEquals(ErrorCode.PAYMENT_ALREADY_CANCELLED, customException.getErrorCode());
				assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
				assertEquals("이미 결제 취소된 내역입니다.", customException.getErrorCode().getMessage());
			});

        verify(paymentRepository, times(1)).findById(anyLong());
		verify(transferRepository, times(1)).findByTransferGroupIdAndTransferType(any(), any());
		verify(accountRepository, times(1)).findById(anyLong());
		verify(transferService, times(0)).transferForRefund(any(), any());
    }
}
