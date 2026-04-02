package com.yanajiki.application.bingoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested bingo room cannot be found.
 * Maps to HTTP 404 NOT FOUND.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RoomNotFoundException extends BingoException {

	/**
	 * Constructs a new {@code RoomNotFoundException} with the given error code and message.
	 *
	 * @param errorCode machine-readable code identifying the error
	 * @param message   human-readable description of the missing resource
	 */
	public RoomNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
