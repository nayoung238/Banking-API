package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.transaction.dto.request.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionRecoverService {

    private final AccountService accountService;

    /**
     * 이체 트랜잭션 실패 시
     * 보낸 사람(송금) 계좌에 다시 돈을 입금한다.
     */
    @Transactional
    public void rollbackSenderAccount(TransactionRequest transactionRequest) {
        int retryCount = 3;


        for (int i = 0; i < retryCount; i++) {
            try {
                Account senderAccount = accountService.getAccountWithLock(transactionRequest.getSenderAccountId());
                senderAccount.increaseMoney(transactionRequest.getAmount());
                return; // 성공 시 메서드 종료
            } catch (Exception e) {
                log.error("Rollback attempt {} failed: {}", i + 1, e.getMessage());
            }
        }

        // if 알림 시스템 추가, 롤백 실패 알림
        throw new CustomException(ErrorCode.ROLLBACK_FAILED);
    }

}
