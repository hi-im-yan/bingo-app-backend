package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.form.CreateRoomForm;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.ConflictException;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.game.NumberLabelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Service layer for bingo room management.
 * <p>
 * Contains all business logic for creating, querying, deleting rooms,
 * and drawing numbers. Controllers delegate to this service and handle
 * only HTTP/WebSocket concerns.
 * </p>
 * <p>
 * Drawn number validation (range and duplicate checks) lives here rather than
 * in {@link RoomEntity}, because the valid range is defined by the active
 * {@link NumberLabelMapper} (i.e., by game type) and must not be hardcoded in
 * the entity.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

	private final RoomRepository repository;
	private final NumberLabelMapper numberLabelMapper;

	/**
	 * Creates a new bingo room with the given form data.
	 * <p>
	 * Validates that no room with the same name already exists, then creates
	 * and persists a new {@link RoomEntity}. Returns the creator view of the
	 * room, which includes the {@code creatorHash} needed for administrative actions.
	 * </p>
	 *
	 * @param form the creation form containing name and description
	 * @return a {@link RoomDTO} with full creator data, including {@code creatorHash}
	 * @throws ConflictException if a room with the same name already exists
	 */
	public RoomDTO createRoom(CreateRoomForm form) {
		log.info("Creating room with name '{}'", form.getName());

		repository.findByName(form.getName())
				.ifPresent(existing -> {
					throw new ConflictException("Room already exists.");
				});

		RoomEntity entity = RoomEntity.createEntityObject(form.getName(), form.getDescription());
		RoomEntity saved = repository.save(entity);

		log.info("Room created successfully with sessionCode '{}'", saved.getSessionCode());
		return RoomDTO.fromEntityToCreator(saved, numberLabelMapper);
	}

	/**
	 * Finds a room by its session code, returning either the creator or player view.
	 * <p>
	 * If a non-blank {@code creatorHash} is provided, the method attempts to find the room
	 * matching both the session code and the hash, returning the creator view on success.
	 * If no {@code creatorHash} is provided, the room is found by session code alone and
	 * the player view (without {@code creatorHash}) is returned.
	 * </p>
	 *
	 * @param sessionCode the public session code of the room
	 * @param creatorHash the creator's authentication hash (optional)
	 * @return a {@link RoomDTO} in creator view if hash matches, otherwise player view
	 * @throws RoomNotFoundException if no matching room is found
	 */
	public RoomDTO findRoomBySessionCode(String sessionCode, String creatorHash) {
		if (StringUtils.isNotBlank(creatorHash)) {
			log.debug("Looking up room '{}' with creator hash", sessionCode);
			RoomEntity entity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
					.orElseThrow(() -> new RoomNotFoundException("not found"));
			return RoomDTO.fromEntityToCreator(entity, numberLabelMapper);
		}

		log.debug("Looking up room '{}' as player", sessionCode);
		RoomEntity entity = repository.findBySessionCode(sessionCode)
				.orElseThrow(() -> new RoomNotFoundException("not found"));
		return RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
	}

	/**
	 * Deletes a room identified by session code, authenticated by creator hash.
	 * <p>
	 * The room is only deleted if both the session code and creator hash match,
	 * ensuring that only the room creator can perform this action.
	 * </p>
	 *
	 * @param sessionCode the public session code of the room
	 * @param creatorHash the creator's authentication hash
	 * @throws RoomNotFoundException if no room matches the given session code and creator hash
	 */
	public void deleteRoom(String sessionCode, String creatorHash) {
		log.info("Deleting room with sessionCode '{}'", sessionCode);

		RoomEntity entity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
				.orElseThrow(() -> new RoomNotFoundException("not found"));

		repository.delete(entity);
		log.info("Room '{}' deleted successfully", sessionCode);
	}

	/**
	 * Draws a number in a room, authenticated by creator hash.
	 * <p>
	 * Validates the creator via session code and hash, then checks that the number
	 * is within the valid range for the active game type and has not already been drawn.
	 * Persists the update and returns the player view of the updated room for broadcasting
	 * to connected clients.
	 * </p>
	 *
	 * @param sessionCode the public session code of the room
	 * @param creatorHash the creator's authentication hash
	 * @param number      the bingo number to draw; must be within [{@code numberLabelMapper.getMinNumber()},
	 *                    {@code numberLabelMapper.getMaxNumber()}] and not yet drawn
	 * @return a {@link RoomDTO} in player view (without {@code creatorHash}) for broadcasting
	 * @throws RoomNotFoundException    if no room matches the given session code and creator hash
	 * @throws IllegalArgumentException if the number is out of range or already drawn
	 */
	public RoomDTO drawNumber(String sessionCode, String creatorHash, int number) {
		log.info("Drawing number {} in room '{}'", number, sessionCode);

		RoomEntity entity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
				.orElseThrow(() -> new RoomNotFoundException("Room not found."));

		validateDrawnNumber(number, entity);

		entity.addDrawnNumber(number);
		repository.save(entity);

		log.info("Number {} drawn in room '{}'", number, sessionCode);
		return RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
	}

	/**
	 * Validates that a number is within the active game's valid range and has not already been drawn.
	 *
	 * @param number the number to validate
	 * @param entity the room in which the number is being drawn
	 * @throws IllegalArgumentException if the number is out of range or already drawn
	 */
	private void validateDrawnNumber(int number, RoomEntity entity) {
		int min = numberLabelMapper.getMinNumber();
		int max = numberLabelMapper.getMaxNumber();
		if (number < min || number > max) {
			throw new IllegalArgumentException(
				"Drawn number must be between " + min + " and " + max + ", got: " + number
			);
		}
		if (entity.getDrawnNumbers().contains(number)) {
			throw new IllegalArgumentException(
				"Number " + number + " has already been drawn in this room"
			);
		}
	}
}
