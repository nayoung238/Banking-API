package SN.BANK.account.conroller;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.account.dto.response.AccountResponse;
import SN.BANK.account.dto.response.CreateAccountResponse;
import SN.BANK.account.service.AccountService;
import SN.BANK.account.entity.Account;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/accounts")
    public ResponseEntity<CreateAccountResponse> createAccount(HttpSession session,
                                                               @RequestBody @Valid CreateAccountRequest request) {
        Long userId = (Long) session.getAttribute("user");
        Account account = accountService.createAccount(userId, request);
        CreateAccountResponse createAccountResponse = CreateAccountResponse.of(account);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createAccountResponse);
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> findAllAccounts(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");

        List<Account> accounts = accountService.findAllAccounts(userId);
        List<AccountResponse> response = accounts.stream()
                .map(AccountResponse::of)
                .toList();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> findAccount(HttpSession session,
                                                       @PathVariable(name = "id") Long accountId) {
        Long userId = (Long) session.getAttribute("user");

        Account account = accountService.findAccount(userId, accountId);
        AccountResponse response = AccountResponse.of(account);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

}
