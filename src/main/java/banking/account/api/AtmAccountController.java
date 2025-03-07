package banking.account.api;

import banking.account.dto.request.DepositRequest;
import banking.account.dto.request.WithdrawalRequest;
import banking.account.dto.response.AccountDetailResponse;
import banking.account.service.AccountBalanceService;
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

@Tag(name="ATM", description = "ATM API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/atm")
public class AtmAccountController {

	private final AccountBalanceService accountBalanceService;

	@Operation(summary = "ATM 입금", description = "바디에 {account number, account password, amount}을 json 형식으로 보내주세요.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "ATM 입금 성공", content = @Content(schema = @Schema(implementation = AccountDetailResponse.class))),
		@ApiResponse(responseCode = "404", description = "존재하지 않는 계좌", content = @Content(schema = @Schema(implementation = String.class))),
		@ApiResponse(responseCode = "401", description = "비밀번호 불일치", content = @Content(schema = @Schema(implementation = String.class)))
	})
	@PostMapping("/deposit")
	public ResponseEntity<?> atmDeposit(@RequestBody @Valid DepositRequest depositRequest) {
		AccountDetailResponse response = accountBalanceService.atmDeposit(depositRequest);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "ATM 출금", description = "바디에 {account number, account password, amount}을 json 형식으로 보내주세요.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "ATM 출금 성공", content = @Content(schema = @Schema(implementation = AccountDetailResponse.class))),
		@ApiResponse(responseCode = "404", description = "존재하지 않는 계좌", content = @Content(schema = @Schema(implementation = String.class))),
		@ApiResponse(responseCode = "401", description = "비밀번호 불일치", content = @Content(schema = @Schema(implementation = String.class))),
		@ApiResponse(responseCode = "400", description = "잔액 부족", content = @Content(schema = @Schema(implementation = String.class)))
	})
	@PostMapping("/withdrawal")
	public ResponseEntity<?> atmWithdrawal(@RequestBody @Valid WithdrawalRequest withdrawalRequest) {
		AccountDetailResponse response = accountBalanceService.atmWithdrawal(withdrawalRequest);
		return ResponseEntity.ok(response);
	}
}
