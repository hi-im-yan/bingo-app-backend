package com.yanajiki.application.bingoapp.exception;

import lombok.Getter;

/**
 * Abstract base exception for all domain errors in the Bingo Room API.
 * <p>
 * Every concrete exception in the hierarchy carries a machine-readable
 * {@link ErrorCode} in addition to a human-readable message. The global
 * exception handler uses the error code to populate structured
 * {@code ErrorResponse} payloads returned to API consumers.
 * </p>
 */
@Getter
public abstract class BingoException extends RuntimeException {

	/** Machine-readable code identifying the error condition. */
	private final ErrorCode errorCode;

	/**
	 * Constructs a new {@code BingoException} with the given error code and message.
	 *
	 * @param errorCode machine-readable code identifying the error
	 * @param message   human-readable description of the error
	 */
	protected BingoException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
