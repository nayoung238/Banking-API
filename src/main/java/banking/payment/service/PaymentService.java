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
import banking.transfer.service.TransferQueryService;
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
    private final TransferQueryService transferQueryService;
    private final AccountService accountService;
    private final UserService userService;

    @Transactional
    public PaymentResponseDto processPayment(Long userId, PaymentRequestDto request) {
        TransferResponseForPaymentDto transferResponse = transferService.transfer(userId, request);

        UserPublicInfoDto payeeUserPublicInfo = userService.findUserPublicInfo(transferResponse.depositAccountId());

        Payment payment = Payment.builder()
            .payerId(userId)
            .payeeId(payeeUserPublicInfo.id())
            .transferGroupId(transferResponse.transferGroupId())
            .paymentStatus(PaymentStatus.PAYMENT_PENDING)
            .build();

        paymentRepository.save(payment);

        return createPaymentResponseDto(userId, payment.getId());
    }

    @Transactional
    public RefundPaymentResponseDto refundPayment(Long userId, PaymentRefundRequestDto request) {
        Payment payment = paymentRepository.findById(request.paymentId())
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        verifyRequesterOwnership(payment, userId);

        if(payment.getPaymentStatus().equals(PaymentStatus.PAYMENT_CANCELLED)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        TransferResponseForPaymentDto refundTransferResponse = transferService.transferForRefund(userId, payment.getTransferGroupId(), request.withdrawalAccountPassword());

        // Payment 상태 변경 (PENDING/COMPLETED -> CANCELLED)
        payment.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);

        // TODO: 결제 취소에 대한 입출금 계좌에 모두 알림

        AccountPublicInfoDto depositAccountPublicInfo = accountService.findAccountPublicInfo(refundTransferResponse.depositAccountId(), refundTransferResponse);
        return RefundPaymentResponseDto.of(depositAccountPublicInfo.accountNumber(), refundTransferResponse.amount());
    }

    private void verifyRequesterOwnership(Payment payment, Long userId) {
        if (!payment.getPayerId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
    }

    public PaymentResponseDto findPaymentById(Long userId, Long paymentId) {
        return createPaymentResponseDto(userId, paymentId);
    }

    private PaymentResponseDto createPaymentResponseDto(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        TransferResponseForPaymentDto transferResponse = transferQueryService.findTransfer(payment.getTransferGroupId(), userId);

        Long requesterAccountId = (transferResponse.transferType().equals(TransferType.WITHDRAWAL)) ?
            transferResponse.withdrawalAccountId() : transferResponse.depositAccountId();

        // 공개용 계좌 정보 GET
        AccountPublicInfoDto accountPublicInfo = accountService.findAccountPublicInfo(requesterAccountId, transferResponse);

        // 공개용 사용자 정보 GET
        UserPublicInfoDto userPublicInfo = userService.findUserPublicInfo(userId, requesterAccountId);

        return PaymentResponseDto.of(payment, transferResponse, accountPublicInfo.accountNumber(), userPublicInfo.name());
    }
}