package SN.BANK.payment.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.fixture.testEntity.AccountFixture;
import SN.BANK.fixture.testEntity.UserFixture;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.payment.enums.PaymentStatus;
import SN.BANK.payment.repository.PaymentRepository;
import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.entity.TransferDetails;
import SN.BANK.transfer.enums.TransferType;
import SN.BANK.transfer.repository.TransferRepository;
import SN.BANK.transfer.service.TransferService;
import SN.BANK.users.entity.Users;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.entity.Payment;
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
			.build();

		TransferDetails withdrawalTransferDetails = TransferDetails.builder()
			.amount(BigDecimal.valueOf(200))
			.build();
		mockTransfer.addTransferDetails(TransferType.WITHDRAWAL, withdrawalTransferDetails);

		Payment mockPayment = Payment.builder().id(1L).build();

		when(transferService.transfer(any())).thenReturn(mockTransfer);
		when(paymentRepository.findById(any())).thenReturn(Optional.ofNullable(mockPayment));
		when(transferRepository.findById(any())).thenReturn(Optional.of(mockTransfer));
		when(accountRepository.findById(any())).thenReturn(Optional.of(withdrawalAccount));

		// when
		paymentService.processPayment(paymentRequest);

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
		Assertions.assertThatThrownBy(() -> paymentService.processPayment(paymentRequest))
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
			.transferId(1L)
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
		when(transferRepository.findById(anyLong())).thenReturn(Optional.of(mockTransfer));
		when(accountRepository.findById(anyLong())).thenReturn(Optional.of(withdrawalAccount));

        PaymentRefundRequestDto refundRequest = PaymentRefundRequestDto.builder()
			.paymentId(1L)
			.withdrawalAccountPassword(AccountFixture.ACCOUNT_FIXTURE_KRW_1.createAccount(user).getPassword())
			.build();

        // when & then
		Assertions.assertThatThrownBy(() -> paymentService.refundPayment(refundRequest))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException) ex;
				assertEquals(ErrorCode.PAYMENT_ALREADY_CANCELLED, customException.getErrorCode());
				assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
				assertEquals("이미 결제 취소된 내역입니다.", customException.getErrorCode().getMessage());
			});

        verify(paymentRepository, times(1)).findById(anyLong());
		verify(transferRepository, times(1)).findById(anyLong());
		verify(accountRepository, times(1)).findById(anyLong());
    }
}
