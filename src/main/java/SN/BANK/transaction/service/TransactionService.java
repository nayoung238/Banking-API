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

    private final WithdrawService withdrawService;
    private final DepositService depositService;
    private final TransactionCreateService createService;

    /**
     * 이체 기능
     */
    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest transactionRequest) {

        BigDecimal amount = transactionRequest.getAmount();

        // 환율 계산
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = amount;

        // 1. 보낸(송금) 사람 계좌 조회 (송금 계좌 Lock 획득)
        // 1-1. 보낸 사람 돈 감소
        Account senderAccount = withdrawService.sendTo(userId, transactionRequest);

        // 2. 받는(수취) 사람 계좌 조회 (수취 계좌 Lock 획득)
        // 2-1. 받는 사람 돈 증가
        // 2-1-1. 성공 시, 거래 내역(transaction) 생성
        // 2-1-2. 실패 시, 보낸(송금) 사람 계좌에 다시 감소된 돈 만큼 증가
        Account receiverAccount = depositService.receiveFrom(transactionRequest, convertedAmount);

        return createService.createTransactionRecords(senderAccount, receiverAccount,
                exchangeRate, amount, convertedAmount);
    }

    /**
     * 결제 시
     * 사용되는 거래내역 생성 메서드
     */
    public void createTransactionForPayment(Account senderAccount, Account receiverAccount,
                                            BigDecimal amount, BigDecimal exchangeRate) {

        BigDecimal convertedAmount = amount;

        // 같은 통화가 아닌 경우
//        if (!exchangeRate.equals(BigDecimal.ONE)) {
//            convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP); // HALF_UP: 반올림
//        }

        // 송신 계좌 먼저 출금
        Account updatedSenderAccount = withdrawService.sendTo(senderAccount, amount);
        // 수신 계좌 입금
        Account updatedReceiverAccount = depositService.receiveFrom(receiverAccount, convertedAmount);

        // 거래내역 생성
        createService.createTransactionRecords(updatedSenderAccount, updatedReceiverAccount,
                exchangeRate, amount, convertedAmount);
    }

    /**
     * 전체 이체 내역 조회
     */
    public List<TransactionFindResponse> findAllTransaction(Long userId, Long accountId) {

        // 유효한 사용자인지 검증
        Users user = usersService.validateUser(userId);

        // 유효한 계좌인지 검증
        Account account = accountService.getAccount(accountId);

        // 해당 계좌가 사용자의 계좌인지 검증
        accountService.validAccountOwner(user.getId(), account);

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
        Account account = accountService.getAccount(request.getAccountId());

        // 해당 계좌가 사용자의 계좌인지 검증
        accountService.validAccountOwner(user.getId(), account);

        // 유효한 거래 내역인지 검증
        TransactionEntity tx = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSACTION));

        // 해당 거래 내역(tx)의 sender 가 사용자 account 면, receiver 는 상대방.
        // 반대로 receiver 가 사용자 account 면, sender 가 상대방.
        Account receiverAccount;

        if (tx.getSenderAccountId().equals(account.getId())) {
            receiverAccount = accountService.getAccount(tx.getReceiverAccountId());
        } else {
            receiverAccount = accountService.getAccount(tx.getSenderAccountId());
        }

        String othersAccountNumber = receiverAccount.getAccountNumber();

        return new TransactionFindDetailResponse(tx, receiverAccount.getUser().getName(), othersAccountNumber);
    }

    public boolean isGreaterThanAmount(Account account, BigDecimal amount) {
        return account.getMoney().compareTo(amount) >= 0;
    }
}
