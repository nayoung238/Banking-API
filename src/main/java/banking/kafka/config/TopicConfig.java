package banking.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class TopicConfig {

	public static final String TRANSFER_FAILED_TOPIC = "banking.transfer.transfer-failed";

	@Bean
	public KafkaAdmin.NewTopics newTopics() {
		return new KafkaAdmin.NewTopics(
			TopicBuilder.name(TRANSFER_FAILED_TOPIC)
				.partitions(1)
				.replicas(1)
				.build()
		);
	}
}
