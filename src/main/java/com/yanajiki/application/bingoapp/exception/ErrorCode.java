package com.yanajiki.application.bingoapp.exception;

/**
 * Machine-readable error codes for all domain errors in the Bingo Room API.
 * <p>
 * Every {@link BingoException} carries one of these codes, allowing API consumers
 * to programmatically distinguish error conditions without parsing message strings.
 * </p>
 */
public enum ErrorCode {

	// Room
	/** The requested room does not exist. */
	ROOM_NOT_FOUND,
	/** A room with the same name already exists. */
	ROOM_NAME_TAKEN,

	// Draw
	/** The requested draw operation does not match the room's draw mode. */
	DRAW_MODE_MISMATCH,
	/** The number is outside the valid range for the active game type. */
	NUMBER_OUT_OF_RANGE,
	/** The number has already been drawn in this room. */
	NUMBER_ALREADY_DRAWN,
	/** All numbers in the pool have been drawn. */
	ALL_NUMBERS_DRAWN,
	/** No numbers have been drawn yet. */
	NO_NUMBERS_DRAWN,

	// Tiebreaker
	/** The requested player count for a tiebreaker is invalid. */
	TIEBREAK_INVALID_PLAYER_COUNT,
	/** There are not enough undrawn numbers to run a tiebreaker. */
	TIEBREAK_NOT_ENOUGH_NUMBERS,
	/** A tiebreaker is already active for this room. */
	TIEBREAK_ALREADY_ACTIVE,
	/** No tiebreaker is currently active for this room. */
	TIEBREAK_NOT_ACTIVE,
	/** The requested slot is out of range for the active tiebreaker. */
	TIEBREAK_INVALID_SLOT,
	/** The requested slot has already been drawn in the active tiebreaker. */
	TIEBREAK_SLOT_ALREADY_DRAWN,
	/** No numbers remain in the pool for a tiebreaker draw. */
	TIEBREAK_NO_NUMBERS_REMAINING,

	// Player
	/** The player name is already taken in this room. */
	PLAYER_NAME_TAKEN,

	// Generic
	/** One or more request fields failed validation. */
	VALIDATION_ERROR,
	/** An unexpected internal error occurred. */
	INTERNAL_ERROR
}
