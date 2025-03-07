package banking.account.api;

import banking.account.dto.request.AccountCreationRequestDto;
import banking.account.dto.response.AccountResponseDto;
import banking.account.service.AccountService;
import banking.auth.entity.UserPrincipal;
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

@Tag(name = "Accounts", description = "계좌 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 생성", description = "바디에 {accountName, currency, password} json 형식으로 추가 & Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "계좌 생성 성공", content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "존재하지 않는 유저", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody @Valid AccountCreationRequestDto request,
                                           @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(accountService.createAccount(userPrincipal.getId(), request));
    }

    @Operation(summary = "사용자의 단일 계좌 조회", description = "url 변수에 계좌 ID 설정 & Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용자의 단일 계좌 조회 성공", content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> findAccount(@PathVariable(name = "id") Long accountId,
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(accountService.findAccount(userPrincipal.getId(), accountId));
    }

    @Operation(summary = "사용자의 모든 계좌 조회", description = "Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용자의 모든 계좌 조회 성공", content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping
    public ResponseEntity<?> findAllAccounts(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(accountService.findAllAccounts(userPrincipal.getId()));
    }
}
