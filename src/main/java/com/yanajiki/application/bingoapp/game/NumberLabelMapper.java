package com.yanajiki.application.bingoapp.game;

/**
 * Abstraction for converting drawn numbers into human-readable display labels,
 * and for defining the valid number range for a given bingo-like game type.
 * <p>
 * Implementations encode the rules of a specific game variant — for example,
 * standard 75-ball bingo maps numbers to BINGO letter columns (e.g., 42 → "N-42"),
 * while a future 90-ball variant might use a different scheme entirely.
 * </p>
 * <p>
 * This interface is also the single source of truth for the valid number range,
 * so validation logic in the service layer can delegate to the active mapper
 * rather than relying on hardcoded constants.
 * </p>
 */
public interface NumberLabelMapper {

	/**
	 * Converts a drawn number to its display label for the given game type.
	 * <p>
	 * For example, in standard 75-ball bingo, {@code toLabel(42)} returns {@code "N-42"}.
	 * </p>
	 *
	 * @param number the drawn number to convert; should be within [{@link #getMinNumber()}, {@link #getMaxNumber()}]
	 * @return the display label string, never {@code null}
	 */
	String toLabel(int number);

	/**
	 * Returns the minimum valid number for this game type (inclusive).
	 *
	 * @return the minimum drawable number
	 */
	int getMinNumber();

	/**
	 * Returns the maximum valid number for this game type (inclusive).
	 *
	 * @return the maximum drawable number
	 */
	int getMaxNumber();
}
