package SN.BANK.account.service;

import SN.BANK.account.dto.request.CreateAccountRequest;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.account.entity.Account;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.domain.Users;
import SN.BANK.user.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
                .money(0L)
                .accountName(accountName)
                .currency(request.getCurrency())
                .build();

        return accountRepository.save(account);
    }

    /**
     * 계좌 번호 생성기
     * UUID 방식으로 생성
     *
     * @return accountNumber
     */
    private String generateAccountNumber() {

        // 1. 계좌 번호를 생성하고 AccountRepository 를 통해 고유성을 체크한다.
        //  1-1. 고유하지 않다면, 고유할 때까지 생성기를 돌려서 계좌 번호를 생성한다.
        String accountNumber;

        do {
            accountNumber = UUID.randomUUID().toString().replaceAll("^[0-9]", "").substring(0, 14);
        } while (accountRepository.existsByAccountNumber(accountNumber));

        // 2. 생성된 계좌 번호를 반환한다.
        return accountNumber;
    }


}
