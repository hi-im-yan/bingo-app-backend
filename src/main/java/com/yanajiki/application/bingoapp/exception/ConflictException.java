package com.yanajiki.application.bingoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation conflicts with the current state of a resource.
 * Maps to HTTP 409 CONFLICT.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends BingoException {

	/**
	 * Constructs a new {@code ConflictException} with the given error code and message.
	 *
	 * @param errorCode machine-readable code identifying the conflict
	 * @param message   human-readable description of the conflict
	 */
	public ConflictException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
