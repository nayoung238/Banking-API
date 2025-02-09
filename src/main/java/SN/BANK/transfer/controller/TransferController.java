package SN.BANK.transfer.controller;

import SN.BANK.transfer.dto.request.TransferDetailsRequestDto;
import SN.BANK.transfer.dto.request.TransferRequestDto;
import SN.BANK.transfer.dto.response.TransferSimpleResponseDto;
import SN.BANK.transfer.dto.response.TransferDetailsResponseDto;
import SN.BANK.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transfer")
@Tag(name = "Transfer", description = "이체 API")
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "이체", description = "바디에 {senderAccountId, receiverAccountId,amount, accountPassword}을 json 형식으로 보내주세요. 세션에 연결되어 있어야합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "송금 성공", content = @Content(schema = @Schema(implementation = TransferDetailsResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌입니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "403", description = "해당 계좌에 대한 접근 권한이 없습니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "비밀번호가 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "잔액이 부족합니다. / 같은 계좌 간 이체는 불가합니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "환율 값은 0보다 커야 합니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    public ResponseEntity<?> transfer(HttpSession session, @RequestBody @Valid TransferRequestDto request) {
        Long userId = (Long) session.getAttribute("user");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transferService.transfer(userId, request));
    }

    @Operation(summary = "계좌의 모든 거래 내역 조회", description = "url 변수에 계좌의 id를 보내주세요. 세션에 연결되어 있어야합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "계좌의 모든 거래 조회 완료", content = @Content(schema = @Schema(implementation = TransferSimpleResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌입니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "403", description = "해당 계좌에 대한 접근 권한이 없습니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/history/{accountId}")
    public ResponseEntity<?> findAllTransferSimple(HttpSession session, @PathVariable("accountId") Long accountId) {
        Long userId = (Long) session.getAttribute("user");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transferService.findAllTransferSimple(userId, accountId));
    }

    @Operation(summary = "거래 상세 내역 조회", description = "바디에 {accountId, transactionId}을 json 형식으로 보내주세요. 세션에 연결되어 있어야합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "거래 상세내역 조회 완료", content = @Content(schema = @Schema(implementation = TransferSimpleResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌입니다. / 존재하지 않는 거래내역입니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "403", description = "해당 계좌에 대한 접근 권한이 없습니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping
    public ResponseEntity<?> findTransferDetails(HttpSession session, @RequestBody @Valid TransferDetailsRequestDto request) {
        Long userId = (Long) session.getAttribute("user");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transferService.findTransferDetails(userId, request));
    }
}
