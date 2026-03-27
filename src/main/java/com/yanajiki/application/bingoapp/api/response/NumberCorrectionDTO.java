package com.yanajiki.application.bingoapp.api.response;

/**
 * Notification DTO broadcast to players when the game master corrects the last drawn number.
 * <p>
 * Contains both the raw numbers and their display labels (e.g., {@code "O-75"}, {@code "B-12"})
 * so the frontend can render a human-readable correction message without additional lookups.
 * </p>
 *
 * @param oldNumber the previously drawn number that was incorrect
 * @param oldLabel  the display label of the old number (e.g., {@code "O-75"})
 * @param newNumber the corrected number replacing the old one
 * @param newLabel  the display label of the new number (e.g., {@code "B-12"})
 * @param message   a human-readable correction message (e.g., {@code "GM changed O-75 to B-12"})
 */
public record NumberCorrectionDTO(
	int oldNumber,
	String oldLabel,
	int newNumber,
	String newLabel,
	String message
) {

	/**
	 * Factory method to create a correction notification from raw numbers and their labels.
	 *
	 * @param oldNumber the old (incorrect) number
	 * @param oldLabel  the display label for the old number
	 * @param newNumber the new (corrected) number
	 * @param newLabel  the display label for the new number
	 * @return a new {@link NumberCorrectionDTO} with all fields populated including a formatted message
	 */
	public static NumberCorrectionDTO of(int oldNumber, String oldLabel, int newNumber, String newLabel) {
		String message = "GM changed " + oldLabel + " to " + newLabel;
		return new NumberCorrectionDTO(oldNumber, oldLabel, newNumber, newLabel, message);
	}
}
