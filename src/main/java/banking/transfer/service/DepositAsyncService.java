package banking.transfer.service;

import banking.account.service.AccountBalanceService;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DepositAsyncService {

	private final AccountBalanceService accountBalanceService;
	private final TransferRepository transferRepository;

	// TODO: 비동기 트랜잭션 관리 & 실패 시 재시도 처리
	@Async("depositExecutor")
	@Transactional
	public void processDepositAsync(Transfer withdrawalTransfer) {
		// TODO: 소수점 정합성 확인 필요
		BigDecimal withdrawalAmount = withdrawalTransfer.getAmount();
		BigDecimal depositAmount = withdrawalAmount.divide(withdrawalTransfer.getExchangeRate());

		// 입금 계좌 잔액 변경
		BigDecimal balancePostTransfer = accountBalanceService.increaseBalance(withdrawalTransfer.getDepositAccountId(), depositAmount);

		// 이체 내역 (입금) 추가
		saveDepositTransferDetails(withdrawalTransfer, depositAmount, balancePostTransfer);

		// TODO: 입금 알림
	}

	public void saveDepositTransferDetails(Transfer withdrawalTransfer, BigDecimal amount, BigDecimal balancePostTransaction) {
		Transfer transfer = Transfer.builder()
			.transferGroupId(withdrawalTransfer.getTransferGroupId())
			.transferOwnerId(withdrawalTransfer.getDepositAccountId())
			.transferType(TransferType.DEPOSIT)
			.withdrawalAccountId(withdrawalTransfer.getWithdrawalAccountId())
			.depositAccountId(withdrawalTransfer.getDepositAccountId())
			.currency(withdrawalTransfer.getCurrency())
			.exchangeRate(withdrawalTransfer.getExchangeRate())
			.amount(amount)
			.balancePostTransaction(balancePostTransaction)
			.build();

		transferRepository.save(transfer);
	}
}
