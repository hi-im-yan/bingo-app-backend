package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.database.FeedbackMessageEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Async service that sends feedback message notifications to a Discord webhook.
 * <p>
 * This service is fire-and-forget: if the webhook URL is not configured, the call
 * is skipped silently. If the HTTP call fails for any reason, the exception is caught
 * and logged at WARN level — it is never propagated to the caller.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordNotifier {

	private final RestClient discordRestClient;

	/** Discord webhook URL. Blank by default (disabled). Override per profile via {@code app.discord.webhook-url}. */
	@Value("${app.discord.webhook-url:}")
	private String webhookUrl;

	/**
	 * Sends an async Discord notification for a newly submitted feedback message.
	 * <p>
	 * The method returns immediately. If {@code webhookUrl} is blank, no HTTP call is made.
	 * Any exception from the HTTP call is swallowed and logged.
	 * </p>
	 *
	 * @param message the feedback message entity that was just persisted
	 */
	@Async
	public void notify(FeedbackMessageEntity message) {
		if (!StringUtils.hasText(webhookUrl)) {
			log.debug("Discord webhook URL not configured, skipping notification");
			return;
		}
		try {
			String payload = buildPayload(message);
			discordRestClient.post()
				.uri(webhookUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.body(payload)
				.retrieve()
				.toBodilessEntity();
			log.info("Discord notification sent for feedback message id={}", message.getId());
		} catch (Exception ex) {
			log.warn("Failed to send Discord notification: {}", ex.getMessage());
		}
	}

	/**
	 * Builds the Discord embed JSON payload for the given feedback message.
	 *
	 * @param message the feedback message
	 * @return a JSON string representing the Discord embed payload
	 */
	private String buildPayload(FeedbackMessageEntity message) {
		return """
			{"embeds":[{"title":"New Feedback Message","fields":[{"name":"Name","value":"%s"},{"name":"Email","value":"%s"},{"name":"Phone","value":"%s"},{"name":"Message","value":"%s"}],"color":5814783}]}
			""".formatted(
				escape(message.getName()),
				emptyIfNull(message.getEmail()),
				emptyIfNull(message.getPhone()),
				escape(message.getContent())
			);
	}

	/**
	 * Escapes double-quotes and newlines for safe embedding in a JSON string value.
	 *
	 * @param s the string to escape, or {@code null}
	 * @return the escaped string, or an empty string if {@code s} is {@code null}
	 */
	private String escape(String s) {
		return s == null ? "" : s.replace("\"", "\\\"").replace("\n", "\\n");
	}

	/**
	 * Returns the string value, or {@code "—"} if {@code null}.
	 *
	 * @param s the string to check
	 * @return the original string or an em-dash placeholder
	 */
	private String emptyIfNull(String s) {
		return s == null ? "—" : s;
	}
}
