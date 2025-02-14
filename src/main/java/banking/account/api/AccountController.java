package banking.account.api;

import banking.account.dto.request.AccountCreationRequestDto;
import banking.account.dto.response.AccountResponseDto;
import banking.account.service.AccountService;
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

import java.util.List;

@Tag(name="Accounts", description = "계좌 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 생성", description = "바디에 {accountName, currency, password}을 json 형식으로 보내주세요. 세션에 연결되어 있어야 합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "계좌 생성 성공", content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "존재하지 않는 유저입니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    public ResponseEntity<?> createAccount(HttpSession session, @RequestBody @Valid AccountCreationRequestDto request) {
        Long userId = (Long) session.getAttribute("user");
        AccountResponseDto response = accountService.createAccount(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "사용자의 단일 계좌 조회", description = "url 변수에 계좌의 id를 보내주세요. 세션에 연결되어 있어야 합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용자의 단일 계좌 조회 성공", content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 계좌입니다.", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 유저입니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> findAccount(HttpSession session, @PathVariable(name = "id") Long accountId) {
        Long userId = (Long) session.getAttribute("user");
        AccountResponseDto response = accountService.findAccount(userId, accountId);
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response);
    }

    @Operation(summary = "사용자의 모든 계좌 조회", description = "세션에 연결되어 있어야 합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "사용자의 모든 계좌 조회 성공", content = @Content(schema = @Schema(implementation = AccountResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 유저입니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping
    public ResponseEntity<?> findAllAccounts(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        List<AccountResponseDto> response = accountService.findAllAccounts(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
