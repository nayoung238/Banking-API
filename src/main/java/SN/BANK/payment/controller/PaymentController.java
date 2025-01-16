package SN.BANK.payment.controller;

import SN.BANK.payment.dto.PaymentListResponseDto;
import SN.BANK.payment.dto.PaymentRefundRequestDto;
import SN.BANK.payment.dto.PaymentRequestDto;
import SN.BANK.payment.dto.PaymentResponseDto;
import SN.BANK.payment.service.PaymentService;
import SN.BANK.users.entity.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 요청을 처리하고 결과를 반환
     *
     * @param request 결제 요청 데이터를 담은 DTO
     * @param customUserDetails 인증된 사용자 정보
     * @return paymentId를 담은 응답 DTO
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDto> makePayment(@Valid @RequestBody PaymentRequestDto request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUsers().getId();
        PaymentResponseDto response = paymentService.makePayment(request,userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 내역을 기반으로 환불 처리
     *
     * @param request 환불하려는 결제 내역 ID
     * @param customUserDetails 인증된 사용자 정보
     * @return 환불 결과 DTO
     */
    @PostMapping("/cancel")
    public ResponseEntity<String> refundPayment(@Valid @RequestBody PaymentRefundRequestDto request, @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUsers().getId();
        paymentService.refundPayment(request.getDepositId(),userId);
        return ResponseEntity.ok("결제취소 완료");
    }

    /**
     * 세션에서 유저 ID를 가져와 결제 내역 조회
     *
     * @param customUserDetails 인증된 사용자 정보
     * @return 유저의 결제내역을 담은 응답 DTO 리스트
     */
    @GetMapping("/history")
    public ResponseEntity<List<PaymentListResponseDto>> getUserPaymentHistory(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getUsers().getId();
        List<PaymentListResponseDto> paymentHistory = paymentService.getUserPaymentHistory(userId);
        return ResponseEntity.ok(paymentHistory);
    }
}
