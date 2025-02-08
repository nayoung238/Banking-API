package SN.BANK.transfer.service;

import SN.BANK.account.entity.Account;
import SN.BANK.account.enums.Currency;
import SN.BANK.account.service.AccountService;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.exchangeRate.ExchangeRateService;
import SN.BANK.transfer.dto.request.TransferFindDetailRequest;
import SN.BANK.transfer.dto.request.TransferRequest;
import SN.BANK.transfer.dto.response.TransferAccountsResponse;
import SN.BANK.transfer.dto.response.TransferFindDetailResponse;
import SN.BANK.transfer.dto.response.TransferFindResponse;
import SN.BANK.transfer.dto.response.TransferResponse;
import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.enums.TransferType;
import SN.BANK.transfer.repository.TransferRepository;
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
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountService accountService;
    private final ExchangeRateService exchangeRateService;

    /**
     * 이체 기능
     */
    @Transactional
    public TransferResponse createTransfer(Long userId, TransferRequest transferRequest) {
        // 1. 보내는(송금) 사람, 받는(수취) 사람 조회
        // 1-1. id 가 더 큰 계좌를 먼저 Lock
        TransferAccountsResponse transferAccounts = getTransferAccounts(transferRequest);
        Account senderAccount = transferAccounts.getSenderAccount();
        Account receiverAccount = transferAccounts.getReceiverAccount();

        // 2. 송금, 수취 계좌의 통화 데이터를 통해 환율 가져오기
        BigDecimal amount = transferRequest.amount(); // (수취 계좌 통화 기준 금액)
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
                senderAccount.getCurrency(), receiverAccount.getCurrency()
        );

        BigDecimal convertedAmount = getExchangeAmount(exchangeRate, amount);

        // 3. 계좌 검증
        validateTransferAccounts(userId,
                senderAccount, receiverAccount,
                convertedAmount, transferRequest.accountPassword());

        // 4. 보낸 사람 돈 감소, 받는 사람 돈 증가
        updateAccountBalance(senderAccount, convertedAmount, receiverAccount, amount);

        return createTransferRecords(senderAccount, receiverAccount,
                exchangeRate, convertedAmount, amount);
    }

    /**
     * 결제 시
     * 사용되는 거래내역 생성 메서드
     * 계좌 검증 부분 빠짐(계좌 검증 부분이 결제 도메인에서 진행됨)
     */
    public void createTransferForPayment(Account senderAccount, Account receiverAccount,
                                            BigDecimal amount, BigDecimal exchangeRate) {

        // 환전
        BigDecimal convertedAmount = getExchangeAmount(exchangeRate, amount);

        // 보낸 사람 돈 감소, 받는 사람 돈 증가
        updateAccountBalance(senderAccount, convertedAmount, receiverAccount, amount);

        // 거래내역 생성
        createTransferRecords(senderAccount, receiverAccount,
                exchangeRate, amount, convertedAmount);
    }

    /**
     * @param txRequest
     * @return
     */
    private TransferAccountsResponse getTransferAccounts(TransferRequest txRequest) {
        Long firstAccountId = Math.max(txRequest.receiverAccountId(), txRequest.senderAccountId());
        Long secondAccountId = Math.min(txRequest.receiverAccountId(), txRequest.senderAccountId());

        Account firstAccount = accountService.getAccountWithLock(firstAccountId);
        Account secondAccount = accountService.getAccountWithLock(secondAccountId);

        Account senderAccount = txRequest.senderAccountId().equals(firstAccount.getId()) ? firstAccount : secondAccount;
        Account receiverAccount = txRequest.receiverAccountId().equals(firstAccount.getId()) ? firstAccount : secondAccount;

        return TransferAccountsResponse.builder()
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .build();
    }

    private void validateTransferAccounts(Long userId,
                                          Account sennderAccount, Account receiverAccount,
                                          BigDecimal amount, String password) {
        accountService.validateAccountOwner(userId, sennderAccount);

        // 사용자 계좌 잔액 검증
        accountService.validateAccountBalance(sennderAccount, amount);

        // 계좌 비밀번호 검증
        accountService.validateAccountPassword(sennderAccount, password);

        // 자기 자신과의 거래인지 검증
        accountService.validateNotSelfTransfer(sennderAccount, receiverAccount);
    }

    private void updateAccountBalance(Account senderAccount, BigDecimal convertedAmount, Account receiverAccount, BigDecimal amount) {
        senderAccount.decreaseMoney(convertedAmount);
        receiverAccount.increaseMoney(amount);
    }

    private BigDecimal getExchangeAmount(BigDecimal exchangeRate, BigDecimal amount) {
        if (exchangeRate == null || exchangeRate.compareTo(BigDecimal.ZERO) < 1) {
            throw new CustomException(ErrorCode.INVALID_EXCHANGE_RATE);
        }
        if (!exchangeRate.equals(BigDecimal.ONE)) {
            return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
        }
        return amount;
    }

    private Transfer buildTransferEntity(Account senderAccount, Account receiverAccount,
                                         TransferType type, LocalDateTime transactedAt,
                                         BigDecimal amount, Currency currency, BigDecimal exchangeRate,
                                         BigDecimal balance, String groupId) {
        return Transfer.builder()
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

    private String generateTransferGroupId() {
        return UUID.randomUUID().toString();
    }

    /**
     * 전체 이체 내역 조회
     */
    public List<TransferFindResponse> findAllTransfer(Long userId, Long accountId) {
        // 유효한 계좌인지 검증 (+ 사용자 유효성, 계좌-사용자 소유 검증)
        Account userAccount = accountService.getAccount(accountId);
        accountService.validateAccountOwner(userId, userAccount);

        Users user = userAccount.getUser();

        List<Transfer> txFindResponse = new ArrayList<>();
        txFindResponse.addAll(transferRepository.findBySenderAccountIdAndType(accountId, TransferType.WITHDRAWAL));
        txFindResponse.addAll(transferRepository.findByReceiverAccountIdAndType(accountId, TransferType.DEPOSIT));

        return txFindResponse.stream()
                .map(tx -> new TransferFindResponse(tx, user.getName(), userAccount.getAccountNumber()))
                .toList();
    }

    /**
     * 이체 내역 단건 조회
     */
    public TransferFindDetailResponse findTransfer(Long userId, TransferFindDetailRequest request) {
        // 유효한 계좌인지 검증 (+ 사용자 유효성, 계좌-사용자 소유 검증)
        Account userAccount = accountService.getAccount(request.accountId());
        accountService.validateAccountOwner(userId, userAccount);

        // 유효한 거래 내역인지 검증
        Transfer tx = transferRepository.findById(request.transactionId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSACTION));

        Account receiverAccount = getReceiverAccount(userId, tx, userAccount);

        String othersAccountNumber = receiverAccount.getAccountNumber();

        return new TransferFindDetailResponse(tx, receiverAccount.getUser().getName(), othersAccountNumber);
    }

    // 해당 거래 내역(tx)의 sender 가 사용자 account 면, receiver 는 상대방.
    // 반대로 receiver 가 사용자 account 면, sender 가 상대방.
    private Account getReceiverAccount(Long userId, Transfer tx, Account userAccount) {
        Account receiverAccount;

        if (tx.getSenderAccountId().equals(userAccount.getId())) {
            receiverAccount = accountService.getAccount(tx.getReceiverAccountId());
        } else {
            receiverAccount = accountService.getAccount(tx.getSenderAccountId());
        }
        return receiverAccount;
    }

    // Transaction (거래 내역) 생성 메서드
    public TransferResponse createTransferRecords(Account senderAccount, Account receiverAccount,
                                                  BigDecimal exchangeRate, BigDecimal convertedAmount, BigDecimal amount) {

        LocalDateTime transactedAt = LocalDateTime.now();
        String txGroupId = generateTransferGroupId();

        Transfer senderTx = buildTransferEntity(senderAccount, receiverAccount, TransferType.WITHDRAWAL, transactedAt,
                convertedAmount, senderAccount.getCurrency(), exchangeRate, senderAccount.getMoney(), txGroupId);

        Transfer receiverTx = buildTransferEntity(senderAccount, receiverAccount, TransferType.DEPOSIT, transactedAt,
                amount, receiverAccount.getCurrency(), exchangeRate, receiverAccount.getMoney(), txGroupId);

        transferRepository.saveAll(List.of(senderTx, receiverTx));

        return new TransferResponse(senderTx, senderAccount.getUser().getName(), receiverAccount.getUser().getName());
    }


    @Transactional
    public TransferResponse createTransferNonLock(Long userId, TransferRequest transferRequest) {
        // 1. 보내는(송금) 사람, 받는(수취) 사람 조회
        // 1-1. id 가 더 큰 계좌를 먼저 Lock
        TransferAccountsResponse transferAccounts = getTransferAccountsNonLock(transferRequest);
        Account senderAccount = transferAccounts.getSenderAccount();
        Account receiverAccount = transferAccounts.getReceiverAccount();

        // 2. 송금, 수취 계좌의 통화 데이터를 통해 환율 가져오기
        BigDecimal amount = transferRequest.amount(); // (수취 계좌 통화 기준 금액)
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
                senderAccount.getCurrency(), receiverAccount.getCurrency()
        );

        BigDecimal convertedAmount = getExchangeAmount(exchangeRate, amount);

        // 3. 계좌 검증
        validateTransferAccounts(userId,
                senderAccount, receiverAccount,
                convertedAmount, transferRequest.accountPassword());

        // 4. 보낸 사람 돈 감소, 받는 사람 돈 증가
        updateAccountBalance(senderAccount, convertedAmount, receiverAccount, amount);

        return createTransferRecords(senderAccount, receiverAccount,
                exchangeRate, convertedAmount, amount);
    }

    private TransferAccountsResponse getTransferAccountsNonLock(TransferRequest txRequest) {
        Account senderAccount = accountService.getAccount(txRequest.senderAccountId());
        Account receiverAccount = accountService.getAccount(txRequest.receiverAccountId());
        return TransferAccountsResponse.builder()
                .senderAccount(senderAccount)
                .receiverAccount(receiverAccount)
                .build();
    }
}
