package SN.BANK.payment.controller;

import SN.BANK.common.aop.Decrypt;
import SN.BANK.payment.dto.request.PaymentRefundRequestDto;
import SN.BANK.payment.dto.request.PaymentRequestDto;
import SN.BANK.payment.dto.response.PaymentResponseDto;
import SN.BANK.payment.dto.response.RefundPaymentResponseDto;
import SN.BANK.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "결제 API")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 요청을 처리하고 결과를 반환
     *
     * @param request 결제 요청 데이터를 담은 DTO
     * @return paymentId를 담은 응답 DTO
     */
    @Operation(summary = "결제", description = "바디에 {withdrawAccountNumber, depositAccountNumber,amount, password}을 json 형식으로 보내주세요.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "결제 성공", content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌입니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "비밀번호가 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "잔액이 부족합니다. / 같은 계좌 간 이체는 불가합니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    @Decrypt
    public ResponseEntity<?> processPayment(@Valid @RequestBody PaymentRequestDto request) {
        PaymentResponseDto response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 내역을 기반으로 환불 처리
     *
     * @param request 환불하려는 결제 내역 ID
     * @return 결제 취소 결과 DTO
     */
    @Operation(summary = "결제 취소", description = "바디에 {paymentId, password}을 json 형식으로 보내주세요.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "결제취소 완료", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "비밀번호가 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "이미 결제 취소된 내역입니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/cancel")
    @Decrypt
    public ResponseEntity<?> refundPayment(@Valid @RequestBody PaymentRefundRequestDto request) {
        RefundPaymentResponseDto response = paymentService.refundPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 결제 내역 조회
     *
     * @param paymentId 조회하려는 결제 ID
     * @return 특정 결제 내역 응답 DTO
     */
    @Operation(summary = "결제 내역 조회", description = "url 변수에 결제 id를 보내주세요.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "결제 내역 조회 완료", content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "결제 내역이 존재하지 않습니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> findPaymentDetail(@PathVariable Long paymentId) {
        PaymentResponseDto response = paymentService.findPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

}
