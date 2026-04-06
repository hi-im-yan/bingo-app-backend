package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.form.FeedbackForm;
import com.yanajiki.application.bingoapp.api.response.FeedbackMessageDTO;
import com.yanajiki.application.bingoapp.database.FeedbackMessageEntity;
import com.yanajiki.application.bingoapp.database.FeedbackMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service layer for handling feedback form submissions.
 * <p>
 * Persists the submitted {@link FeedbackForm} as a {@link FeedbackMessageEntity},
 * triggers an async Discord notification via {@link DiscordNotifier}, and returns
 * a {@link FeedbackMessageDTO} with all fields populated.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

	private final FeedbackMessageRepository repository;
	private final DiscordNotifier discordNotifier;

	/**
	 * Processes a feedback form submission.
	 * <p>
	 * Creates and persists a {@link FeedbackMessageEntity} from the given form,
	 * then fires a non-blocking Discord notification. Optional fields ({@code email}
	 * and {@code phone}) may be {@code null} and are stored as-is.
	 * </p>
	 *
	 * @param form the submitted feedback form; {@code name} and {@code content} are required,
	 *             {@code email} and {@code phone} are optional
	 * @return a {@link FeedbackMessageDTO} representing the persisted message
	 */
	public FeedbackMessageDTO submit(FeedbackForm form) {
		log.info("Saving feedback message from '{}'", form.getName());

		FeedbackMessageEntity entity = FeedbackMessageEntity.create(
			form.getName(),
			form.getEmail(),
			form.getPhone(),
			form.getContent()
		);

		FeedbackMessageEntity saved = repository.save(entity);
		log.info("Feedback message saved with id={}", saved.getId());

		discordNotifier.notify(saved);

		return FeedbackMessageDTO.fromEntity(saved);
	}
}
