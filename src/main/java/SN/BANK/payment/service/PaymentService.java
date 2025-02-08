package SN.BANK.payment.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.dto.response.PaymentResponseDto;
import SN.BANK.payment.entity.Payment;
import SN.BANK.payment.enums.PaymentStatus;
import SN.BANK.payment.repository.PaymentRepository;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.transfer.dto.request.TransferRequestDto;
import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.repository.TransferRepository;
import SN.BANK.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransferService transferService;
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto request) {
        //출금 계좌와 입금 계좌가 다른지 확인
        validateDifferentAccounts(request.withdrawAccountNumber(), request.depositAccountNumber());

        Transfer transfer = transferService.transfer(TransferRequestDto.of(request));
        Payment payment = Payment.builder()
            .transferId(transfer.getId())
            .paymentStatus(PaymentStatus.PAYMENT_PENDING)
            .build();

        paymentRepository.save(payment);

        return createPaymentResponseDto(payment.getId());
    }

    @Transactional
    public void refundPayment(PaymentRefundRequestDto request) {
        // 결제 내역 조회
        Payment paymentList = getPaymentListByIdWithLock(request.paymentId());

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
        validateAccountPassword(withdrawAccount, request.password());
        // 이미 결제 취소된 상태인지 확인
        validatePaymentStatus(paymentList, PaymentStatus.PAYMENT_CANCELLED, ErrorCode.PAYMENT_ALREADY_CANCELLED);

        BigDecimal exchangeRate = paymentList.getExchangeRate();

        // 입출금 처리 및 거래내역 생성
        transactionService.createTransactionForPayment(depositAccount,withdrawAccount,paymentList.getAmount().multiply(exchangeRate),BigDecimal.ONE.divide(exchangeRate, 20, BigDecimal.ROUND_HALF_UP));

        // 결제 상태 변경
        paymentList.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);
    }

    public PaymentResponseDto findPaymentById(Long paymentId) {
        return createPaymentResponseDto(paymentId);
    }

    private void validateDifferentAccounts(String withdrawAccountNumber, String depositAccountNumber) {
        if (withdrawAccountNumber.equals(depositAccountNumber)) {
            throw new CustomException(ErrorCode.INVALID_TRANSFER);
        }
    }

    private PaymentResponseDto createPaymentResponseDto(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        Transfer transfer = transferRepository.findById(payment.getTransferId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

        Account withdrawalAccount = accountRepository.findById(transfer.getWithdrawalAccountId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

        Account depositAccount = accountRepository.findById(transfer.getDepositAccountId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));

        return PaymentResponseDto.of(payment, transfer, withdrawalAccount.getAccountNumber(), depositAccount.getUser().getName());
    }
}