package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.api.form.FeedbackForm;
import com.yanajiki.application.bingoapp.api.response.FeedbackMessageDTO;
import com.yanajiki.application.bingoapp.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for feedback form submissions.
 * <p>
 * Thin controller — delegates all business logic to {@link FeedbackService}.
 * </p>
 */
@Tag(name = "Feedback", description = "Feedback form submission")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

	private final FeedbackService feedbackService;

	/**
	 * Receives a feedback form submission, persists it, and fires an async Discord notification.
	 *
	 * @param form the validated feedback form
	 * @return the persisted feedback message as a DTO
	 */
	@Operation(
		summary = "Submit a feedback message",
		description = "Saves the message and fires an async Discord notification."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Message received",
			content = @Content(schema = @Schema(implementation = FeedbackMessageDTO.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "Validation error"
		)
	})
	@PostMapping
	public FeedbackMessageDTO submit(@Valid @RequestBody FeedbackForm form) {
		return feedbackService.submit(form);
	}
}
