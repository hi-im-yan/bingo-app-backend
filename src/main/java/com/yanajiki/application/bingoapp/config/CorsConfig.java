package com.yanajiki.application.bingoapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration for all REST API endpoints.
 * <p>
 * Applies CORS rules to all routes based on the {@code app.cors.allowed-origins}
 * property, which is set per Spring profile (e.g. {@code *} in dev, explicit origins in prod).
 * This supersedes any {@code @CrossOrigin} annotations on individual controllers.
 * </p>
 */
@Configuration
public class CorsConfig {

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	/**
	 * Registers a global CORS mapping for all routes.
	 * <p>
	 * When the configured origin is {@code *}, credentials are not supported and the
	 * wildcard is left as-is. For explicit origins, the same policy applies since
	 * this application does not use cookie-based authentication.
	 * </p>
	 *
	 * @return a {@link WebMvcConfigurer} that applies the CORS policy
	 */
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
					.allowedOrigins(allowedOrigins)
					.allowedMethods("GET", "POST", "DELETE", "OPTIONS")
					.allowedHeaders("*")
					.allowCredentials(false);
			}
		};
	}
}
