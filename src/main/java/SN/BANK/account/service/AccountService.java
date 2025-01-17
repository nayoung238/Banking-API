package SN.BANK.account.service;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.entity.Account;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.users.entity.Users;
import SN.BANK.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UsersRepository userRepository;

    @Transactional
    public Account createAccount(Long userId, CreateAccountRequest request) {

        // username 을 통한 사용자 조회 및 검증 (+ exception) ✅
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // CreateAccountRequest 데이터 복호화 과정 필요 ❎

        // 계좌 번호 생성 ✅
        String accountNumber = generateAccountNumber();

        // 계좌 별칭 체크 ✅
        String accountName = request.getAccountName() == null ? "SN은행-계좌" : request.getAccountName();

        Account account = Account.builder()
                .user(user)
                .password(request.getPassword())
                .accountNumber(accountNumber)
                .money(BigDecimal.valueOf(0))
                .accountName(accountName)
                .currency(request.getCurrency())
                .build();

        return accountRepository.save(account);
    }

    public Account findAccount(Long userId, Long accountId) {
        // 1. 유효한 사용자인지 검증
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        // 2. 유효한 계좌인지 검증
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // 3. 해당 계좌가 사용자의 계좌인지 검증
        if (!account.getUser().equals(user)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }

        return account;
    }

    public List<Account> findAllAccounts(Long userId) {
        // 1. 유효한 사용자인지 검증
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

        return accountRepository.findByUser(user);
    }

    /**
     * 계좌 번호 생성기
     * Random 방식으로 생성
     *
     * @return accountNumber
     */
    public String generateAccountNumber() {

        // 1. 계좌 번호를 생성하고 AccountRepository 를 통해 고유성을 체크한다.
        //  1-1. 고유하지 않다면, 고유할 때까지 생성기를 돌려서 계좌 번호를 생성한다.

        String accountNumber;
        do {
            accountNumber = generateNumber();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        // 2. 생성된 계좌 번호를 반환한다.
        return accountNumber;
    }

    private String generateNumber() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 14; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }
}
