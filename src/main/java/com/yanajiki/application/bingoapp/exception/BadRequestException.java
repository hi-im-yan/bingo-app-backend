package com.yanajiki.application.bingoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a request is semantically invalid (400 Bad Request).
 * <p>
 * Use this exception to replace ad-hoc {@link IllegalArgumentException} and
 * {@link IllegalStateException} throws when the error condition has a known
 * {@link ErrorCode}. The global exception handler maps this to HTTP 400.
 * </p>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends BingoException {

	/**
	 * Constructs a new {@code BadRequestException} with the given error code and message.
	 *
	 * @param errorCode machine-readable code identifying the error
	 * @param message   human-readable description of the error
	 */
	public BadRequestException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
