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
import banking.transfer.dto.request.TransferRequestDto;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.service.TransferService;
import banking.users.dto.response.UserPublicInfoDto;
import banking.users.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransferService transferService;
    private final AccountService accountService;
    private final UsersService usersService;

    @Transactional
    public PaymentResponseDto processPayment(Long requesterId, PaymentRequestDto request) {
        //출금 계좌와 입금 계좌가 다른지 확인
        validateDifferentAccounts(request.withdrawalAccountNumber(), request.depositAccountNumber());

        Transfer transfer = transferService.transfer(TransferRequestDto.of(request));

        // TODO: 상대 정보를 가져올 수 있는 권한
        UserPublicInfoDto payeeUserPublicInfo = usersService.findUserPublicInfo(transfer.getDepositAccountId());

        Payment payment = Payment.builder()
            .payerId(requesterId)
            .payeeId(payeeUserPublicInfo.id())
            .transferGroupId(transfer.getTransferGroupId())
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

        Transfer refundTransfer = transferService.transferForRefund(requesterId, payment.getTransferGroupId(), request.withdrawalAccountPassword());

        // Payment 상태 변경 (PENDING/COMPLETED -> CANCELLED)
        payment.updatePaymentStatus(PaymentStatus.PAYMENT_CANCELLED);

        // TODO: 결제 취소에 대한 입출금 계좌에 모두 알림

        AccountPublicInfoDto depositAccountPublicInfo = accountService.findAccountPublicInfo(refundTransfer.getDepositAccountId());
        return RefundPaymentResponseDto.of(depositAccountPublicInfo.accountNumber(), refundTransfer.getAmount());
    }

    private void verifyRequesterOwnership(Payment payment, Long requesterId) {
        if (!payment.getPayerId().equals(requesterId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_PAYMENT_ACCESS);
        }
    }

    public PaymentResponseDto findPaymentById(Long requesterId, Long paymentId) {
        return createPaymentResponseDto(requesterId, paymentId);
    }

    private void validateDifferentAccounts(String withdrawalAccountNumber, String depositAccountNumber) {
        if (withdrawalAccountNumber.equals(depositAccountNumber)) {
            throw new CustomException(ErrorCode.SAME_ACCOUNT_TRANSFER_NOT_ALLOWED);
        }
    }

    private PaymentResponseDto createPaymentResponseDto(Long requesterId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_PAYMENT));

        TransferType transferType = findTransferType(requesterId, payment.getPayerId(), payment.getPayeeId());
        Transfer transfer = transferService.findTransferEntity(payment.getTransferGroupId(), transferType);

        Long requesterAccountId = (transferType.equals(TransferType.WITHDRAWAL)) ?
            transfer.getWithdrawalAccountId() : transfer.getDepositAccountId();

        // 공개용 계좌 정보 GET
        AccountPublicInfoDto accountPublicInfo = accountService.findAccountPublicInfo(requesterAccountId);

        // 공개용 사용자 정보 GET
        UserPublicInfoDto userPublicInfo = usersService.findUserPublicInfo(requesterId, requesterAccountId);

        return PaymentResponseDto.of(payment, transfer, accountPublicInfo.accountNumber(), userPublicInfo.name());
    }

    public TransferType findTransferType(Long requesterId, Long payerId, Long payeeId) {
        if (payerId.equals(requesterId)) {
            return TransferType.WITHDRAWAL;
        }
        if (payeeId.equals(requesterId)) {
            return TransferType.DEPOSIT;
        }
        throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
    }
}