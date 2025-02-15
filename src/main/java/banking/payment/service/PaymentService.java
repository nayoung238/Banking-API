package banking.payment.service;

import banking.account.dto.response.AccountPublicInfoDto;
import banking.account.service.AccountService;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.payment.dto.request.PaymentRefundRequestDto;
import banking.payment.dto.response.PaymentResponseDto;
import banking.payment.dto.response.RefundPaymentResponseDto;
import banking.payment.entity.Payment;
import banking.payment.enums.PaymentStatus;
import banking.payment.repository.PaymentRepository;
import banking.payment.dto.request.PaymentRequestDto;
import banking.transfer.dto.response.TransferResponseForPaymentDto;
import banking.transfer.enums.TransferType;
import banking.transfer.service.TransferService;
import banking.user.dto.response.UserPublicInfoDto;
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

    @Transactional
    public PaymentResponseDto processPayment(Long requesterId, PaymentRequestDto request) {
        TransferResponseForPaymentDto transferResponse = transferService.transfer(requesterId, request);

        UserPublicInfoDto payeeUserPublicInfo = userService.findUserPublicInfo(transferResponse.depositAccountId());

        Payment payment = Payment.builder()
            .payerId(requesterId)
            .payeeId(payeeUserPublicInfo.id())
            .transferGroupId(transferResponse.transferGroupId())
            .paymentStatus(PaymentStatus.PAYMENT_PENDING)
            .build();

        paymentRepository.save(payment);

        return createPaymentResponseDto(requesterId, payment.getId());
    }

    @Transactional
    public RefundPaymentResponseDto refundPayment(Long requesterId, PaymentRefundRequestDto request) {
        Payment payment = paymentRepository.findById(request.paymentId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        verifyRequesterOwnership(payment, requesterId);

        if(payment.getPaymentStatus().equals(PaymentStatus.PAYMENT_CANCELLED)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        TransferResponseForPaymentDto refundTransferResponse = transferService.transferForRefund(requesterId, payment.getTransferGroupId(), request.withdrawalAccountPassword());

        // Payment 상태 변경 (PENDING/COMPLETED -> CANCELLED)
        payment.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);

        // TODO: 결제 취소에 대한 입출금 계좌에 모두 알림

        AccountPublicInfoDto depositAccountPublicInfo = accountService.findAccountPublicInfo(refundTransferResponse.depositAccountId(), refundTransferResponse);
        return RefundPaymentResponseDto.of(depositAccountPublicInfo.accountNumber(), refundTransferResponse.amount());
    }

    private void verifyRequesterOwnership(Payment payment, Long requesterId) {
        if (!payment.getPayerId().equals(requesterId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
    }

    public PaymentResponseDto findPaymentById(Long requesterId, Long paymentId) {
        return createPaymentResponseDto(requesterId, paymentId);
    }

    private PaymentResponseDto createPaymentResponseDto(Long requesterId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        TransferResponseForPaymentDto transferResponse = transferService.findTransfer(payment.getTransferGroupId(), requesterId);

        Long requesterAccountId = (transferResponse.transferType().equals(TransferType.WITHDRAWAL)) ?
            transferResponse.withdrawalAccountId() : transferResponse.depositAccountId();

        // 공개용 계좌 정보 GET
        AccountPublicInfoDto accountPublicInfo = accountService.findAccountPublicInfo(requesterAccountId, transferResponse);

        // 공개용 사용자 정보 GET
        UserPublicInfoDto userPublicInfo = userService.findUserPublicInfo(requesterId, requesterAccountId);

        return PaymentResponseDto.of(payment, transferResponse, accountPublicInfo.accountNumber(), userPublicInfo.name());
    }
}