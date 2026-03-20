package com.yanajiki.application.bingoapp.websocket.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket message payload for drawing a bingo number in a room.
 * <p>
 * The session code identifies the room, the creator hash authenticates the requester,
 * and the number is the bingo ball value to draw (must be between 1 and 75 inclusive).
 * </p>
 */
@NoArgsConstructor
@Getter
@Setter
public class AddNumberForm {

	/** The unique code that identifies the bingo room. */
	@NotBlank
	@JsonProperty("session-code")
	private String sessionCode;

	/** The creator's authentication hash, issued when the room was created. */
	@NotBlank
	@JsonProperty("creator-hash")
	private String creatorHash;

	/** The bingo number to draw. Must be between 1 and 75 inclusive. */
	@NotNull
	@Min(1)
	@Max(75)
	private Integer number;
}
