package com.yanajiki.application.bingoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application entry point.
 * <p>
 * Enables JPA auditing (managed timestamps), scheduling (room expiration cleanup),
 * and async execution (Discord webhook notifications).
 * </p>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
