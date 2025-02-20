package banking.kafka.service;

import banking.kafka.dto.TransferFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerService {

	private final KafkaTemplate<String, TransferFailedEvent> kafkaTemplate;

	public void send(String topic, @Payload(required = false) TransferFailedEvent transferFailedEvent) {
		kafkaTemplate.send(topic, transferFailedEvent.transferGroupId(), transferFailedEvent)
			.whenComplete((stringTransferFailedEventSendResult, throwable) -> {
				if (throwable == null) {
					RecordMetadata metadata = stringTransferFailedEventSendResult.getRecordMetadata();
					log.info("Kafka event published successfully -> topic={}, transferGroupId(key)={}",
						metadata.topic(),
						stringTransferFailedEventSendResult.getProducerRecord().key());
				}
				else {
					// TODO: 카프카 이벤트 재발행
				}
			});
	}
}
