package SN.BANK.payment.service;

import SN.BANK.account.entity.Account;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.exchangeRate.ExchangeRateService;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.entity.PaymentList;
import SN.BANK.payment.dto.response.PaymentListResponseDto;
import SN.BANK.payment.enums.PaymentStatus;
import SN.BANK.payment.repository.PaymentListRepository;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor

public class PaymentService {

    private final ExchangeRateService exchangeRateService;
    private final AccountRepository accountRepository;
    private final PaymentListRepository paymentListRepository;
    private final TransactionService transactionService;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Long makePayment(PaymentRequestDto request) {


        //출금계좌와 입금 계좌가 동일한지 확인
        validateDifferentAccounts(request.getWithdrawAccountNumber(), request.getDepositAccountNumber());

        // 출금 계좌
        Account withdrawAccount;
        // 입금 계좌
        Account depositAccount;

        // 계좌 번호가 사전 순서상 앞에있는것부터 조회 (데드락 방지)
        if(request.getWithdrawAccountNumber().compareTo(request.getDepositAccountNumber()) < 0){
            withdrawAccount = getAccountByNumberWithLock(request.getWithdrawAccountNumber());
            depositAccount = getAccountByNumberWithLock(request.getDepositAccountNumber());
        }
        else {
            depositAccount = getAccountByNumberWithLock(request.getDepositAccountNumber());
            withdrawAccount = getAccountByNumberWithLock(request.getWithdrawAccountNumber());
        }

        // 계좌 비밀번호 확인
        validateAccountPassword(withdrawAccount, request.getPassword());


        // 환율 가져오기
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(withdrawAccount.getCurrency(), depositAccount.getCurrency());

        //출금 계좌 잔액 확인
        validateAccountBalance(withdrawAccount,request.getAmount().multiply(exchangeRate));

        // 입출금 처리 및 거래내역 생성
        transactionService.createTransactionForPayment(withdrawAccount,depositAccount, request.getAmount(),exchangeRate);

        // 결제내역 생성 및 저장
        PaymentList payment = createPaymentList(request, withdrawAccount, depositAccount, exchangeRate);
        PaymentList savedPaymentList = paymentListRepository.save(payment);

        return savedPaymentList.getId();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void refundPayment(PaymentRefundRequestDto request) {
        // 결제 내역 조회
        PaymentList paymentList = getPaymentListByIdWithLock(request.getPaymentId());

        // 출금 계좌
        Account withdrawAccount;
        // 입금 계좌
        Account depositAccount;

        // 계좌 번호가 사전 순서상 앞에있는것부터 조회 (데드락 방지)
        if(paymentList.getWithdrawAccountNumber().compareTo(paymentList.getDepositAccountNumber()) < 0){
            withdrawAccount = getAccountByNumberWithLock(paymentList.getWithdrawAccountNumber());
            depositAccount = getAccountByNumberWithLock(paymentList.getDepositAccountNumber());
        }
        else {
            depositAccount = getAccountByNumberWithLock(paymentList.getDepositAccountNumber());
            withdrawAccount = getAccountByNumberWithLock(paymentList.getWithdrawAccountNumber());
        }

        // 계좌 비밀번호 확인
        validateAccountPassword(withdrawAccount, request.getPassword());
        // 이미 결제 취소된 상태인지 확인
        validatePaymentStatus(paymentList, PaymentStatus.PAYMENT_CANCELLED, ErrorCode.PAYMENT_ALREADY_CANCELLED);

        BigDecimal exchangeRate = paymentList.getExchangeRate();

        // 입출금 처리 및 거래내역 생성
        transactionService.createTransactionForPayment(depositAccount,withdrawAccount,paymentList.getAmount().multiply(exchangeRate),BigDecimal.ONE.divide(exchangeRate, 20, BigDecimal.ROUND_HALF_UP));

        // 결제 상태 변경
        paymentList.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);
    }

    public PaymentListResponseDto getPaymentListById(Long paymentId) {
        PaymentList payment = getPaymentById(paymentId);
        return PaymentListResponseDto.of(payment);
    }

