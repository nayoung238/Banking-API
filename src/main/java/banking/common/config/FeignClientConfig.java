package banking.common.config;

import feign.Request;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class FeignClientConfig {

	public Request.Options options() {
		return new Request.Options(
			Duration.ofMillis(3000),
			Duration.ofMillis(2000),
			true
		);
	}
}
