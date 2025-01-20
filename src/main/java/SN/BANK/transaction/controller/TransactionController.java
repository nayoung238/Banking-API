package SN.BANK.transaction.controller;

import SN.BANK.transaction.dto.request.TransactionFindDetailRequest;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionFindDetailResponse;
import SN.BANK.transaction.dto.response.TransactionFindResponse;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/transfer/history/{accountId}")
    public ResponseEntity<List<TransactionFindResponse>> findAllTransaction(HttpSession session,
                                                                            @PathVariable Long accountId) {

        Long userId = (Long) session.getAttribute("user");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transactionService.findAllTransaction(userId, accountId));
    }

    @GetMapping("/transfer/history/detail")
    public ResponseEntity<TransactionFindDetailResponse> findTransaction(
            HttpSession session,
            @RequestBody @Valid TransactionFindDetailRequest request) {

        Long userId = (Long) session.getAttribute("user");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transactionService.findTransaction(userId, request));
    }


}
