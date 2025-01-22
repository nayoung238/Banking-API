package SN.BANK;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "SN.BANK.exchangeRate")
public class SnBankApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnBankApplication.class, args);
	}

}
