package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.database.FeedbackMessageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DiscordNotifier}.
 */
@ExtendWith(MockitoExtension.class)
class DiscordNotifierTest {

	@Mock
	private RestClient restClient;

	@Mock
	private RestClient.RequestBodyUriSpec bodyUriSpec;

	@Mock
	private RestClient.RequestBodySpec bodySpec;

	@Mock
	private RestClient.ResponseSpec responseSpec;

	private DiscordNotifier notifier;

	@BeforeEach
	void setUp() {
		notifier = new DiscordNotifier(restClient);
	}

	private FeedbackMessageEntity buildMessage() {
		FeedbackMessageEntity entity = FeedbackMessageEntity.create("Alice", "alice@example.com", "+1234567890", "Great app!");
		ReflectionTestUtils.setField(entity, "id", 1L);
		return entity;
	}

	@Nested
	@DisplayName("notify()")
	class NotifyTests {

		@Test
		@DisplayName("skips HTTP call when webhookUrl is blank")
		void skipsWhenWebhookUrlBlank() {
			ReflectionTestUtils.setField(notifier, "webhookUrl", "");

			notifier.notify(buildMessage());

			verifyNoInteractions(restClient);
		}

		@Test
		@DisplayName("skips HTTP call when webhookUrl is whitespace only")
		void skipsWhenWebhookUrlWhitespace() {
			ReflectionTestUtils.setField(notifier, "webhookUrl", "   ");

			notifier.notify(buildMessage());

			verifyNoInteractions(restClient);
		}

		@Test
		@DisplayName("POSTs JSON payload to Discord when webhookUrl is configured")
		void postsPayloadWhenWebhookUrlSet() {
			ReflectionTestUtils.setField(notifier, "webhookUrl", "https://discord.com/api/webhooks/test");

			when(restClient.post()).thenReturn(bodyUriSpec);
			when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec);
			when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
			when(bodySpec.body(anyString())).thenReturn(bodySpec);
			when(bodySpec.retrieve()).thenReturn(responseSpec);
			when(responseSpec.toBodilessEntity()).thenReturn(null);

			notifier.notify(buildMessage());

			verify(restClient).post();
			verify(bodyUriSpec).uri("https://discord.com/api/webhooks/test");
			verify(bodySpec).contentType(MediaType.APPLICATION_JSON);
			verify(bodySpec).body(anyString());
			verify(bodySpec).retrieve();
			verify(responseSpec).toBodilessEntity();
		}

		@Test
		@DisplayName("swallows exception from HTTP call without propagating")
		void swallowsExceptionFromHttpCall() {
			ReflectionTestUtils.setField(notifier, "webhookUrl", "https://discord.com/api/webhooks/test");

			when(restClient.post()).thenReturn(bodyUriSpec);
			when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec);
			when(bodySpec.contentType(any(MediaType.class))).thenReturn(bodySpec);
			when(bodySpec.body(anyString())).thenReturn(bodySpec);
			when(bodySpec.retrieve()).thenThrow(new RuntimeException("connection refused"));

			// Should not throw
			notifier.notify(buildMessage());
		}
	}
}
