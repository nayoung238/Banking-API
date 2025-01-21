package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.transaction.dto.request.TransactionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final AccountService accountService;

    /**
     * 1. 보낸(송금) 사람 계좌 조회 (송금 계좌 Lock 획득)
     * 1-1. 보낸 사람 돈 감소
     * @param userId
     * @param transactionRequest
     * @return
     */
    @Transactional
    public Account sendTo(Long userId, TransactionRequest transactionRequest) {

        // 송금 계좌 및 사용자 검증
        Account senderAccount = accountService.getAccountWithLock(transactionRequest.getSenderAccountId());
        accountService.validAccountOwner(userId, senderAccount);
        accountService.validAccountBalance(senderAccount, transactionRequest.getAmount());

        senderAccount.decreaseMoney(transactionRequest.getAmount());
        return senderAccount;
    }
}
