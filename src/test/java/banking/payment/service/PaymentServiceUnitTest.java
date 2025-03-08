package banking.payment.service;

import banking.account.dto.response.AccountPublicInfoResponse;
import banking.account.entity.Account;
import banking.account.service.AccountService;
import banking.fixture.testEntity.AccountFixture;
import banking.fixture.testEntity.UserFixture;
import banking.payment.dto.request.PaymentRequest;
import banking.payment.enums.PaymentStatus;
import banking.payment.repository.PaymentRepository;
import banking.transfer.dto.response.PaymentTransferDetailResponse;
import banking.transfer.enums.TransferType;
import banking.transfer.service.TransferQueryService;
import banking.transfer.service.TransferService;
import banking.user.dto.response.UserPublicInfoResponse;
import banking.user.entity.User;
import banking.user.service.UserService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.payment.dto.request.PaymentRefundRequest;
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
	AccountService accountService;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    TransferService transferService;

	@Mock
	TransferQueryService transferQueryService;

	@Mock
	UserService userService;

    @Test
    @DisplayName("[결제 성공 테스트] 결제 처리 시 TransferService에 의존")
    void payment_test () {
		// given
		final Long transferId = 3L;
		User user = UserFixture.USER_FIXTURE_1.createUser();
		Account withdrawalAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user);
		Account depositAccount = AccountFixture.ACCOUNT_FIXTURE_KRW_2.createAccount(user);

		PaymentRequest paymentRequest = PaymentRequest.builder()
			.withdrawalAccountId(withdrawalAccount.getId())
			.withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
			.depositAccountNumber(depositAccount.getAccountNumber())
			.amount(BigDecimal.valueOf(200))
			.build();

		PaymentTransferDetailResponse transferResponse = PaymentTransferDetailResponse.builder()
			.transferId(transferId)
			.transferType(TransferType.WITHDRAWAL)
			.withdrawalAccountId(withdrawalAccount.getId())
			.depositAccountId(depositAccount.getId())
			.amount(BigDecimal.valueOf(200))
			.exchangeRate(BigDecimal.ONE)
			.build();

		Payment mockPayment = Payment.builder()
			.id(1L)
			.transferId(transferId)
			.payerId(withdrawalAccount.getId())
			.payeeId(depositAccount.getId())
			.build();

		AccountPublicInfoResponse mockAccountPublicInfoResponse = AccountPublicInfoResponse.builder()
			.ownerName(user.getName())
			.accountNumber(withdrawalAccount.getAccountNumber())
			.accountName(withdrawalAccount.getAccountName())
			.currency(withdrawalAccount.getCurrency())
			.build();

		UserPublicInfoResponse mockUserPublicInfoResponse = UserPublicInfoResponse.builder()
			.id(user.getId())
			.name(user.getName())
			.build();

		when(transferService.transfer(anyLong(), any(PaymentRequest.class))).thenReturn(transferResponse);
		when(userService.findUserPublicInfo(any())).thenReturn(mockUserPublicInfoResponse);
		when(paymentRepository.save(any(Payment.class))).thenReturn(null);
		when(paymentRepository.findById(any())).thenReturn(Optional.ofNullable(mockPayment));
		when(transferQueryService.findTransfer(anyLong())).thenReturn(transferResponse);
		when(accountService.findAccountPublicInfo(anyLong(), any(PaymentTransferDetailResponse.class))).thenReturn(mockAccountPublicInfoResponse);
		when(userService.findUserPublicInfo(any(), any())).thenReturn(mockUserPublicInfoResponse);

		// when
		paymentService.processPayment(user.getId(), paymentRequest);

		// then
		verify(transferService, times(1)).transfer(anyLong(), any(PaymentRequest.class));
		verify(paymentRepository, times(1)).save(any(Payment.class));
	}

    @Test
    @DisplayName("[결제 취소 실패 테스트] 이미 결제 취소된 상태는 처리하지 않음")
    void refund_payment_fail_when_already_cancelled () {
        // given
		User user = UserFixture.USER_FIXTURE_1.createUser();

		Payment mockPayment = Payment.builder()
			.id(1L)
			.payerId(user.getId())
			.payeeId(12L)
			.transferId(3L)
			.paymentStatus(PaymentStatus.PAYMENT_CANCELLED)
			.build();

		when(paymentRepository.findById(anyLong())).thenReturn(Optional.ofNullable(mockPayment));

        PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
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
		verify(transferService, times(0)).transferForRefund(any(), any(), any());
    }
}
