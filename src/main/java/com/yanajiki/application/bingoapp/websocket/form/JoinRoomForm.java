package com.yanajiki.application.bingoapp.websocket.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket message payload for a player joining a bingo room.
 * <p>
 * The session code identifies the room and the player name is the display name
 * the joining player wishes to use. Names must be unique within a room.
 * </p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class JoinRoomForm {

	/** The player's display name. Must not be blank and may not exceed 50 characters. */
	@NotBlank
	@Size(max = 50)
	@JsonProperty("player-name")
	private String playerName;

	/** The unique code that identifies the bingo room to join. */
	@NotBlank
	@JsonProperty("session-code")
	private String sessionCode;
}
