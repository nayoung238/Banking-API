package SN.BANK.payment.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.dto.response.PaymentResponseDto;
import SN.BANK.payment.dto.response.RefundPaymentResponseDto;
import SN.BANK.payment.entity.Payment;
import SN.BANK.payment.enums.PaymentStatus;
import SN.BANK.payment.repository.PaymentRepository;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.transfer.dto.request.TransferRequestDto;
import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.enums.TransferType;
import SN.BANK.transfer.repository.TransferRepository;
import SN.BANK.transfer.service.TransferService;
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
    public RefundPaymentResponseDto refundPayment(PaymentRefundRequestDto request) {
        Payment payment = paymentRepository.findById(request.paymentId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        verifyWithdrawalAccountPassword(payment.getTransferId(), request.withdrawalAccountPassword());

        Transfer refundTransfer = transferService.transferForRefund(payment.getTransferId());

        // Payment 상태 변경 (PENDING/COMPLETED -> CANCELLED)
        payment.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);

        // TODO: 결제 취소에 대한 입출금 계좌에 모두 알림

        Account depositAccount = accountRepository.findById(refundTransfer.getDepositAccountId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_DEPOSIT_ACCOUNT));
        return RefundPaymentResponseDto.builder()
            .depositAccountNumber(depositAccount.getAccountNumber())
            .depositAmount(refundTransfer.getTransferDetails().get(TransferType.DEPOSIT).getAmount())
            .build();
    }

    private void verifyWithdrawalAccountPassword(Long transferId, String withdrawalAccountPassword) {
        Transfer transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

        Account withdrawalAccount = accountRepository.findById(transfer.getWithdrawalAccountId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

        if(withdrawalAccount.getPassword().equals(withdrawalAccountPassword)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
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