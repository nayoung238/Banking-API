package SN.BANK.account.conroller;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.account.dto.response.CreateAccountResponse;
import SN.BANK.account.service.AccountService;
import SN.BANK.account.entity.Account;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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

}
