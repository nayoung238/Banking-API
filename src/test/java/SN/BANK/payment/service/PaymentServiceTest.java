package SN.BANK.payment.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.exchangeRate.ExchangeRateService;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.payment.dto.response.PaymentListResponseDto;
import SN.BANK.payment.dto.response.PaymentResponseDto;
import SN.BANK.payment.enums.PaymentStatus;
import SN.BANK.payment.repository.PaymentListRepository;
import SN.BANK.transfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.entity.PaymentList;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PaymentListRepository paymentListRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private TransferService transferService;

    private Account mockWithdrawAccount;
    private Account mockDepositAccount;

    @BeforeEach
    void setUp() {
        // Mock 계좌
        mockWithdrawAccount = new Account(1L, null, "password", "12345", BigDecimal.valueOf(1000000), "WithdrawAccount", null, Currency.KRW);
        mockDepositAccount = new Account(2L, null, "password", "67890", BigDecimal.valueOf(500), "DepositAccount", null, Currency.USD);
    }

    @Test
    @DisplayName("결제 성공")
    void makePaymentSuccess() {
        // Given
        PaymentRequestDto request = PaymentRequestDto.builder()
                .withdrawAccountNumber("12345")
                .depositAccountNumber("67890")
                .amount(BigDecimal.valueOf(100))
                .password("password")
                .build();

        PaymentList paymentList = new PaymentList(1L,"12345","67890",BigDecimal.valueOf(100),LocalDateTime.now(),Currency.USD,BigDecimal.valueOf(1400),PaymentStatus.PAYMENT_COMPLETED);

        when(accountRepository.findByAccountNumberWithLock("12345")).thenReturn(Optional.of(mockWithdrawAccount));
        when(accountRepository.findByAccountNumberWithLock("67890")).thenReturn(Optional.of(mockDepositAccount));
        when(exchangeRateService.getExchangeRate(Currency.KRW, Currency.USD)).thenReturn(BigDecimal.valueOf(1450));
        when(paymentListRepository.save(any(PaymentList.class))).thenReturn(paymentList);
        // When
        PaymentResponseDto response = paymentService.makePayment(request);

        // Then
        assertNotNull(response);
        verify(accountRepository, times(2)).findByAccountNumberWithLock(anyString());
        verify(exchangeRateService, times(1)).getExchangeRate(Currency.KRW, Currency.USD);
        verify(paymentListRepository, times(1)).save(any(PaymentList.class));
    }

    @Test
    @DisplayName("결제 실패 - 일치하지 않는 비밀번호")
    void makePaymentInvalidPassword() {
        // Given
        PaymentRequestDto request = PaymentRequestDto.builder()
                .withdrawAccountNumber("12345")
                .depositAccountNumber("67890")
                .amount(BigDecimal.valueOf(100))
                .password("wrongPassword")
                .build();

        when(accountRepository.findByAccountNumberWithLock("12345")).thenReturn(Optional.of(mockWithdrawAccount));
        when(accountRepository.findByAccountNumberWithLock("67890")).thenReturn(Optional.of(mockDepositAccount));
        // When
        CustomException exception = assertThrows(CustomException.class, () -> paymentService.makePayment(request));

        // Then
        assertEquals(ErrorCode.INVALID_PASSWORD, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 실패 - 잔액 부족")
    void makePaymentInsufficientBalance() {
        // Given
        PaymentRequestDto request = PaymentRequestDto.builder()
                .withdrawAccountNumber("12345")
                .depositAccountNumber("67890")
                .amount(BigDecimal.valueOf(2000))
                .password("password")
                .build();

        when(accountRepository.findByAccountNumberWithLock("12345")).thenReturn(Optional.of(mockWithdrawAccount));
        when(accountRepository.findByAccountNumberWithLock("67890")).thenReturn(Optional.of(mockDepositAccount));

        when(exchangeRateService.getExchangeRate(Currency.KRW, Currency.USD)).thenReturn(BigDecimal.valueOf(1450));

        // When
        CustomException exception = assertThrows(CustomException.class, () -> paymentService.makePayment(request));

        // Then
        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 실패 - 존재하지 않는 계좌")
    void makePaymentAccountNotFound() {
        // Given
        PaymentRequestDto request = PaymentRequestDto.builder()
                .withdrawAccountNumber("99999")
                .depositAccountNumber("67890")
                .amount(BigDecimal.valueOf(100))
                .password("password")
                .build();
        when(accountRepository.findByAccountNumberWithLock("99999")).thenReturn(Optional.empty());

        // When
        CustomException exception = assertThrows(CustomException.class, () -> paymentService.makePayment(request));

        // Then
        assertEquals(ErrorCode.NOT_FOUND_ACCOUNT, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 실패 - 출금계좌와 입금계좌가 동일함")
    void makePaymentSameAccount() {
        // Given
        PaymentRequestDto request = PaymentRequestDto.builder()
                .withdrawAccountNumber("12345")
                .depositAccountNumber("12345")
                .amount(BigDecimal.valueOf(100))
                .password("password")
                .build();

        // When
        CustomException exception = assertThrows(CustomException.class, () -> paymentService.makePayment(request));

        // Then
        assertEquals(ErrorCode.INVALID_TRANSFER, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 실패 - 환율 데이터 가져오기 실패")
    void makePaymentExchangeRateFetchFail() {
        // Given
        PaymentRequestDto request = PaymentRequestDto.builder()
                .withdrawAccountNumber("12345")
                .depositAccountNumber("67890")
                .amount(BigDecimal.valueOf(100))
                .password("password")
                .build();
        when(accountRepository.findByAccountNumberWithLock("12345")).thenReturn(Optional.of(mockWithdrawAccount));
        when(accountRepository.findByAccountNumberWithLock("67890")).thenReturn(Optional.of(mockDepositAccount));
        when(exchangeRateService.getExchangeRate(Currency.KRW, Currency.USD))
                .thenThrow(new CustomException(ErrorCode.EXCHANGE_RATE_FETCH_FAIL));

        // When
        CustomException exception = assertThrows(CustomException.class, () -> paymentService.makePayment(request));

        // Then
        assertEquals(ErrorCode.EXCHANGE_RATE_FETCH_FAIL, exception.getErrorCode());
    }

    @Test
    @DisplayName("결제 취소 성공")
    void refundPaymentSuccess() {
        // Given
        PaymentRefundRequestDto request = new PaymentRefundRequestDto(1L, "password");
        PaymentList mockPayment = PaymentList.builder()
                .withdrawAccountNumber("12345")
                .depositAccountNumber("67890")
                .amount(BigDecimal.valueOf(100))
                .exchangeRate(BigDecimal.ONE)
                .currency(Currency.KRW)
                .paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
                .build();

        when(paymentListRepository.findByIdWithLock(1L)).thenReturn(Optional.of(mockPayment));
        when(accountRepository.findByAccountNumberWithLock("12345")).thenReturn(Optional.of(mockWithdrawAccount));
        when(accountRepository.findByAccountNumberWithLock("67890")).thenReturn(Optional.of(mockDepositAccount));

        // When
        paymentService.refundPayment(request);

        // Then
        assertEquals(PaymentStatus.PAYMENT_CANCELLED, mockPayment.getPaymentStatus());
        verify(paymentListRepository, times(1)).findByIdWithLock(1L);
    }

    @Test
    @DisplayName("결제 취소 실패 - 결제 내역 조회 실패")
    void refundPaymentFailure_NotFoundPayment() {
        // given
        PaymentRefundRequestDto request = new PaymentRefundRequestDto(1L,"password");

        when(paymentListRepository.findByIdWithLock(request.paymentId())).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(CustomException.class, () -> paymentService.refundPayment(request));

        // then
        assertEquals(ErrorCode.NOT_FOUND_PAYMENT_LIST, exception.getErrorCode());
        verify(paymentListRepository, times(1)).findByIdWithLock(request.paymentId());
        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    @DisplayName("결제 취소 실패 - 이미 결제 취소된 상태")
    void refundPaymentFailure_AlreadyCancelled() {
        // given
        PaymentRefundRequestDto request = new PaymentRefundRequestDto(1L,"password");

        PaymentList mockPayment = PaymentList.builder()
                .withdrawAccountNumber("12345")
                .depositAccountNumber("67890")
                .amount(BigDecimal.valueOf(100))
                .exchangeRate(BigDecimal.ONE)
                .currency(Currency.KRW)
                .paymentStatus(PaymentStatus.PAYMENT_CANCELLED)
                .build();

        when(paymentListRepository.findByIdWithLock(1L)).thenReturn(Optional.of(mockPayment));
        when(accountRepository.findByAccountNumberWithLock("12345")).thenReturn(Optional.of(mockWithdrawAccount));
        when(accountRepository.findByAccountNumberWithLock("67890")).thenReturn(Optional.of(mockDepositAccount));

        // when
        CustomException exception = assertThrows(CustomException.class, () -> paymentService.refundPayment(request));

        // then
        assertEquals(ErrorCode.PAYMENT_ALREADY_CANCELLED, exception.getErrorCode());
        verify(paymentListRepository, times(1)).findByIdWithLock(request.paymentId());
    }

    @Test
    @DisplayName("결제 조회 성공")
    void getPaymentDetailsSuccess() {
        // given

        PaymentList paymentList = PaymentList.builder()
                .withdrawAccountNumber("12345")
                .depositAccountNumber("67890")
                .amount(BigDecimal.valueOf(100))
                .currency(Currency.KRW)
                .exchangeRate(BigDecimal.valueOf(1.12))
                .paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
                .paidAt(LocalDateTime.now())
                .build();

        when(paymentListRepository.findById(1L)).thenReturn(Optional.of(paymentList));

        // when
        PaymentListResponseDto response = paymentService.getPaymentListById(1L);

        // then
        assertNotNull(response);
        assertEquals("12345", response.getWithdrawAccountNumber());
        assertEquals("67890", response.getDepositAccountNumber());
        verify(paymentListRepository, times(1)).findById(1L);
    }
}
