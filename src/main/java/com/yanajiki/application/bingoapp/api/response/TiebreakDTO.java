package com.yanajiki.application.bingoapp.api.response;

import com.yanajiki.application.bingoapp.game.NumberLabelMapper;
import com.yanajiki.application.bingoapp.service.TiebreakState;

import java.util.List;

/**
 * Immutable DTO broadcast to clients during a tiebreaker session.
 * <p>
 * Contains the current status, player count, individual draw entries,
 * and the winning slot (set only when status is {@code FINISHED}).
 * </p>
 *
 * @param status      {@code "STARTED"}, {@code "IN_PROGRESS"}, or {@code "FINISHED"}
 * @param playerCount number of contestants in this tiebreaker
 * @param draws       list of completed draw entries in slot order
 * @param winnerSlot  the 1-based winning slot, or {@code null} if not yet finished
 */
public record TiebreakDTO(
	String status,
	int playerCount,
	List<TiebreakDrawEntry> draws,
	Integer winnerSlot
) {

	/**
	 * A single draw entry within a tiebreaker.
	 *
	 * @param slot   the 1-based player slot
	 * @param number the raw drawn number
	 * @param label  the display label (e.g., {@code "N-42"})
	 */
	public record TiebreakDrawEntry(int slot, int number, String label) {}

	/**
	 * Builds a {@link TiebreakDTO} from in-memory tiebreaker state.
	 *
	 * @param state  the active tiebreaker state
	 * @param mapper the number-to-label mapper for the active game type
	 * @return a fully populated DTO reflecting the current tiebreaker state
	 */
	public static TiebreakDTO from(TiebreakState state, NumberLabelMapper mapper) {
		List<TiebreakDrawEntry> entries = state.getDraws().entrySet().stream()
			.map(e -> new TiebreakDrawEntry(e.getKey(), e.getValue(), mapper.toLabel(e.getValue())))
			.toList();

		Integer winner = state.isComplete() ? state.getWinnerSlot() : null;

		return new TiebreakDTO(state.getStatus(), state.getPlayerCount(), entries, winner);
	}
}
