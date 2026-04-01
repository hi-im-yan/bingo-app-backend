package com.yanajiki.application.bingoapp.websocket.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket payload for starting a tiebreaker in an automatic draw room.
 * <p>
 * Sent by the game master when multiple players get BINGO simultaneously.
 * The {@code playerCount} determines how many slots will draw in this tiebreaker.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartTiebreakForm {

	/** The unique code that identifies the bingo room. */
	@NotBlank
	@JsonProperty("session-code")
	private String sessionCode;

	/** The creator's authentication hash, issued when the room was created. */
	@NotBlank
	@JsonProperty("creator-hash")
	private String creatorHash;

	/** Number of contestants competing in the tiebreaker (minimum 2, capped by available numbers). */
	@JsonProperty("player-count")
	private int playerCount;
}
