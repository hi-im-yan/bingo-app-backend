package com.yanajiki.application.bingoapp.exception;

import com.yanajiki.application.bingoapp.api.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * <p>
 * Centralises HTTP error response construction, removing the need for per-controller
 * {@code @ExceptionHandler} methods. Each handler maps a specific exception type to the
 * appropriate HTTP status and wraps the error detail in an {@link ApiResponse}.
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	/**
	 * Handles {@link ConflictException}, returning HTTP 409 CONFLICT.
	 *
	 * @param ex the conflict exception thrown by the service layer
	 * @return a {@link ResponseEntity} containing the error status and message
	 */
	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiResponse> handleConflict(ConflictException ex) {
		int httpStatus = HttpStatus.CONFLICT.value();
		return ResponseEntity.status(httpStatus).body(new ApiResponse(httpStatus, ex.getMessage()));
	}

	/**
	 * Handles {@link RoomNotFoundException}, returning HTTP 404 NOT FOUND.
	 *
	 * @param ex the not-found exception thrown when a room cannot be located
	 * @return a {@link ResponseEntity} containing the error status and message
	 */
	@ExceptionHandler(RoomNotFoundException.class)
	public ResponseEntity<ApiResponse> handleNotFound(RoomNotFoundException ex) {
		int httpStatus = HttpStatus.NOT_FOUND.value();
		return ResponseEntity.status(httpStatus).body(new ApiResponse(httpStatus, ex.getMessage()));
	}

	/**
	 * Handles {@link MethodArgumentNotValidException}, returning HTTP 400 BAD REQUEST.
	 * <p>
	 * Collects all field-level validation errors and joins them into a single message
	 * in the form {@code "fieldName: error message; ..."}.
	 * </p>
	 *
	 * @param ex the validation exception produced by Bean Validation
	 * @return a {@link ResponseEntity} containing the error status and aggregated field errors
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
		int httpStatus = HttpStatus.BAD_REQUEST.value();
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getDefaultMessage)
				.collect(Collectors.joining("; "));
		return ResponseEntity.status(httpStatus).body(new ApiResponse(httpStatus, message));
	}

	/**
	 * Handles {@link IllegalArgumentException}, returning HTTP 400 BAD REQUEST.
	 *
	 * @param ex the illegal argument exception
	 * @return a {@link ResponseEntity} containing the error status and message
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex) {
		int httpStatus = HttpStatus.BAD_REQUEST.value();
		return ResponseEntity.status(httpStatus).body(new ApiResponse(httpStatus, ex.getMessage()));
	}

	/**
	 * Catch-all handler for any unhandled {@link Exception}, returning HTTP 500 INTERNAL SERVER ERROR.
	 * <p>
	 * The full exception is logged at ERROR level. A generic message is returned to the client
	 * to avoid leaking internal details.
	 * </p>
	 *
	 * @param ex the unexpected exception
	 * @return a {@link ResponseEntity} with a generic 500 error body
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse> handleUnknown(Exception ex) {
		log.error("UNKNOWN_ERROR:: {}", ex.getMessage(), ex);
		int httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
		return ResponseEntity.status(httpStatus).body(new ApiResponse(httpStatus, "If the error persists, open a ticket."));
	}
}
