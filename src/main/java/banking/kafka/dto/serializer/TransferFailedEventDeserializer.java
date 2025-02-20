package banking.kafka.dto.serializer;

import banking.kafka.dto.TransferFailedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

@Slf4j
public class TransferFailedEventDeserializer implements Deserializer<TransferFailedEvent> {

	ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Override
	public TransferFailedEvent deserialize(String topic, byte[] data) {
		TransferFailedEvent message = null;
		try {
			message = objectMapper.readValue(data, TransferFailedEvent.class);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		return message;
	}
}
