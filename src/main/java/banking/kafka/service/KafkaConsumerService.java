package banking.kafka.service;

import banking.kafka.config.TopicConfig;
import banking.kafka.dto.TransferFailedEvent;
import banking.transfer.service.TransferCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

	private final TransferCompensationService transferCompensationService;

	@KafkaListener(topics = TopicConfig.TRANSFER_FAILED_TOPIC)
	private void listenTransferFailedTopic(ConsumerRecord<String, TransferFailedEvent> record) {
		log.info("Event consumed successfully -> Topic: {}, transferGroupId(key): {}",
			record.topic(),
			record.key());

		transferCompensationService.processTransferFailedEvent(record.key());
	}
}
