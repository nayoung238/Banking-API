package SN.BANK.transaction.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.exchangeRate.ExchangeRateService;
import SN.BANK.transaction.dto.request.TransactionFindDetailRequest;
import SN.BANK.transaction.dto.request.TransactionRequest;
import SN.BANK.transaction.dto.response.TransactionAccountsResponse;
import SN.BANK.transaction.dto.response.TransactionFindDetailResponse;
import SN.BANK.transaction.dto.response.TransactionFindResponse;
import SN.BANK.transaction.dto.response.TransactionResponse;
import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import SN.BANK.transaction.repository.TransactionRepository;
import SN.BANK.users.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final ExchangeRateService exchangeRateService;

    /**
     * 이체 기능
     */
    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest transactionRequest) {

        // 1. 보내는(송금) 사람, 받는(수취) 사람 검증 및 조회
        TransactionAccountsResponse txAccounts = getUserAccount(userId, transactionRequest);

        Account senderAccount = txAccounts.getSenderAccount();
        Account receiverAccount = txAccounts.getReceiverAccount();

        // 2. 송금, 수취 계좌의 통화 데이터를 통해 환율 가져오기
        BigDecimal amount = transactionRequest.getAmount();
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
                senderAccount.getCurrency(), receiverAccount.getCurrency()
        );

        BigDecimal convertedAmount = getExchangeAmount(exchangeRate, amount);

        // 3. 보낸 사람 돈 감소, 받는 사람 돈 증가
        updateAccountBalance(senderAccount, amount, receiverAccount, convertedAmount);

        return createTransactionRecords(senderAccount, receiverAccount,
                exchangeRate, amount, convertedAmount);
    }

    /**
     * 결제 시
     * 사용되는 거래내역 생성 메서드
     * 계좌 검증 부분 빠짐(계좌 검증 부분이 결제 도메인에서 진행됨)
     */
    public void createTransactionForPayment(Account senderAccount, Account receiverAccount,
                                            BigDecimal amount, BigDecimal exchangeRate) {

        // 환전
        BigDecimal convertedAmount = getExchangeAmount(exchangeRate, amount);

        // 보낸 사람 돈 감소, 받는 사람 돈 증가
        updateAccountBalance(senderAccount, amount, receiverAccount, convertedAmount);

        // 거래내역 생성
        createTransactionRecords(senderAccount, receiverAccount,
                exchangeRate, amount, convertedAmount);
    }

    /**
     * @param userId
     * @param txRequest
     * @return
     */
    private TransactionAccountsResponse getUserAccount(Long userId, TransactionRequest txRequest) {

        // 유효한 계좌인지 검증
        Account userAccount = accountService.getAccountWithLock(txRequest.getSenderAccountId());
        Account receiverAccount = accountService.getAccountWithLock(txRequest.getReceiverAccountId());

        // 사용자 유효성, 계좌-사용자 소유 검증
        accountService.validateAccountOwner(userId, userAccount);

        // 사용자 계좌 잔액 검증
        accountService.validateAccountBalance(userAccount, txRequest.getAmount());

        // 계좌 비밀번호 검증
        accountService.validateAccountPassword(userAccount, txRequest.getAccountPassword());

        // 자기 자신과의 거래인지 검증
        accountService.validateNotSelfTransfer(userAccount, receiverAccount);

        return TransactionAccountsResponse.builder()
                .senderAccount(userAccount)
                .receiverAccount(receiverAccount)
                .build();
    }

    private void updateAccountBalance(Account senderAccount, BigDecimal amount, Account receiverAccount, BigDecimal convertedAmount) {
        senderAccount.decreaseMoney(amount);
        receiverAccount.increaseMoney(convertedAmount);
    }

    private BigDecimal getExchangeAmount(BigDecimal exchangeRate, BigDecimal amount) {
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) < 1) {
            throw new CustomException(ErrorCode.INVALID_EXCHANGE_RATE);
        }
        if (!exchangeRate.equals(BigDecimal.ONE)) {
            return amount.divide(exchangeRate, 2, RoundingMode.HALF_UP);
        }
        return amount;
    }

    private TransactionEntity buildTransactionEntity(Account senderAccount, Account receiverAccount,
                                                     TransactionType type, LocalDateTime transactedAt,
                                                     BigDecimal amount, Currency currency, BigDecimal exchangeRate,
                                                     BigDecimal balance, String groupId) {
        return TransactionEntity.builder()
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .type(type)
                .transactedAt(transactedAt)
                .amount(amount)
                .currency(currency)
                .exchangeRate(exchangeRate)
                .balance(balance)
                .groupId(groupId)
                .build();
    }

    private String generateTransactionGroupId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 전체 이체 내역 조회
     */
    public List<TransactionFindResponse> findAllTransaction(Long userId, Long accountId) {

        // 유효한 계좌인지 검증 (+ 사용자 유효성, 계좌-사용자 소유 검증)
        Account userAccount = accountService.getAccount(accountId);
        accountService.validateAccountOwner(userId, userAccount);

        Users user = userAccount.getUser();

        List<TransactionEntity> txFindResponse = new ArrayList<>();
        txFindResponse.addAll(transactionRepository.findBySenderAccountIdAndType(accountId, TransactionType.WITHDRAWAL));
        txFindResponse.addAll(transactionRepository.findByReceiverAccountIdAndType(accountId, TransactionType.DEPOSIT));

        return txFindResponse.stream()
                .map(tx -> new TransactionFindResponse(tx, user.getName(), userAccount.getAccountNumber()))
                .toList();
    }

    /**
     * 이체 내역 단건 조회
     */
    public TransactionFindDetailResponse findTransaction(Long userId, TransactionFindDetailRequest request) {

        // 유효한 계좌인지 검증 (+ 사용자 유효성, 계좌-사용자 소유 검증)
        Account userAccount = accountService.getAccount(request.getAccountId());
        accountService.validateAccountOwner(userId, userAccount);

        // 유효한 거래 내역인지 검증
        TransactionEntity tx = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSACTION));

        Account receiverAccount = getReceiverAccount(userId, tx, userAccount);

        String othersAccountNumber = receiverAccount.getAccountNumber();

        return new TransactionFindDetailResponse(tx, receiverAccount.getUser().getName(), othersAccountNumber);
    }

    // 해당 거래 내역(tx)의 sender 가 사용자 account 면, receiver 는 상대방.
    // 반대로 receiver 가 사용자 account 면, sender 가 상대방.
    private Account getReceiverAccount(Long userId, TransactionEntity tx, Account userAccount) {
        Account receiverAccount;

        if (tx.getSenderAccountId().equals(userAccount.getId())) {
            receiverAccount = accountService.getAccount(tx.getReceiverAccountId());
        } else {
            receiverAccount = accountService.getAccount(tx.getSenderAccountId());
        }
        return receiverAccount;
    }

    // Transaction (거래 내역) 생성 메서드
    public TransactionResponse createTransactionRecords(Account senderAccount, Account receiverAccount,
                                                        BigDecimal exchangeRate, BigDecimal amount, BigDecimal convertedAmount) {

        LocalDateTime transactedAt = LocalDateTime.now();
        String txGroupId = generateTransactionGroupId();

        TransactionEntity senderTx = buildTransactionEntity(senderAccount, receiverAccount, TransactionType.WITHDRAWAL, transactedAt,
                amount, senderAccount.getCurrency(), exchangeRate, senderAccount.getMoney(), txGroupId);

        TransactionEntity receiverTx = buildTransactionEntity(senderAccount, receiverAccount, TransactionType.DEPOSIT, transactedAt,
                convertedAmount, receiverAccount.getCurrency(), exchangeRate, receiverAccount.getMoney(), txGroupId);

        transactionRepository.saveAll(List.of(senderTx, receiverTx));

        return new TransactionResponse(senderTx, senderAccount.getUser().getName(), receiverAccount.getUser().getName());
    }
}
