package banking.transfer.service;

import banking.account.service.AccountBalanceService;
import banking.common.exception.CustomException;
import banking.kafka.config.TopicConfig;
import banking.kafka.dto.TransferFailedEvent;
import banking.kafka.service.KafkaProducerService;
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
	private final KafkaProducerService kafkaProducerService;

	@Async("depositExecutor")
	@Transactional
	public void processDepositAsync(Transfer withdrawalTransfer) {
		BigDecimal withdrawalAmount = withdrawalTransfer.getAmount();
		BigDecimal depositAmount = withdrawalAmount.divide(withdrawalTransfer.getExchangeRate());

		try {
			// 입금 계좌 잔액 변경
			BigDecimal balancePostTransfer = accountBalanceService.increaseBalanceWithLock(withdrawalTransfer.getDepositAccountId(), depositAmount);

			// 이체 내역 (입금) 추가
			saveDepositTransferDetails(withdrawalTransfer, depositAmount, balancePostTransfer);

			// TODO: 입금 알림
		} catch (CustomException e) {	// 입금 계좌 !active 상태로, 입금 불가능
			kafkaProducerService.send(TopicConfig.TRANSFER_FAILED_TOPIC, TransferFailedEvent.of(withdrawalTransfer.getTransferGroupId(), TransferType.DEPOSIT));
		}
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
