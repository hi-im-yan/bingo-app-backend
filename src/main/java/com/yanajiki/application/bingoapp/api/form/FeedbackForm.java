package com.yanajiki.application.bingoapp.api.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for submitting a feedback message.
 * <p>
 * Name and content are required. Email and phone are optional — users may submit
 * anonymously or provide contact information if they want a response.
 * </p>
 */
@NoArgsConstructor
@Getter
@Setter
public class FeedbackForm {

	/** The sender's display name. Must not be blank and at most 100 characters. */
	@NotBlank
	@Size(max = 100)
	private String name;

	/** Optional email address. If provided, must be a valid email format. At most 254 characters. */
	@Email
	@Size(max = 254)
	private String email;

	/** Optional phone number. At most 20 characters. */
	@Size(max = 20)
	private String phone;

	/** The feedback message content. Must not be blank and at most 2000 characters. */
	@NotBlank
	@Size(max = 2000)
	private String content;
}
