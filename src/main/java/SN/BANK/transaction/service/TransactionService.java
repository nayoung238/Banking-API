package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.transaction.dto.request.TransactionFindDetailRequest;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionFindDetailResponse;
import SN.BANK.transaction.dto.response.TransactionFindResponse;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import SN.BANK.transaction.repository.TransactionRepository;
import SN.BANK.users.entity.Users;
import SN.BANK.users.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UsersService usersService;
    private final AccountService accountService;
    private final AccountBalanceService accountBalanceService;
    private final AccountValidationService accountValidationService;
    private final TransactionHistoryService transactionHistoryService;

    /**
     * 이체 기능
     */
    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest transactionRequest) {

        BigDecimal amount = transactionRequest.getAmount();

        // 환율 계산
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = amount;

        // 1. 계좌 검증 및 락 설정
        Account senderAccount = accountValidationService.validateSenderAccount(userId, transactionRequest);
        Account receiverAccount = accountValidationService.validateReceiverAccount(senderAccount, transactionRequest);

        // 2. 잔액 갱신
        accountBalanceService.updateAccountBalances(senderAccount, receiverAccount, amount);

        // 3. 거래 내역 생성
        return transactionHistoryService.createTransactionRecords(senderAccount, receiverAccount,
                exchangeRate, amount, convertedAmount);
    }

    /**
     * 전체 이체 내역 조회
     */
    public List<TransactionFindResponse> findAllTransaction(Long userId, Long accountId) {

        // 유효한 사용자인지 검증
        Users user = usersService.validateUser(userId);

        // 유효한 계좌인지 검증
        Account account = accountService.findValidAccount(accountId);

        // 해당 계좌가 사용자의 계좌인지 검증
        accountService.validAccountOwner(account, user.getId());

        List<TransactionEntity> txFindResponse = new ArrayList<>();
        txFindResponse.addAll(transactionRepository.findBySenderAccountIdAndType(accountId, TransactionType.WITHDRAWAL));
        txFindResponse.addAll(transactionRepository.findByReceiverAccountIdAndType(accountId, TransactionType.DEPOSIT));

        return txFindResponse.stream()
                .map(tx -> new TransactionFindResponse(tx, account.getUser().getName(), account.getAccountNumber()))
                .toList();
    }

    /**
     * 이체 내역 단건 조회
     */
    public TransactionFindDetailResponse findTransaction(Long userId, TransactionFindDetailRequest request) {

        // 유효한 사용자인지 검증
        Users user = usersService.validateUser(userId);

        // 유효한 계좌인지 검증
        Account account = accountService.findValidAccount(request.getAccountId());

        // 해당 계좌가 사용자의 계좌인지 검증
        accountService.validAccountOwner(account, user.getId());

        // 유효한 거래 내역인지 검증
        TransactionEntity tx = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSACTION));

        // 해당 거래 내역(tx)의 sender 가 사용자 account 면, receiver 는 상대방.
        // 반대로 receiver 가 사용자 account 면, sender 가 상대방.
        Account receiverAccount;

        if (tx.getSenderAccountId().equals(account.getId())) {
            receiverAccount = accountService.findValidAccount(tx.getReceiverAccountId());
        } else {
            receiverAccount = accountService.findValidAccount(tx.getSenderAccountId());
        }

        String othersAccountNumber = receiverAccount.getAccountNumber();

        return new TransactionFindDetailResponse(tx, receiverAccount.getUser().getName(), othersAccountNumber);
    }

    public boolean isGreaterThanAmount(Account account, BigDecimal amount) {
        return account.getMoney().compareTo(amount) >= 0;
    }
}
