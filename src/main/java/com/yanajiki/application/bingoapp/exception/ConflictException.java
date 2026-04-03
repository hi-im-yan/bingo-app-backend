package com.yanajiki.application.bingoapp.exception;

/**
 * Exception thrown when an operation conflicts with the current state of a resource.
 * Maps to HTTP 409 CONFLICT.
 */
public class ConflictException extends BingoException {

	public ConflictException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
