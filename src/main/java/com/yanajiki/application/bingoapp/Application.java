package com.yanajiki.application.bingoapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application entry point.
 * <p>
 * Enables JPA auditing (managed timestamps) and scheduling (room expiration cleanup).
 * </p>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
