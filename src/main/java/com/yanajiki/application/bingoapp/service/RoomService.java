package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.form.CreateRoomForm;
import com.yanajiki.application.bingoapp.api.response.NumberCorrectionDTO;
import com.yanajiki.application.bingoapp.api.response.PlayerDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.database.PlayerEntity;
import com.yanajiki.application.bingoapp.database.PlayerRepository;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.ConflictException;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.game.DrawMode;
import com.yanajiki.application.bingoapp.game.NumberLabelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.IntStream;

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
	private final PlayerRepository playerRepository;

	/**
	 * Creates a new bingo room with the given form data.
	 * <p>
	 * Validates that no room with the same name already exists, then creates
	 * and persists a new {@link RoomEntity}. Returns the creator view of the
	 * room, which includes the {@code creatorHash} needed for administrative actions.
	 * </p>
	 * <p>
	 * If {@code drawMode} is not specified in the form (null), it defaults to {@link DrawMode#MANUAL}.
	 * </p>
	 *
	 * @param form the creation form containing name, description, and optional draw mode
	 * @return a {@link RoomDTO} with full creator data, including {@code creatorHash} and {@code drawMode}
	 * @throws ConflictException if a room with the same name already exists
	 */
	public RoomDTO createRoom(CreateRoomForm form) {
		log.info("Creating room with name '{}'", form.getName());

		repository.findByName(form.getName())
				.ifPresent(existing -> {
					throw new ConflictException("Room already exists.");
				});

		DrawMode mode = form.getDrawMode() != null ? form.getDrawMode() : DrawMode.MANUAL;
		RoomEntity entity = RoomEntity.createEntityObject(form.getName(), form.getDescription(), mode);
		RoomEntity saved = repository.save(entity);

		log.info("Room created successfully with sessionCode '{}' and drawMode '{}'", saved.getSessionCode(), mode);
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

		if (entity.getDrawMode() != DrawMode.MANUAL) {
			throw new IllegalArgumentException("This room uses automatic draw mode");
		}

		validateDrawnNumber(number, entity);

		entity.addDrawnNumber(number);
		repository.save(entity);

		log.info("Number {} drawn in room '{}'", number, sessionCode);
		return RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
	}

	/**
	 * Corrects the last drawn number in a MANUAL mode room.
	 * <p>
	 * Validates the creator, ensures the room is in MANUAL mode, checks that at least one number
	 * has been drawn, and that the new number is within range and not already drawn.
	 * Replaces the last element in the drawn numbers list and persists the change.
	 * </p>
	 *
	 * @param sessionCode the public session code of the room
	 * @param creatorHash the creator's authentication hash
	 * @param newNumber   the corrected number to replace the last drawn number
	 * @return a {@link CorrectionResult} containing the updated player-view DTO and correction notification
	 * @throws RoomNotFoundException    if no room matches the given session code and creator hash
	 * @throws IllegalArgumentException if the room is not MANUAL, or the new number is invalid
	 * @throws IllegalStateException    if no numbers have been drawn yet
	 */
	public CorrectionResult correctLastNumber(String sessionCode, String creatorHash, int newNumber) {
		log.info("Correcting last number in room '{}' to {}", sessionCode, newNumber);

		RoomEntity entity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
				.orElseThrow(() -> new RoomNotFoundException("Room not found."));

		if (entity.getDrawMode() != DrawMode.MANUAL) {
			throw new IllegalArgumentException("Number correction is only available for manual draw mode rooms");
		}

		List<Integer> drawnNumbers = entity.getDrawnNumbers();
		if (drawnNumbers.isEmpty()) {
			throw new IllegalStateException("No numbers have been drawn yet");
		}

		int oldNumber = drawnNumbers.get(drawnNumbers.size() - 1);
		drawnNumbers.remove(drawnNumbers.size() - 1);

		validateDrawnNumber(newNumber, entity);

		drawnNumbers.add(newNumber);
		repository.save(entity);

		String oldLabel = numberLabelMapper.toLabel(oldNumber);
		String newLabel = numberLabelMapper.toLabel(newNumber);

		log.info("Corrected last number in room '{}': {} -> {}", sessionCode, oldLabel, newLabel);

		RoomDTO roomDTO = RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
		NumberCorrectionDTO correctionDTO = NumberCorrectionDTO.of(oldNumber, oldLabel, newNumber, newLabel);

		return new CorrectionResult(roomDTO, correctionDTO);
	}

	/**
	 * Draws a random number from the remaining pool for automatic draw mode rooms.
	 * <p>
	 * Selects a random number from the set of numbers in [{@code numberLabelMapper.getMinNumber()},
	 * {@code numberLabelMapper.getMaxNumber()}] that have not yet been drawn. Persists the update
	 * and returns the player view for broadcasting to connected clients.
	 * </p>
	 *
	 * @param sessionCode the public session code of the room
	 * @param creatorHash the creator's authentication hash
	 * @return a {@link RoomDTO} in player view (without {@code creatorHash}) with the new number included
	 * @throws RoomNotFoundException     if no room matches the given session code and creator hash
	 * @throws IllegalArgumentException  if the room is not in {@link DrawMode#AUTOMATIC} draw mode
	 * @throws IllegalStateException     if all numbers in the pool have already been drawn
	 */
	public RoomDTO drawRandomNumber(String sessionCode, String creatorHash) {
		log.info("Drawing random number in room '{}'", sessionCode);

		RoomEntity entity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
				.orElseThrow(() -> new RoomNotFoundException("Room not found"));

		if (entity.getDrawMode() != DrawMode.AUTOMATIC) {
			throw new IllegalArgumentException("This room uses manual draw mode");
		}

		int number = selectRandomNumber(entity);
		entity.addDrawnNumber(number);
		repository.save(entity);

		log.info("Random number {} drawn in room '{}'", number, sessionCode);
		return RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
	}

	/**
	 * Registers a player in a bingo room.
	 * <p>
	 * Looks up the room by session code, validates that the player name is not already
	 * taken in that room, then persists the player and returns a {@link PlayerDTO}.
	 * </p>
	 *
	 * @param sessionCode the room's session code
	 * @param playerName  the player's display name
	 * @return {@link PlayerDTO} with the registered player's data
	 * @throws RoomNotFoundException if no room with the given session code exists
	 * @throws ConflictException     if the player name is already taken in this room
	 */
	public PlayerDTO joinRoom(String sessionCode, String playerName) {
		log.info("Player '{}' joining room '{}'", playerName, sessionCode);

		RoomEntity room = repository.findBySessionCode(sessionCode)
			.orElseThrow(() -> new RoomNotFoundException("Room not found with session code: " + sessionCode));

		if (playerRepository.existsByNameAndRoomEntity(playerName, room)) {
			throw new ConflictException("Player name '" + playerName + "' is already taken in this room");
		}

		PlayerEntity player = PlayerEntity.create(playerName, room);
		playerRepository.save(player);

		log.info("Player '{}' successfully joined room '{}'", playerName, sessionCode);
		return PlayerDTO.fromEntity(player);
	}

	/**
	 * Returns all players in a room. Creator-only operation.
	 * <p>
	 * Authenticates the request by looking up the room using both session code and creator hash.
	 * Returns the full player list as a list of {@link PlayerDTO} instances.
	 * </p>
	 *
	 * @param sessionCode the room's session code
	 * @param creatorHash the creator's authentication hash
	 * @return list of {@link PlayerDTO} for all players in the room; empty list if no players joined
	 * @throws RoomNotFoundException if no room matches the given session code and creator hash
	 */
	public List<PlayerDTO> getPlayersByRoom(String sessionCode, String creatorHash) {
		log.debug("Fetching player list for room '{}' by creator", sessionCode);

		RoomEntity room = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
			.orElseThrow(() -> new RoomNotFoundException("Room not found or invalid creator hash"));

		return playerRepository.findByRoomEntity(room).stream()
			.map(PlayerDTO::fromEntity)
			.toList();
	}

	/**
	 * Selects a random number from the pool of numbers not yet drawn in the given room.
	 * <p>
	 * Uses {@link SecureRandom} for selection, consistent with session code generation.
	 * </p>
	 *
	 * @param entity the room entity whose drawn numbers are checked against the full pool
	 * @return a randomly selected undrawn number
	 * @throws IllegalStateException if no numbers remain in the pool
	 */
	private int selectRandomNumber(RoomEntity entity) {
		List<Integer> remaining = IntStream.rangeClosed(
						numberLabelMapper.getMinNumber(), numberLabelMapper.getMaxNumber())
				.filter(n -> !entity.getDrawnNumbers().contains(n))
				.boxed()
				.toList();

		if (remaining.isEmpty()) {
			throw new IllegalStateException("All numbers have been drawn");
		}

		return remaining.get(new SecureRandom().nextInt(remaining.size()));
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
