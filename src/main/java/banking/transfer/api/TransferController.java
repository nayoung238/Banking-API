package banking.transfer.api;

import banking.auth.entity.UserPrincipal;
import banking.transfer.dto.request.TransferDetailsRequestDto;
import banking.transfer.dto.request.TransferRequestDto;
import banking.transfer.dto.response.TransferSimpleResponseDto;
import banking.transfer.dto.response.TransferDetailsResponseDto;
import banking.transfer.service.TransferQueryService;
import banking.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transfer")
@Tag(name = "Transfer", description = "이체 API")
public class TransferController {

    private final TransferService transferService;
    private final TransferQueryService transferQueryService;

    @Operation(summary = "이체", description = "바디에 {senderAccountId, receiverAccountId,amount, accountPassword}을 json 형식 추가 & Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "송금 성공", content = @Content(schema = @Schema(implementation = TransferDetailsResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "403", description = "해당 계좌에 대한 접근 권한 없음", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "비밀번호 불일치", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "잔액 부족 / 같은 계좌 간 이체 불가", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "환율 값 0보다 작은 경우", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    public ResponseEntity<?> transfer(@RequestBody @Valid TransferRequestDto transferRequest,
                                      @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transferService.transfer(userPrincipal.getId(), transferRequest));
    }

    @Operation(summary = "계좌의 모든 거래 내역 조회", description = "url 변수에 계좌의 id 추가 & Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "계좌의 모든 거래 조회 완료", content = @Content(schema = @Schema(implementation = TransferSimpleResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "403", description = "해당 계좌에 대한 접근 권한 없음", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/history/{accountId}")
    public ResponseEntity<?> findAllTransferSimple(@PathVariable("accountId") Long accountId,
                                                   @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transferQueryService.findAllTransferSimple(userPrincipal.getId(), accountId));
    }

    @Operation(summary = "거래 상세 내역 조회", description = "바디에 {accountId, transactionId}을 json 형식 추가 & Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "거래 상세 내역 조회 완료", content = @Content(schema = @Schema(implementation = TransferSimpleResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌 / 존재하지 않는 거래내역", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "403", description = "해당 계좌에 대한 접근 권한 없음", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping
    public ResponseEntity<?> findTransferDetails(@RequestBody @Valid TransferDetailsRequestDto request,
                                                 @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transferQueryService.findTransferDetails(userPrincipal.getId(), request));
    }
}
