package banking.payment.service;

import banking.account.entity.Account;
import banking.account.repository.AccountRepository;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.payment.dto.request.PaymentRefundRequestDto;
import banking.payment.dto.response.PaymentResponseDto;
import banking.payment.dto.response.RefundPaymentResponseDto;
import banking.payment.entity.Payment;
import banking.payment.enums.PaymentStatus;
import banking.payment.repository.PaymentRepository;
import banking.payment.dto.request.PaymentRequestDto;
import banking.transfer.dto.request.TransferRequestDto;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import banking.transfer.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        validateDifferentAccounts(request.withdrawalAccountNumber(), request.depositAccountNumber());

        Transfer transfer = transferService.transfer(TransferRequestDto.of(request));
        Payment payment = Payment.builder()
            .transferId(transfer.getId())
            .paymentStatus(PaymentStatus.PAYMENT_PENDING)
            .build();

        paymentRepository.save(payment);

        return createPaymentResponseDto(payment.getId());
    }

    @Transactional
    public RefundPaymentResponseDto refundPayment(PaymentRefundRequestDto request) {
        Payment payment = paymentRepository.findById(request.paymentId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        verifyWithdrawalAccountPassword(payment.getTransferId(), request.withdrawalAccountPassword());

        if(payment.getPaymentStatus().equals(PaymentStatus.PAYMENT_CANCELLED)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        Transfer refundTransfer = transferService.transferForRefund(payment.getTransferId());

        // Payment 상태 변경 (PENDING/COMPLETED -> CANCELLED)
        payment.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);

        // TODO: 결제 취소에 대한 입출금 계좌에 모두 알림

        Account depositAccount = accountRepository.findById(refundTransfer.getDepositAccountId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));
        return RefundPaymentResponseDto.of(depositAccount.getAccountNumber(), refundTransfer.getTransferDetails().get(TransferType.DEPOSIT).getAmount());
    }

    private void verifyWithdrawalAccountPassword(Long transferId, String withdrawalAccountPassword) {
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

        Account withdrawalAccount = accountRepository.findById(transfer.getWithdrawalAccountId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

        if(!withdrawalAccount.getPassword().equals(withdrawalAccountPassword)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }

    public PaymentResponseDto findPaymentById(Long paymentId) {
        return createPaymentResponseDto(paymentId);
    }

    private void validateDifferentAccounts(String withdrawAccountNumber, String depositAccountNumber) {
        if (withdrawAccountNumber.equals(depositAccountNumber)) {
            throw new CustomException(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED);
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