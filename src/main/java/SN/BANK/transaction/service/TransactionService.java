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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final UsersService usersService;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
//    private final ExchangeRateService exchangeRateService;

    /**
     * 이체 기능
     */
    @Transactional
    public TransactionResponse createTransaction(Long userId, TransactionRequest transactionRequest) {

        BigDecimal amount = transactionRequest.getAmount();

        // 1. 계좌 검증
        Account senderAccount = validateSenderAccount(userId, transactionRequest);
        Account receiverAccount = validateReceiverAccount(transactionRequest, senderAccount);

        // 2. 환율 계산 (외부 API 이용)
        BigDecimal exchangeRate = BigDecimal.ONE;
        BigDecimal convertedAmount = amount;

        // 통화가 다른 경우에만 변경
        if (!senderAccount.getCurrency().equals(receiverAccount.getCurrency())) {
            /*
            from 계좌의 통화를 기준으로 (1) to 계좌의 통화 환율을 반환한다.
            ex) from 통화: KRW, to 통화: USD
            exchangeRate = 0.00068; -> 1원 당 0.00068 달러 의미
            즉, amount 가 10,000(won) 이면 convertedAmount 는 6.80($)가 됨
             */
//            exchangeRate = exchangeRateService.getExchangeRate(senderAccount.getCurrency(), receiverAccount.getCurrency());
//            convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP); // HALF_UP: 반올림
        }

        // 3. 잔액 업데이트
        updateAccountBalance(senderAccount, receiverAccount, amount, convertedAmount);

        // 4. 거래 내역 생성
        // 4-1. 송신(주체) 계좌 거래 내역 생성
        // 4-2. 수신 계좌 거래 내역 생성
        LocalDateTime transactedAt = LocalDateTime.now();
        String txGroupId = generateTransactionGroupId();

        TransactionEntity senderTx = getSenderTransactionEntity(senderAccount, receiverAccount, transactedAt,
                amount, exchangeRate, txGroupId);

        TransactionEntity receiverTx = getReceiverTransactionEntity(senderAccount, receiverAccount, transactedAt,
                convertedAmount, exchangeRate, txGroupId);

        return new TransactionResponse(senderTx,
                senderAccount.getUser().getName(),
                receiverAccount.getUser().getName());
    }

    /**
     * 결제 시
     * 사용되는 거래내역 생성 메서드
     */
    @Transactional
    public void createTransactionForPayment(Account senderAccount, Account receiverAccount,
                                                           BigDecimal amount, BigDecimal exchangeRate) {

        BigDecimal convertedAmount = amount;

        // 같은 통화가 아닌 경우
        if (!exchangeRate.equals(BigDecimal.ONE)) {
            convertedAmount = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP); // HALF_UP: 반올림
        }

        updateAccountBalance(senderAccount, receiverAccount, amount, convertedAmount);

        // 거래내역 생성
        LocalDateTime transactedAt = LocalDateTime.now();
        String txGroupId = generateTransactionGroupId();

        getSenderTransactionEntity(senderAccount, receiverAccount, transactedAt,
                amount, exchangeRate, txGroupId);

        getReceiverTransactionEntity(senderAccount, receiverAccount, transactedAt,
                convertedAmount, exchangeRate, txGroupId);
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
    private Account validateSenderAccount(Long userId, TransactionRequest transactionRequest) {

        Account senderAccount = accountService.findValidAccount(transactionRequest.getSenderAccountId());

        accountService.validAccountOwner(senderAccount, userId);

        if (!senderAccount.getPassword().equals(transactionRequest.getAccountPassword())) { // 복호화 및 규칙 추가 예정
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        if (!isGreaterThanAmount(senderAccount, transactionRequest.getAmount())) {
            throw new CustomException(ErrorCode.INSUFFICIENT_MONEY);
        }

        return senderAccount;
    }

    /**
     * 1. to 계좌 검증
     * 1-1. 유효한 계좌인지
     * 1-2. to 계좌가 from 계좌와 같은지
     *
     * @param transactionRequest
     * @param senderAccount
     * @return
     */
    private Account validateReceiverAccount(TransactionRequest transactionRequest, Account senderAccount) {
        Account receiverAccount = accountService.findValidAccount(transactionRequest.getReceiverAccountId());

        if (receiverAccount.equals(senderAccount)) {
            throw new CustomException(ErrorCode.INVALID_TRANSFER);
        }

        return receiverAccount;
    }

    private void updateAccountBalance(Account senderAccount, Account receiverAccount,
                                      BigDecimal amount, BigDecimal convertedAmount) {
        BigDecimal restMoney = senderAccount.getMoney().subtract(amount);
        senderAccount.changeMoney(restMoney);

        BigDecimal addedMoney = receiverAccount.getMoney().add(convertedAmount);
        receiverAccount.changeMoney(addedMoney);
    }

    private TransactionEntity getSenderTransactionEntity(Account senderAccount, Account receiverAccount,
                                                         LocalDateTime transactedAt, BigDecimal amount,
                                                         BigDecimal exchangeRate, String txGroupId) {

        TransactionEntity senderTx = TransactionEntity.builder()
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .type(TransactionType.WITHDRAWAL) // 출금
                .transactedAt(transactedAt)
                .amount(amount)
                .currency(senderAccount.getCurrency())
                .exchangeRate(exchangeRate)
                .balance(senderAccount.getMoney())
                .groupId(txGroupId)
                .build();

        return transactionRepository.save(senderTx);
    }

    private TransactionEntity getReceiverTransactionEntity(Account senderAccount, Account receiverAccount,
                                                           LocalDateTime transactedAt, BigDecimal amount,
                                                           BigDecimal exchangeRate, String txGroupId) {
        TransactionEntity receiverTx = TransactionEntity.builder()
                .senderAccountId(senderAccount.getId())
                .receiverAccountId(receiverAccount.getId())
                .type(TransactionType.DEPOSIT) // 출금
                .transactedAt(transactedAt)
                .amount(amount)
                .currency(receiverAccount.getCurrency())
                .exchangeRate(exchangeRate)
                .balance(receiverAccount.getMoney())
                .groupId(txGroupId)
                .build();

        return transactionRepository.save(receiverTx);
    }

    public boolean isGreaterThanAmount(Account account, BigDecimal amount) {
        return account.getMoney().compareTo(amount) >= 0;
    }

    private String generateTransactionGroupId() {
        String groupId;

        // UUID가 중복되지 않도록 확인
        do {
            groupId = UUID.randomUUID().toString();
        } while (transactionRepository.existsByGroupId(groupId));

        return groupId;
    }
}
