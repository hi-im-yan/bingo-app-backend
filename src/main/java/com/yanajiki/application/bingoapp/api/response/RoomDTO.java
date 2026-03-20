package com.yanajiki.application.bingoapp.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.game.NumberLabelMapper;

import java.util.List;

/**
 * Immutable DTO representing a bingo room returned to API callers.
 * <p>
 * Two static factory methods produce view-specific projections:
 * {@link #fromEntityToCreator} includes the {@code creatorHash} for administrative use,
 * while {@link #fromEntityToPlayer} omits it for public/player access.
 * Fields with {@code null} values are excluded from the JSON output.
 * </p>
 * <p>
 * Both {@code drawnNumbers} and {@code drawnLabels} are included in responses so that
 * the frontend has display-ready labels (e.g., {@code "N-42"}) as well as raw integers
 * for any game logic it may need to perform.
 * </p>
 *
 * @param name         the unique display name of the room
 * @param description  an optional description of the room
 * @param sessionCode  the public code players use to join the room
 * @param creatorHash  the creator's authentication token; {@code null} in player view
 * @param drawnNumbers the list of bingo numbers drawn so far in this room (raw integers)
 * @param drawnLabels  the list of display labels for drawn numbers (e.g., {@code "N-42"})
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomDTO(
	String name,
	String description,
	String sessionCode,
	String creatorHash,
	List<Integer> drawnNumbers,
	List<String> drawnLabels
) {

	/**
	 * Creates a creator-view DTO from a {@link RoomEntity}, exposing the {@code creatorHash}.
	 * Use this when the caller has authenticated as the room creator.
	 *
	 * @param entity the persisted room entity
	 * @param mapper the {@link NumberLabelMapper} used to produce display labels for drawn numbers
	 * @return a {@link RoomDTO} with all fields populated, including {@code creatorHash}
	 */
	public static RoomDTO fromEntityToCreator(RoomEntity entity, NumberLabelMapper mapper) {
		List<Integer> numbers = entity.getDrawnNumbers();
		return new RoomDTO(
			entity.getName(),
			entity.getDescription(),
			entity.getSessionCode(),
			entity.getCreatorHash(),
			numbers,
			toLabels(numbers, mapper)
		);
	}

	/**
	 * Creates a player-view DTO from a {@link RoomEntity}, omitting the {@code creatorHash}.
	 * Use this for public endpoints and WebSocket broadcasts.
	 *
	 * @param entity the persisted room entity
	 * @param mapper the {@link NumberLabelMapper} used to produce display labels for drawn numbers
	 * @return a {@link RoomDTO} with {@code creatorHash} set to {@code null}
	 */
	public static RoomDTO fromEntityToPlayer(RoomEntity entity, NumberLabelMapper mapper) {
		List<Integer> numbers = entity.getDrawnNumbers();
		return new RoomDTO(
			entity.getName(),
			entity.getDescription(),
			entity.getSessionCode(),
			null,
			numbers,
			toLabels(numbers, mapper)
		);
	}

	/**
	 * Converts a list of raw numbers to their display labels using the given mapper.
	 *
	 * @param numbers the raw drawn numbers
	 * @param mapper  the label mapper for the active game type
	 * @return an ordered list of label strings; empty list if {@code numbers} is empty
	 */
	private static List<String> toLabels(List<Integer> numbers, NumberLabelMapper mapper) {
		return numbers.stream()
			.map(mapper::toLabel)
			.toList();
	}
}
