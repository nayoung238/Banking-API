package banking.transfer.service;

import banking.account.dto.response.AccountPublicInfoResponse;
import banking.account.service.AccountService;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.transfer.dto.request.TransferDetailsRequest;
import banking.transfer.dto.response.TransferDetailResponse;
import banking.transfer.dto.response.PaymentTransferDetailResponse;
import banking.transfer.dto.response.TransferSummaryResponse;
import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import banking.transfer.repository.TransferRepository;
import banking.user.dto.response.UserPublicInfoResponse;
import banking.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferQueryService {

	private final TransferRepository transferRepository;
	private final AccountService accountService;
	private final UserService userService;

	/**
	 * 이체 내역 전체 조회
	 */
	// TODO: PAGE 적용
	public List<TransferSummaryResponse> findAllTransferSimple(Long userId, Long accountId) {
		// 계좌 소유자 검증
		accountService.verifyAccountOwner(accountId, userId);

		return transferRepository.findTransfersByUserIdAndAccountId(userId, accountId)
			.stream()
			.map(transfer -> {
				UserPublicInfoResponse peerUserPublicInfo;
				if (transfer.getTransferType().equals(TransferType.WITHDRAWAL)) {
					peerUserPublicInfo = userService.findUserPublicInfo(transfer.getDepositAccountId());
				} else if (transfer.getTransferType().equals(TransferType.DEPOSIT)) {
					peerUserPublicInfo = userService.findUserPublicInfo(transfer.getWithdrawalAccountId());
				} else {
					// TODO: 관리자에게 알림하고, 클라이언트에게는 응답
					throw new CustomException(ErrorCode.UNSUPPORTED_TRANSFER_TYPE);
				}
				return TransferSummaryResponse.of(transfer, peerUserPublicInfo.name());
			})
			.toList();
	}

	/**
	 * 이체 내역 단건 조회
	 */
	public TransferDetailResponse findTransferDetails(Long userId, TransferDetailsRequest request) {
		// 계좌 소유자 검증
		accountService.verifyAccountOwner(request.accountId(), userId);

		Transfer transfer = transferRepository.findById(request.transferId())
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

		if (!transfer.getTransferOwnerId().equals(userId)) {
			throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
		}

		AccountPublicInfoResponse withdrawalAccountPublicInfo = accountService.findAccountPublicInfo(transfer.getWithdrawalAccountId(), transfer);
		AccountPublicInfoResponse depositAccountPublicInfo = accountService.findAccountPublicInfo(transfer.getDepositAccountId(), transfer);

		verifyTransferAccount(userId, transfer.getTransferType(), withdrawalAccountPublicInfo, depositAccountPublicInfo);

		return TransferDetailResponse.of(transfer, withdrawalAccountPublicInfo, depositAccountPublicInfo);
	}

	private void verifyTransferAccount(Long userId, TransferType transferType,
									   AccountPublicInfoResponse withdrawalAccountPublicInfo, AccountPublicInfoResponse depositAccountPublicInfo) {

		if (transferType.equals(TransferType.WITHDRAWAL)) {
			if (!withdrawalAccountPublicInfo.ownerUserId().equals(userId)) {
				throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
			}
		} else if (transferType.equals(TransferType.DEPOSIT)) {
			if (!depositAccountPublicInfo.ownerUserId().equals(userId)) {
				throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
			}
		} else {
			throw new CustomException(ErrorCode.UNAUTHORIZED_TRANSFER_ACCESS);
		}
	}

	public PaymentTransferDetailResponse findTransfer(Long transferId) {
		Transfer transfer =  transferRepository.findById(transferId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_TRANSFER));

		return PaymentTransferDetailResponse.of(transfer);
	}
}
