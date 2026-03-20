package com.yanajiki.application.bingoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested bingo room cannot be found.
 * Maps to HTTP 404 NOT FOUND.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RoomNotFoundException extends RuntimeException {

	/**
	 * Constructs a new RoomNotFoundException with the given message.
	 *
	 * @param message description of the missing resource
	 */
	public RoomNotFoundException(String message) {
		super(message);
	}
}
