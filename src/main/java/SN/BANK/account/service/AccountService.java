package SN.BANK.account.service;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.account.dto.response.AccountResponse;
import SN.BANK.account.dto.response.CreateAccountResponse;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.entity.Account;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.users.entity.Users;
import SN.BANK.users.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UsersService usersService;

    @Transactional
    public CreateAccountResponse createAccount(Long userId, CreateAccountRequest request) {

        // username 을 통한 사용자 조회 및 검증 (+ exception) ✅
        Users user = usersService.validateUser(userId);

        // CreateAccountRequest 데이터 복호화 과정 필요 ❎

        // 계좌 번호 생성 ✅
        String accountNumber = generateAccountNumber();

        // 계좌 별칭 체크 ✅
        String accountName = request.accountName() == null ? "SN은행-계좌" : request.accountName();

        Account account = Account.builder()
                .user(user)
                .password(request.password())
                .accountNumber(accountNumber)
                .money(BigDecimal.valueOf(0, 2))
                .accountName(accountName)
                .currency(request.currency())
                .build();

        Account savedAccount = accountRepository.save(account);
        return CreateAccountResponse.of(savedAccount);
    }

    public AccountResponse findAccount(Long userId, Long accountId) {
        // 유효한 계좌인지 검증 (+ 사용자 유효성, 계좌-사용자 소유 검증)
        Account account = getAccount(accountId);
        validateAccountOwner(userId, account);
        return AccountResponse.of(account);
    }

    public List<AccountResponse> findAllAccounts(Long userId) {
        // 1. 유효한 사용자인지 검증
        Users user = usersService.validateUser(userId);

        List<Account> accounts = accountRepository.findByUser(user);
        return accounts.stream()
                .map(AccountResponse::of)
                .toList();
    }

    /**
     * 계좌 번호 생성기
     * UUID 방식으로 생성
     *
     * @return accountNumber
     */
    public String generateAccountNumber() {

        // 1. 계좌 번호를 생성하고 AccountRepository 를 통해 고유성을 체크한다.
        //  1-1. 고유하지 않다면, 고유할 때까지 생성기를 돌려서 계좌 번호를 생성한다.
        String accountNumber;

        do {
            accountNumber = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        } while(accountNumber.length() < 14);

        accountNumber = accountNumber.substring(0, 14);

        // 2. 생성된 계좌 번호를 반환한다.
        return accountNumber;
    }

    /**
     * Lock 사용
     * @param accountId
     * @return
     */
    public Account getAccountWithLock(Long accountId) {
        return accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
    }

    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
    }

    public void validateAccountOwner(Long userId, Account account) {
        // 1. 유효한 사용자인지 검증
        usersService.validateUser(userId);

        // 2. 계좌가 사용자의 것인지 검증
        if (!account.isAccountOwner(userId))
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
    }

    public void validateAccountBalance(Account account, BigDecimal amount) {
        // balance < amount, throw error
        if (account.isGreaterThanBalance(amount)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    public void validateAccountPassword(Account account, String password) {
        // balance < amount, throw error
        if (!account.isCorrectPassword(password)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }

    public void validateNotSelfTransfer(Account senderAccount, Account receiverAccount) {
        if (senderAccount.getId().equals(receiverAccount.getId())) {
            throw new CustomException(ErrorCode.INVALID_TRANSFER);
        }
    }
}
