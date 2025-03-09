package banking.payment.service;

import banking.account.dto.response.AccountPublicInfoResponse;
import banking.account.service.AccountService;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.payment.dto.request.PaymentRefundRequest;
import banking.payment.dto.response.RefundPaymentResponse;
import banking.payment.entity.Payment;
import banking.payment.entity.PaymentView;
import banking.payment.enums.PaymentStatus;
import banking.payment.repository.PaymentRepository;
import banking.payment.dto.request.PaymentRequest;
import banking.payment.repository.PaymentViewRepository;
import banking.transfer.dto.response.PaymentTransferDetailResponse;
import banking.transfer.service.TransferService;
import banking.user.dto.response.UserPublicInfoResponse;
import banking.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransferService transferService;
    private final AccountService accountService;
    private final UserService userService;
    private final PaymentViewRepository paymentViewRepository;

    @Transactional
    public PaymentView processPayment(Long userId, PaymentRequest request) {
        PaymentTransferDetailResponse transferResponse = transferService.transfer(userId, request);

        UserPublicInfoResponse payeeUserPublicInfo = userService.findUserPublicInfo(transferResponse.depositAccountId());

        Payment payment = Payment.builder()
            .payerId(userId)
            .payeeId(payeeUserPublicInfo.id())
            .transferId(transferResponse.transferId())
            .paymentStatus(PaymentStatus.PAYMENT_PENDING)
            .build();

        paymentRepository.save(payment);

        return paymentViewRepository.findByPaymentId(payment.getId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));
    }

    @Transactional
    public RefundPaymentResponse refundPayment(Long userId, PaymentRefundRequest request) {
        Payment payment = paymentRepository.findById(request.paymentId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        verifyRequesterOwnership(payment, userId);

        if(payment.getPaymentStatus().equals(PaymentStatus.PAYMENT_CANCELLED)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        PaymentTransferDetailResponse refundTransferResponse = transferService.transferForRefund(userId, payment.getTransferId(), request.withdrawalAccountPassword());

        // Payment 상태 변경 (PENDING/COMPLETED -> CANCELLED)
        payment.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);

        // TODO: 결제 취소에 대한 입출금 계좌에 모두 알림

        AccountPublicInfoResponse depositAccountPublicInfo = accountService.findAccountPublicInfo(refundTransferResponse.depositAccountId(), refundTransferResponse);
        return RefundPaymentResponse.of(depositAccountPublicInfo.accountNumber(), refundTransferResponse.amount());
    }

    private void verifyRequesterOwnership(Payment payment, Long userId) {
        if (!payment.getPayerId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
    }

    public PaymentView findPaymentById(Long userId, Long paymentId) {
        boolean isExist = paymentRepository.existsByIdAndPayerId(paymentId, userId);
        if (!isExist) {
            throw new CustomException(ErrorCode.NOT_FOUND_PAYMENT);
        }

        return paymentViewRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));
    }
}