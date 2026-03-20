package com.yanajiki.application.bingoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation conflicts with the current state of a resource.
 * Maps to HTTP 409 CONFLICT.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

	/**
	 * Constructs a new ConflictException with the given message.
	 *
	 * @param message description of the conflict
	 */
	public ConflictException(String message) {
		super(message);
	}
}
