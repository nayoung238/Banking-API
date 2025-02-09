package SN.BANK.account.service;

import SN.BANK.account.dto.request.AccountCreationRequestDto;
import SN.BANK.account.dto.response.AccountResponseDto;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.entity.Account;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.users.entity.Users;
import SN.BANK.users.service.UsersService;
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
    private final UsersService userService;

    @Retryable (
        retryFor = DataIntegrityViolationException.class,
        maxAttempts = 5,
        backoff = @Backoff(delay = 500)
    )
    @Transactional
    public AccountResponseDto createAccount(Long userId, AccountCreationRequestDto request) {
        Users user = userService.findUserEntity(userId);

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
        Users user = userService.findUserEntity(userId);

        List<Account> accounts = accountRepository.findByUser(user);
        return accounts.stream()
                .map(AccountResponseDto::of)
                .toList();
    }
}
