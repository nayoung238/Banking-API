package banking.account.service;

import banking.account.dto.request.DepositRequest;
import banking.account.dto.request.WithdrawalRequest;
import banking.account.dto.response.AccountDetailResponse;
import banking.account.entity.Account;
import banking.account.repository.AccountRepository;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountBalanceService {

	private final AccountRepository accountRepository;
	private final AccountService accountService;

	/**
	 * ATM 전용 입금
	 * @param request - 입금 계좌번호, 입금 계좌 비밀번호, 입금 금액
	 * @return 입금 계좌 상태
	 */
	@Transactional
	public AccountDetailResponse atmDeposit(DepositRequest request) {
		Account account = accountRepository.findByAccountNumberWithLock(request.accountNumber())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

		if(!account.getPassword().equals(request.accountPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}

		account.increaseBalance(request.amount());
		return AccountDetailResponse.of(account);
	}

	/**
	 * ATM 전용 출금
	 * @param request - 출금 계좌번호, 출금 계좌 비밀번호, 출금 금액
	 * @return 출금 계좌 상태
	 */
	public AccountDetailResponse atmWithdrawal(WithdrawalRequest request) {
		Account account = accountRepository.findByAccountNumberWithLock(request.accountNumber())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

		if(account.getPassword().equals(request.accountPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}

		account.decreaseBalance(request.amount());
		return AccountDetailResponse.of(account);
	}

	@Transactional
	public BigDecimal increaseBalanceWithLock(Long accountId, BigDecimal amount) {
		Account account = accountRepository.findByIdWithLock(accountId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

		accountService.verifyAccountActiveStatus(account);

		account.increaseBalance(amount);
		return account.getBalance();
	}
}
