package com.yanajiki.application.bingoapp.api.response;

import com.yanajiki.application.bingoapp.database.PlayerEntity;

import java.time.LocalDateTime;

/**
 * Immutable DTO representing a player in a bingo room.
 * <p>
 * Used in REST responses and WebSocket broadcasts to expose player data
 * without leaking any internal entity details.
 * </p>
 *
 * @param name         the player's display name
 * @param joinDateTime the timestamp at which the player joined the room
 */
public record PlayerDTO(
	String name,
	LocalDateTime joinDateTime
) {

	/**
	 * Creates a {@link PlayerDTO} from a {@link PlayerEntity}.
	 *
	 * @param entity the persisted player entity
	 * @return a new {@link PlayerDTO} containing the player's name and join timestamp
	 */
	public static PlayerDTO fromEntity(PlayerEntity entity) {
		return new PlayerDTO(
			entity.getName(),
			entity.getJoinDateTime()
		);
	}
}
