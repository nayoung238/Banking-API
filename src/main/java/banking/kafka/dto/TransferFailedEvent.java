package banking.kafka.dto;

import banking.transfer.enums.TransferType;
import lombok.Builder;

@Builder
public record TransferFailedEvent (

	String transferGroupId,
	TransferType transferType
) {

	public static TransferFailedEvent of(String transferGroupId, TransferType transferType) {
		return TransferFailedEvent.builder()
			.transferGroupId(transferGroupId)
			.transferType(transferType)
			.build();
	}
}
