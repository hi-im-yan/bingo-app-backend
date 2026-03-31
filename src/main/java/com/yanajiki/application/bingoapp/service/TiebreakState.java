package com.yanajiki.application.bingoapp.service;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory state for a single tiebreaker session within a bingo room.
 * <p>
 * Tracks which slots have drawn and what number each received.
 * A room may have multiple sequential tiebreakers over the course of a game,
 * but only one active at a time. Each tiebreaker produces exactly one winner
 * (the slot with the highest drawn number).
 * </p>
 * <p>
 * Not persisted to the database — held in a {@code ConcurrentHashMap} keyed
 * by session code in {@link TiebreakService}.
 * </p>
 */
@Getter
public class TiebreakState {

	/** Number of players (slots) competing in this tiebreaker. */
	private final int playerCount;

	/** Slot → drawn number, in insertion order. */
	private final Map<Integer, Integer> draws = new LinkedHashMap<>();

	/**
	 * Creates a new tiebreaker state for the given number of players.
	 *
	 * @param playerCount number of contestants (slots 1 through playerCount)
	 */
	public TiebreakState(int playerCount) {
		this.playerCount = playerCount;
	}

	/**
	 * Records a draw for the given slot.
	 *
	 * @param slot   the 1-based slot index
	 * @param number the randomly drawn number
	 */
	public void addDraw(int slot, int number) {
		draws.put(slot, number);
	}

	/**
	 * Checks whether the given slot has already drawn.
	 *
	 * @param slot the 1-based slot index
	 * @return {@code true} if the slot has a recorded draw
	 */
	public boolean isSlotDrawn(int slot) {
		return draws.containsKey(slot);
	}

	/**
	 * Returns {@code true} when all slots have drawn.
	 *
	 * @return whether the tiebreaker is complete
	 */
	public boolean isComplete() {
		return draws.size() == playerCount;
	}

	/**
	 * Returns the slot that drew the highest number.
	 * <p>
	 * On a tie, the first slot (by insertion order) wins.
	 * </p>
	 *
	 * @return the 1-based winning slot index
	 * @throws java.util.NoSuchElementException if no draws have been recorded
	 */
	public int getWinnerSlot() {
		return draws.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.orElseThrow()
			.getKey();
	}

	/**
	 * Returns the current tiebreaker status.
	 *
	 * @return {@code "STARTED"} if no draws, {@code "FINISHED"} if all drawn, {@code "IN_PROGRESS"} otherwise
	 */
	public String getStatus() {
		if (draws.isEmpty()) return "STARTED";
		return isComplete() ? "FINISHED" : "IN_PROGRESS";
	}
}
