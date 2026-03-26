package com.yanajiki.application.bingoapp.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket (STOMP) broker configuration.
 * <p>
 * Registers the {@code /bingo-connect} STOMP endpoint and configures the in-memory
 * message broker. Allowed origins for the WebSocket handshake are driven by the
 * {@code app.cors.allowed-origins} property so that dev and prod profiles each
 * get the appropriate policy.
 * </p>
 * <p>
 * When the configured value is {@code *}, Spring Boot 3.x requires using
 * {@code setAllowedOriginPatterns("*")} instead of {@code setAllowedOrigins("*")}.
 * </p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	/** Allowed origin(s) for WebSocket handshakes, resolved from {@code app.cors.allowed-origins}. */
	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	/**
	 * Configures the in-memory STOMP message broker.
	 *
	 * @param config the message broker registry
	 */
	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/room", "/testing"); // topic prefix
		config.setApplicationDestinationPrefixes("/app"); // send message prefix
	}

	/**
	 * Registers the STOMP WebSocket endpoint and applies the configured CORS origin policy.
	 *
	 * @param registry the STOMP endpoint registry
	 */
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		if ("*".equals(allowedOrigins)) {
			registry.addEndpoint("/bingo-connect").setAllowedOriginPatterns("*").withSockJS();
		} else {
			registry.addEndpoint("/bingo-connect").setAllowedOrigins(allowedOrigins).withSockJS();
		}
	}

}
