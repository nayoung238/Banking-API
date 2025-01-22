package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.transaction.dto.request.TransactionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final AccountService accountService;

    /**
     * 1. 받는(수취) 사람 계좌 조회 (수취 계좌 Lock 획득)
     * 1-1. 받는 사람 돈 증가
     */
    public Account receiveFrom(TransactionRequest transactionRequest, BigDecimal convertedAmount) {
            Account receiverAccount = accountService.getAccountWithLock(transactionRequest.getReceiverAccountId());
            receiverAccount.increaseMoney(convertedAmount);
            return receiverAccount;
    }

    /**
     * 결제 시에만 사용
     * @param receiverAccount
     * @param convertedAmount
     * @return
     */
    public Account receiveFrom(Account receiverAccount, BigDecimal convertedAmount) {
        Account receiverAccountWithLock = accountService.getAccountWithLock(receiverAccount.getId());
        receiverAccountWithLock.increaseMoney(convertedAmount);
        return receiverAccountWithLock;
    }

}
