package com.billmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync(proxyTargetClass = true)
public class BillMateApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillMateApplication.class, args);
	}

}
