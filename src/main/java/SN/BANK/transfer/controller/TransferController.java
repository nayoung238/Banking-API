package SN.BANK.transfer.controller;

import SN.BANK.transfer.dto.request.TransferRequest;
import SN.BANK.transfer.dto.response.PaymentListResponse;
import SN.BANK.transfer.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/transfer")
    public ResponseEntity<PaymentListResponse> transfer(@RequestBody @Valid TransferRequest request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transferService.transfer(request));
    }

}
