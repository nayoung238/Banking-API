package banking.transfer.service;

import banking.account.service.AccountBalanceService;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferCompensationService {

	private final TransferRepository transferRepository;
	private final AccountBalanceService accountBalanceService;

	@Transactional
	public void processTransferFailedEvent(String transferGroupId) {
		Transfer baseTransfer = transferRepository.findByTransferGroupIdAndTransferType(transferGroupId, TransferType.WITHDRAWAL)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_WITHDRAWAL_TRANSFER));

		BigDecimal refundAmount = baseTransfer.getAmount();
		BigDecimal balancePostTransfer = accountBalanceService.increaseBalanceWithLock(baseTransfer.getWithdrawalAccountId(), refundAmount);

		Transfer refundTransfer = Transfer.builder()
			.transferGroupId(transferGroupId)
			.transferType(TransferType.REFUNDED)
			.withdrawalAccountId(baseTransfer.getDepositAccountId())
			.depositAccountId(baseTransfer.getWithdrawalAccountId())
			.currency(baseTransfer.getCurrency())
			.exchangeRate(baseTransfer.getExchangeRate())
			.amount(refundAmount)
			.balancePostTransaction(balancePostTransfer)
			.build();

		transferRepository.save(refundTransfer);
	}
}
