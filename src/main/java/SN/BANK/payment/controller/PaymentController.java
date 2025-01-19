package SN.BANK.payment.controller;

import SN.BANK.payment.dto.PaymentListResponseDto;
import SN.BANK.payment.dto.PaymentRefundRequestDto;
import SN.BANK.payment.dto.PaymentRequestDto;
import SN.BANK.payment.dto.PaymentResponseDto;
import SN.BANK.payment.service.PaymentService;
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

        paymentService.refundPayment(request);
        return ResponseEntity.ok("결제취소 완료");
    }

    /**
     * 특정 결제 내역 조회
     *
     * @param paymentId 조회하려는 결제 ID
     * @return 특정 결제 내역 응답 DTO
     */
    @GetMapping("/history/{paymentId}")
    public ResponseEntity<PaymentListResponseDto> getPaymentDetail(
            @PathVariable Long paymentId) {
        PaymentListResponseDto paymentListResponseDto = paymentService.getPaymentListById(paymentId);
        return ResponseEntity.ok(paymentListResponseDto);
    }

}
