package com.yanajiki.application.bingoapp.websocket.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket payload for drawing a tiebreaker number for a specific slot.
 * <p>
 * Sent by the game master for each contestant in turn. The service picks
 * a random number from the undrawn pool (excluding other tiebreaker draws)
 * and assigns it to the given slot.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TiebreakDrawForm {

	/** The unique code that identifies the bingo room. */
	@NotBlank
	@JsonProperty("session-code")
	private String sessionCode;

	/** The creator's authentication hash, issued when the room was created. */
	@NotBlank
	@JsonProperty("creator-hash")
	private String creatorHash;

	/** The 1-based slot index of the contestant drawing. */
	private int slot;
}
