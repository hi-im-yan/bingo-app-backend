package com.yanajiki.application.bingoapp.websocket.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket payload for automatic number draw.
 * <p>
 * Only requires session identification and creator authentication — no number needed.
 * The number is randomly selected by the service for AUTOMATIC draw mode rooms.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DrawNumberForm {

	/** The unique code that identifies the bingo room. */
	@NotBlank
	@JsonProperty("session-code")
	private String sessionCode;

	/** The creator's authentication hash, issued when the room was created. */
	@NotBlank
	@JsonProperty("creator-hash")
	private String creatorHash;
}
