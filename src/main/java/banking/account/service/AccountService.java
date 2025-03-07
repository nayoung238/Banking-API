package banking.account.service;

import banking.account.dto.request.AccountCreationRequest;
import banking.account.dto.response.AccountDetailResponse;
import banking.account.dto.response.AccountPublicInfoResponse;
import banking.account.enums.AccountStatus;
import banking.account.repository.AccountRepository;
import banking.account.entity.Account;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.transfer.dto.response.PaymentTransferDetailResponse;
import banking.transfer.entity.Transfer;
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
    public AccountDetailResponse createAccount(Long userId, AccountCreationRequest request) {
        User user = userService.findUserEntity(userId);
        String accountName = request.accountName() != null ? request.accountName() : "은행 계좌명";

        Account account = Account.builder()
            .user(user)
            .accountNumber(generateUniqueAccountNumber())
            .password(request.password())
            .balance(BigDecimal.valueOf(0, 2))
            .accountName(accountName)
            .currency(request.currency())
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        accountRepository.save(account);
        return AccountDetailResponse.of(account);
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.valueOf(ThreadLocalRandom.current().nextLong(10000000000000L, 99999999999999L));
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber.substring(0, 7) + "-" + accountNumber.substring(7);
    }

    /**
     * /payment, /transfer 에서 사용
     * 계좌 소유자만 접근 가능
     * @param accountId   요청 계좌 PK
     * @param userId      요청 사용자 PK
     * @param password    요청 계좌 비밀번호
     * @return 계좌 소유자인 경우 계좌 반환
     */
    @Transactional
    public Account findAccountWithLock(Long userId, Long accountId, String password) {
        boolean isOwner = accountRepository.existsByIdAndUserId(accountId, userId);
        if (!isOwner) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }

        // 소유자 확인 후 락 사용
        Account account = accountRepository.findByIdWithLock(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        // TODO: 비밀번호 암호화
        if (!account.getPassword().equals(password)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
        return account;
    }

    /**
     * /payment, /transfer 에서 사용 (refund)
     * 거래에 연관된 사용자만 상대 계좌 접근 가능
     * @param accountId 상태 계좌 PK
     * @param transfer 관련 거래
     * @return 거래 권한 확인 후 상대 계좌 반환  // TODO: 비밀번호 제외
     */
    @Transactional
    public Account findAccountWithLock(Long accountId, Transfer transfer) {
        if (!isTransferParticipant(accountId, transfer)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }

        return accountRepository.findByIdWithLock(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));
    }

    /**
     * 거래 생성 시 상대 계좌의 일부 데이터 필요 (계좌 PK, 통화)
     * @param accountNumber 거래 상대 계좌 번호
     * @return 상대 계좌 일부 반환
     */
    public AccountPublicInfoResponse findAccountPublicInfo(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return AccountPublicInfoResponse.of(account);
    }

    /**
     * 거래 취소, 이체 조회 시 상대 계좌 일부 데이터 필요 (계좌 PK, 통화, 계좌번호, 계좌 소유자명)
     * @param accountId 거래 상대 계좌 PK
     * @param transfer 관련 거래
     * @return 거래 권한 확인 후 상대 계좌 일부 반환
     */
    public AccountPublicInfoResponse findAccountPublicInfo(Long accountId, Transfer transfer) {
        if (!isTransferParticipant(accountId, transfer)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return AccountPublicInfoResponse.of(account);
    }

    /**
     * 거래 취소, 이체 조회 시 상대 계좌 일부 데이터 필요 (계좌 PK, 통화, 계좌번호, 계좌 소유자명)
     * @param accountId 거래 상대 계좌 PK
     * @param transferResponse 관련 거래
     * @return 거래 권한 확인 후 상대 계좌 일부 반환
     */
    public AccountPublicInfoResponse findAccountPublicInfo(Long accountId, PaymentTransferDetailResponse transferResponse) {
        if (!isTransferParticipant(accountId, transferResponse)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCOUNT_ACCESS);
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return AccountPublicInfoResponse.of(account);
    }

    private boolean isTransferParticipant(Long accountId, Transfer transfer) {
        return transfer.getWithdrawalAccountId().equals(accountId)
            || transfer.getDepositAccountId().equals(accountId);
    }

    private boolean isTransferParticipant(Long accountId, PaymentTransferDetailResponse transferResponse) {
        return transferResponse.withdrawalAccountId().equals(accountId)
            || transferResponse.depositAccountId().equals(accountId);
    }

    public void verifyAccountActiveStatus(Account account) {
        switch (account.getAccountStatus()) {
            case ACTIVE -> {}
            case SUSPENDED, RESTRICTED -> throw new CustomException(ErrorCode.ACCOUNT_RESTRICTED);
            case CLOSED -> throw new CustomException(ErrorCode.ACCOUNT_CLOSED);
            default -> throw new CustomException(ErrorCode.ACCOUNT_UNAVAILABLE);
        }
    }

    public AccountDetailResponse findAccount(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return AccountDetailResponse.of(account);
    }

    public List<AccountDetailResponse> findAllAccounts(Long userId) {
        List<Account> accounts = accountRepository.findAllByUserId(userId);

        return accounts.stream()
            .map(AccountDetailResponse::of)
            .toList();
    }

    public void verifyAccountOwner(Long accountId, Long userId) {
        if (!accountRepository.existsByIdAndUserId(accountId, userId)) {
            throw new CustomException(ErrorCode.NOT_FOUND_ACCOUNT);
        }
    }
}
