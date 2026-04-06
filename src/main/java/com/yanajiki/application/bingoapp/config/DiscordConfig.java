package com.yanajiki.application.bingoapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for the Discord webhook HTTP client.
 * <p>
 * Provides a {@link RestClient} bean used by {@link com.yanajiki.application.bingoapp.service.DiscordNotifier}
 * to POST notification payloads to a Discord webhook URL.
 * </p>
 */
@Configuration
public class DiscordConfig {

	/**
	 * Creates a default {@link RestClient} for Discord webhook calls.
	 *
	 * @return a new {@link RestClient} instance
	 */
	@Bean
	public RestClient discordRestClient() {
		return RestClient.builder().build();
	}
}
