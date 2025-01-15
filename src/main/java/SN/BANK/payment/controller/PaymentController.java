package SN.BANK.payment.controller;

import SN.BANK.payment.dto.PaymentListResponseDto;
import SN.BANK.payment.dto.PaymentRefundRequestDto;
import SN.BANK.payment.dto.PaymentRequestDto;
import SN.BANK.payment.dto.PaymentResponseDto;
import SN.BANK.payment.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
     * @return paymentId를 담은 응답 DTO
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDto> makePayment(@Valid @RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.makePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 내역을 기반으로 환불 처리
     *
     * @param request 환불하려는 결제 내역 ID
     * @return 환불 결과 DTO
     */
    @PostMapping("/cancel")
    public ResponseEntity<String> refundPayment(@Valid @RequestBody PaymentRefundRequestDto request) {
        paymentService.refundPayment(request.getDepositId());
        return ResponseEntity.ok("결제취소 완료");
    }

    /**
     * 세션에서 유저 ID를 가져와 결제 내역 조회
     *
     * @param session HTTP 세션
     * @return 유저의 결제내역을 담은 응답 DTO 리스트
     */
    @GetMapping("/history")
    public ResponseEntity<List<PaymentListResponseDto>> getUserPaymentHistory(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        List<PaymentListResponseDto> paymentHistory = paymentService.getUserPaymentHistory(userId);
        return ResponseEntity.ok(paymentHistory);
    }
}
