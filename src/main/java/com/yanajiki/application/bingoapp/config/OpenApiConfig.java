package com.yanajiki.application.bingoapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration.
 * <p>
 * Exposes the {@link OpenAPI} bean that populates the Swagger UI metadata
 * available at {@code /swagger-ui.html}.
 * </p>
 */
@Configuration
public class OpenApiConfig {

	/**
	 * Builds the top-level {@link OpenAPI} descriptor for the Bingo App API.
	 *
	 * @return a configured {@link OpenAPI} instance with title, description, version and contact
	 */
	@Bean
	public OpenAPI bingoAppOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("Bingo App API")
				.description("API for managing bingo rooms and real-time number drawing")
				.version("2.0.0")
				.contact(new Contact()
					.name("Bingo App")
					.url("https://github.com/yanajiki/bingoapp")));
	}
}
