package banking.payment.api;

import banking.auth.entity.UserPrincipal;
import banking.common.aop.Decrypt;
import banking.payment.dto.request.PaymentRefundRequestDto;
import banking.payment.dto.request.PaymentRequestDto;
import banking.payment.dto.response.PaymentResponseDto;
import banking.payment.dto.response.RefundPaymentResponseDto;
import banking.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * @param paymentRequest 결제 요청 데이터를 담은 DTO
     * @return paymentId를 담은 응답 DTO
     */
    @Operation(
        summary = "결제",
        description = "바디에 {withdrawAccountNumber, depositAccountNumber,amount, password} json 형식 추가 & Request Header에 Access Token 설정"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "결제 성공", content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "비밀번호 불알치", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "잔액 부족 / 같은 계좌 간 이체 불가", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
//    @Decrypt
    public ResponseEntity<?> processPayment(@Valid @RequestBody PaymentRequestDto paymentRequest,
                                            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PaymentResponseDto response = paymentService.processPayment(userPrincipal.getId(), paymentRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 내역을 기반으로 환불 처리
     *
     * @param refundRequest 환불하려는 결제 내역 ID
     * @return 결제 취소 결과 DTO
     */
    @Operation(summary = "결제 취소", description = "바디에 {paymentId, password}을 json 형식 추가 & Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "결제취소 완료", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "비밀번호 불일치", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "이미 취소된 결제", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/cancel")
//    @Decrypt
    public ResponseEntity<?> refundPayment(@Valid @RequestBody PaymentRefundRequestDto refundRequest,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        RefundPaymentResponseDto response = paymentService.refundPayment(userPrincipal.getId(), refundRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 결제 내역 조회
     *
     * @param paymentId 조회하려는 결제 ID
     * @return 특정 결제 내역 응답 DTO
     */
    @Operation(summary = "결제 내역 조회", description = "url 변수에 결제 id 설정 & Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "결제 내역 조회 완료", content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 결제 내역", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> findPaymentDetail(@PathVariable("paymentId") Long paymentId,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal) {
        PaymentResponseDto response = paymentService.findPaymentById(userPrincipal.getId(), paymentId);
        return ResponseEntity.ok(response);
    }

}
