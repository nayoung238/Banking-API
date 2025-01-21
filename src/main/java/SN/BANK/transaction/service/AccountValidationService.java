package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.transaction.dto.request.TransactionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class AccountValidationService {

    private final AccountService accountService;

    /**
     * 1. from 계좌 검증
     * 1-1. 유효한 계좌인지
     * 1-2. 해당 계좌가 현재 로그인한 유저(본인)의 계좌가 맞는지
     * 1-3. 계좌 비밀번호가 맞는지 (+ 데이터 암복호화 기능 추가해야 함)
     * 1-4. 잔액이 보내려는 금액보다 크거나 같은지
     *
     * @param userId
     * @param transactionRequest
     * @return
     */
    public Account validateSenderAccount(Long userId, TransactionRequest transactionRequest) {
        Account senderAccount = accountService.getAccountWithLock(transactionRequest.getSenderAccountId());
        accountService.validAccountOwner(senderAccount, userId);
        if (!senderAccount.getPassword().equals(transactionRequest.getAccountPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
        return senderAccount;
    }

    /**
     * 1. to 계좌 검증
     * 1-1. 유효한 계좌인지
     * 1-2. to 계좌가 from 계좌와 같은지
     *
     * @param senderAccount
     * @param transactionRequest
     * @return
     */
    public Account validateReceiverAccount(Account senderAccount, TransactionRequest transactionRequest) {
        Account receiverAccount = accountService.getAccountWithLock(transactionRequest.getReceiverAccountId());
        if (receiverAccount.getId().equals(senderAccount.getId())) {
            throw new CustomException(ErrorCode.INVALID_TRANSFER);
        }
        return receiverAccount;
    }
}


