package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.response.TiebreakDTO;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.game.DrawMode;
import com.yanajiki.application.bingoapp.game.NumberLabelMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Service managing tiebreaker lifecycle within bingo rooms.
 * <p>
 * A room may have multiple sequential tiebreakers over the course of a game,
 * but only one active at a time. Each tiebreaker produces exactly one winner
 * (the slot with the highest drawn number). Tiebreaker draws are ephemeral —
 * they come from the undrawn pool but are not added to the room's drawnNumbers.
 * </p>
 * <p>
 * State is held in-memory via a {@link ConcurrentHashMap} keyed by session code.
 * State is cleared after a tiebreaker finishes, allowing a new one to start.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TiebreakService {

	private final RoomRepository roomRepository;
	private final NumberLabelMapper numberLabelMapper;
	private final ConcurrentHashMap<String, TiebreakState> activeTiebreaks = new ConcurrentHashMap<>();

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final int MIN_PLAYERS = 2;
	private static final int MAX_PLAYERS = 6;

	/**
	 * Starts a tiebreaker for the given room.
	 * <p>
	 * Validates that the room exists, is in AUTOMATIC mode, the player count is within range,
	 * and no tiebreaker is already active for this room.
	 * </p>
	 *
	 * @param sessionCode the room's session code
	 * @param creatorHash the creator's authentication hash
	 * @param playerCount number of contestants (2–6)
	 * @return a {@link TiebreakDTO} with status {@code STARTED}
	 * @throws RoomNotFoundException    if the room is not found or hash is invalid
	 * @throws IllegalArgumentException if the room is not AUTOMATIC or player count is out of range
	 * @throws IllegalStateException    if a tiebreaker is already active for this room
	 */
	public TiebreakDTO startTiebreak(String sessionCode, String creatorHash, int playerCount) {
		log.info("Starting tiebreaker for room '{}' with {} players", sessionCode, playerCount);

		RoomEntity entity = roomRepository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
			.orElseThrow(() -> new RoomNotFoundException("Room not found"));

		if (entity.getDrawMode() != DrawMode.AUTOMATIC) {
			throw new IllegalArgumentException("Tiebreaker is only available for automatic draw mode rooms");
		}

		if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
			throw new IllegalArgumentException(
				"Player count must be between " + MIN_PLAYERS + " and " + MAX_PLAYERS + ", got: " + playerCount);
		}

		if (activeTiebreaks.containsKey(sessionCode)) {
			throw new IllegalStateException("Room '" + sessionCode + "' already has an active tiebreaker");
		}

		TiebreakState state = new TiebreakState(playerCount);
		activeTiebreaks.put(sessionCode, state);

		log.info("Tiebreaker started for room '{}' with {} slots", sessionCode, playerCount);
		return TiebreakDTO.from(state, numberLabelMapper);
	}

	/**
	 * Draws a random undrawn number for the given slot in the active tiebreaker.
	 * <p>
	 * The number is selected from the room's undrawn pool, also excluding numbers
	 * already drawn in this tiebreaker. The number is <em>not</em> added to the room's
	 * drawnNumbers — tiebreaker draws are ephemeral.
	 * </p>
	 *
	 * @param sessionCode the room's session code
	 * @param creatorHash the creator's authentication hash
	 * @param slot        the 1-based slot index to draw for
	 * @return an updated {@link TiebreakDTO} reflecting the new draw
	 * @throws RoomNotFoundException    if the room is not found or hash is invalid
	 * @throws IllegalStateException    if no tiebreaker is active for this room
	 * @throws IllegalArgumentException if the slot is out of range or already drawn
	 */
	public TiebreakDTO drawForSlot(String sessionCode, String creatorHash, int slot) {
		log.info("Tiebreaker draw for room '{}', slot {}", sessionCode, slot);

		RoomEntity entity = roomRepository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
			.orElseThrow(() -> new RoomNotFoundException("Room not found"));

		TiebreakState state = activeTiebreaks.get(sessionCode);
		if (state == null) {
			throw new IllegalStateException("No active tiebreaker for room '" + sessionCode + "'");
		}

		if (slot < 1 || slot > state.getPlayerCount()) {
			throw new IllegalArgumentException(
				"Slot must be between 1 and " + state.getPlayerCount() + ", got: " + slot);
		}

		if (state.isSlotDrawn(slot)) {
			throw new IllegalArgumentException("Slot " + slot + " has already drawn");
		}

		int number = selectTiebreakNumber(entity, state);
		state.addDraw(slot, number);

		log.info("Tiebreaker slot {} drew number {} in room '{}'", slot, number, sessionCode);
		return TiebreakDTO.from(state, numberLabelMapper);
	}

	/**
	 * Removes active tiebreaker state for a room.
	 * No-op if no tiebreaker is active.
	 *
	 * @param sessionCode the room's session code
	 */
	public void clearTiebreak(String sessionCode) {
		activeTiebreaks.remove(sessionCode);
		log.debug("Tiebreaker cleared for room '{}'", sessionCode);
	}

	/**
	 * Checks if a room has an active tiebreaker.
	 *
	 * @param sessionCode the room's session code
	 * @return {@code true} if a tiebreaker is active
	 */
	public boolean hasActiveTiebreak(String sessionCode) {
		return activeTiebreaks.containsKey(sessionCode);
	}

	/**
	 * Selects a random number from the pool of numbers not drawn in the room
	 * and not already drawn in this tiebreaker.
	 *
	 * @param entity the room entity
	 * @param state  the active tiebreaker state
	 * @return a randomly selected number
	 * @throws IllegalStateException if no numbers remain in the pool
	 */
	private int selectTiebreakNumber(RoomEntity entity, TiebreakState state) {
		List<Integer> remaining = IntStream.rangeClosed(
				numberLabelMapper.getMinNumber(), numberLabelMapper.getMaxNumber())
			.filter(n -> !entity.getDrawnNumbers().contains(n))
			.filter(n -> !state.getDraws().containsValue(n))
			.boxed()
			.toList();

		if (remaining.isEmpty()) {
			throw new IllegalStateException("No numbers remaining for tiebreaker draw");
		}

		return remaining.get(SECURE_RANDOM.nextInt(remaining.size()));
	}
}
