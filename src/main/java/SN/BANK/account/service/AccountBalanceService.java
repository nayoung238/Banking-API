package SN.BANK.account.service;

import SN.BANK.account.dto.request.DepositRequestDto;
import SN.BANK.account.dto.request.WithdrawalRequestDto;
import SN.BANK.account.dto.response.AccountResponseDto;
import SN.BANK.account.entity.Account;
import SN.BANK.account.repository.AccountRepository;
import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountBalanceService {

	private final AccountRepository accountRepository;

	/**
	 * ATM 전용 입금
	 * @param request - 입금 계좌번호, 입금 계좌 비밀번호, 입금 금액
	 * @return 입금 계좌 상태
	 */
	@Transactional
	public AccountResponseDto atmDeposit(DepositRequestDto request) {
		Account account = accountRepository.findByAccountNumberWithPessimisticLock(request.accountNumber())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

		if(!account.getPassword().equals(request.accountPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}

		account.increaseBalance(request.amount());
		return AccountResponseDto.of(account);
	}

	/**
	 * ATM 전용 출금
	 * @param request - 출금 계좌번호, 출금 계좌 비밀번호, 출금 금액
	 * @return 출금 계좌 상태
	 */
	public AccountResponseDto atmWithdraw(WithdrawalRequestDto request) {
		Account account = accountRepository.findByAccountNumberWithPessimisticLock(request.accountNumber())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

		if(account.getPassword().equals(request.accountPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}

		account.decreaseBalance(request.amount());
		return AccountResponseDto.of(account);
	}
}
