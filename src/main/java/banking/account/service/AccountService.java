package banking.account.service;

import banking.account.dto.request.AccountCreationRequestDto;
import banking.account.dto.response.AccountResponseDto;
import banking.account.dto.response.AccountPublicInfoDto;
import banking.account.repository.AccountRepository;
import banking.account.entity.Account;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.user.entity.User;
import banking.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    @Retryable (
        retryFor = DataIntegrityViolationException.class,
        maxAttempts = 5,
        backoff = @Backoff(delay = 500)
    )
    @Transactional
    public AccountResponseDto createAccount(Long userId, AccountCreationRequestDto request) {
        User user = userService.findUserEntity(userId);
        String accountName = request.accountName() != null ? request.accountName() : "은행 계좌명";

        Account account = Account.builder()
            .user(user)
            .accountNumber(generateUniqueAccountNumber())
            .password(request.password())
            .balance(BigDecimal.valueOf(0, 2))
            .accountName(accountName)
            .currency(request.currency())
            .build();

        accountRepository.save(account);
        return AccountResponseDto.of(account);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.valueOf(ThreadLocalRandom.current().nextLong(10000000000000L, 99999999999999L));
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber.substring(0, 7) + "-" + accountNumber.substring(7);
    }

    public AccountResponseDto findAccount(Long userId, Long accountId) {
        Account account = findAccountById(accountId);
        verifyAccountOwner(userId, account);
        return AccountResponseDto.of(account);
    }

    private Account findAccountById(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
    }

    private void verifyAccountOwner(Long userId, Account account) {
        if(!userService.isExistUser(userId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }

        if (!account.isAccountOwner(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }
    }

    public List<AccountResponseDto> findAllAccounts(Long userId) {
        User user = userService.findUserEntity(userId);

        List<Account> accounts = accountRepository.findByUser(user);
        return accounts.stream()
                .map(AccountResponseDto::of)
                .toList();
    }

    public AccountPublicInfoDto findAccountPublicInfo(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return AccountPublicInfoDto.of(account);
    }

    public AccountPublicInfoDto findAccountPublicInfo(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return AccountPublicInfoDto.of(account);
    }

    public Account findAuthorizedAccountWithLock(Long requesterId, String accountNumber, String password) {
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

        if(requesterId != null && !account.getUser().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }

        if(!account.getPassword().equals(password)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        return account;
    }

    public Account findAuthorizedAccountWithLock(Long requesterId, Long accountId, String password) {
        Account account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_ACCOUNT));

        if(requesterId != null && !account.getUser().getId().equals(requesterId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }

        if(!account.getPassword().equals(password)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        return account;
    }

    // TODO: 상대 계좌 접근 권한
    public Account findAccountWithLock(Long accountId) {
        return accountRepository.findByIdWithLock(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
    }

    public void verifyAccountOwner(Long accountId, Long userId) {
        if(!accountRepository.existsByAccountIdAndUserId(accountId, userId))
            throw new CustomException(ErrorCode.NOT_FOUND_ACCOUNT);
    }
}
