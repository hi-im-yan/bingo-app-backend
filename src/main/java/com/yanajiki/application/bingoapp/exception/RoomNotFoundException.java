package com.yanajiki.application.bingoapp.exception;

/**
 * Exception thrown when a requested bingo room cannot be found.
 * Maps to HTTP 404 NOT FOUND.
 */
public class RoomNotFoundException extends BingoException {

	public RoomNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