    // 결제내역 조회
    private PaymentList getPaymentListByIdWithLock(Long paymentId){
        return paymentListRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT_LIST));
    }

    // 출금 및 입금 계좌가 동일한지 검증
    private void validateDifferentAccounts(String withdrawAccountNumber, String depositAccountNumber) {
        if (withdrawAccountNumber.equals(depositAccountNumber)) {
            throw new CustomException(ErrorCode.INVALID_TRANSFER);
        }
    }

    //계좌 번호로 계좌 조회 (락)
    private Account getAccountByNumberWithLock(String accountNumber) {
        return accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
    }

    // 계좌 비밀번호 검증
    private void validateAccountPassword(Account account, String password) {
        if (!account.getPassword().equals(password)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }

    // 결제 상태 검증 메서드
    private void validatePaymentStatus(PaymentList paymentList, PaymentStatus invalidStatus, ErrorCode errorCode) {
        if (paymentList.getPaymentStatus() == invalidStatus) {
            throw new CustomException(errorCode);
        }
    }

    //잔액 검증
    private void validateAccountBalance(Account account, BigDecimal amount) {
        if (account.getMoney().compareTo(amount) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    // 결제 ID로 결제 내역 조회
    public PaymentList getPaymentById(Long paymentId) {
        return paymentListRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT_LIST));
    }

    // 결제 내역 생성
    private PaymentList createPaymentList(PaymentRequestDto request, Account withdrawAccount, Account depositAccount, BigDecimal exchangeRate) {
        return PaymentList.builder()
                .paidAt(LocalDateTime.now())
                .withdrawAccountNumber(withdrawAccount.getAccountNumber())
                .depositAccountNumber(depositAccount.getAccountNumber())
                .amount(request.getAmount()) // 입금 계좌 통화 기준
                .exchangeRate(exchangeRate)
                .currency(depositAccount.getCurrency())
                .paymentStatus(PaymentStatus.PAYMENT_COMPLETED)
                .build();
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Long makePaymentNonLock(PaymentRequestDto request) {


        //출금계좌와 입금 계좌가 동일한지 확인
        validateDifferentAccounts(request.getWithdrawAccountNumber(), request.getDepositAccountNumber());

        // 출금 계좌
        Account withdrawAccount = accountRepository.findByAccountNumber(request.getWithdrawAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
        // 입금 계좌
        Account depositAccount = accountRepository.findByAccountNumberWithLock(request.getDepositAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // 계좌 비밀번호 확인
        validateAccountPassword(withdrawAccount, request.getPassword());


        // 환율 가져오기
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(withdrawAccount.getCurrency(), depositAccount.getCurrency());

        //출금 계좌 잔액 확인
        validateAccountBalance(withdrawAccount,request.getAmount().multiply(exchangeRate));

        // 입출금 처리 및 거래내역 생성
        transactionService.createTransactionForPayment(withdrawAccount,depositAccount, request.getAmount(),exchangeRate);

        // 결제내역 생성 및 저장
        PaymentList payment = createPaymentList(request, withdrawAccount, depositAccount, exchangeRate);
        PaymentList savedPaymentList = paymentListRepository.save(payment);

        return savedPaymentList.getId();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void refundPaymentNonLock(PaymentRefundRequestDto request) {
        // 결제 내역 조회
        PaymentList paymentList = getPaymentById(request.getPaymentId());

        // 출금 계좌
        Account withdrawAccount = accountRepository.findByAccountNumber(paymentList.getWithdrawAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
        // 입금 계좌
        Account depositAccount = accountRepository.findByAccountNumberWithLock(paymentList.getDepositAccountNumber())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // 계좌 비밀번호 확인
        validateAccountPassword(withdrawAccount, request.getPassword());
        // 이미 결제 취소된 상태인지 확인
        validatePaymentStatus(paymentList, PaymentStatus.PAYMENT_CANCELLED, ErrorCode.PAYMENT_ALREADY_CANCELLED);

        BigDecimal exchangeRate = paymentList.getExchangeRate();

        // 입출금 처리 및 거래내역 생성
        transactionService.createTransactionForPayment(depositAccount,withdrawAccount,paymentList.getAmount().multiply(exchangeRate),BigDecimal.ONE.divide(exchangeRate, 20, BigDecimal.ROUND_HALF_UP));

        // 결제 상태 변경
        paymentList.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);
    }


}