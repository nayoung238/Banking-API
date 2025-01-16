package SN.BANK.transaction.controller;

import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.service.TransactionService;
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
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(HttpSession session,
                                                        @RequestBody @Valid TransactionRequest request) {

        Long userId = (Long) session.getAttribute("user");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transactionService.createTransaction(userId, request));
    }


}
