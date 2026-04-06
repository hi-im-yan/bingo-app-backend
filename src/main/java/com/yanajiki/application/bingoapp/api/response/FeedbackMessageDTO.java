package com.yanajiki.application.bingoapp.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yanajiki.application.bingoapp.database.FeedbackMessageEntity;

import java.time.Instant;

/**
 * Immutable DTO representing a feedback message returned to API callers.
 * <p>
 * Optional fields ({@code email}, {@code phone}) are excluded from the JSON output
 * when {@code null}. Use {@link #fromEntity(FeedbackMessageEntity)} to convert a
 * persisted entity to this response view.
 * </p>
 *
 * @param id        the unique identifier of the feedback message
 * @param name      the sender's name
 * @param email     the sender's email address; omitted from JSON when {@code null}
 * @param phone     the sender's phone number; omitted from JSON when {@code null}
 * @param content   the feedback message body
 * @param createdAt the timestamp at which the message was persisted
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedbackMessageDTO(
	Long id,
	String name,
	String email,
	String phone,
	String content,
	Instant createdAt
) {

	/**
	 * Creates a {@link FeedbackMessageDTO} from a {@link FeedbackMessageEntity}.
	 *
	 * @param entity the persisted feedback message entity
	 * @return a new {@link FeedbackMessageDTO} with all fields mapped from the entity
	 */
	public static FeedbackMessageDTO fromEntity(FeedbackMessageEntity entity) {
		return new FeedbackMessageDTO(
			entity.getId(),
			entity.getName(),
			entity.getEmail(),
			entity.getPhone(),
			entity.getContent(),
			entity.getCreatedAt()
		);
	}
}
