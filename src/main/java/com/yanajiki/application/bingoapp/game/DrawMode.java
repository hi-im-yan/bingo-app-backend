package com.yanajiki.application.bingoapp.game;

/**
 * Defines the draw mode for a bingo room.
 * <p>
 * <ul>
 *   <li>{@link #MANUAL}: the creator selects a specific number to draw.</li>
 *   <li>{@link #AUTOMATIC}: the server randomly selects the next number from the remaining pool.</li>
 * </ul>
 * </p>
 */
public enum DrawMode {

	/**
	 * The room creator manually selects which number to draw each round.
	 */
	MANUAL,

	/**
	 * The server automatically and randomly draws the next available number.
	 */
	AUTOMATIC
}
