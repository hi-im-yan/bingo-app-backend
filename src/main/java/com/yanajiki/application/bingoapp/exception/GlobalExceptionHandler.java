package com.yanajiki.application.bingoapp.exception;

import com.yanajiki.application.bingoapp.api.response.ErrorResponse;
import com.yanajiki.application.bingoapp.api.response.FieldError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * <p>
 * Centralises HTTP error response construction, removing the need for per-controller
 * {@code @ExceptionHandler} methods. Each handler maps a specific exception type to the
 * appropriate HTTP status and wraps the error detail in an {@link ErrorResponse}.
 * </p>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	/**
	 * Handles all {@link BingoException} subtypes (ConflictException, RoomNotFoundException,
	 * BadRequestException), extracting the machine-readable {@link ErrorCode} from the exception.
	 *
	 * @param ex the bingo domain exception thrown by the service layer
	 * @return a {@link ResponseEntity} containing the structured error response
	 */
	@ExceptionHandler(BingoException.class)
	public ResponseEntity<ErrorResponse> handleBingoException(BingoException ex) {
		HttpStatus status = resolveStatus(ex);
		return ResponseEntity
				.status(status)
				.body(new ErrorResponse(status.value(), ex.getErrorCode().name(), ex.getMessage()));
	}

	/**
	 * Handles {@link MethodArgumentNotValidException}, returning HTTP 400 BAD REQUEST
	 * with per-field validation details.
	 * <p>
	 * Collects all field-level validation errors into a {@code fields} array and joins
	 * them into a single message in the form {@code "fieldName: error message; ..."}.
	 * </p>
	 *
	 * @param ex the validation exception produced by Bean Validation
	 * @return a {@link ResponseEntity} containing the error status, code, and field details
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		List<FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new FieldError(fe.getField(), mapValidationCode(fe)))
				.toList();

		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining("; "));

		return ResponseEntity
				.badRequest()
				.body(new ErrorResponse(400, ErrorCode.VALIDATION_ERROR.name(), message, fields));
	}

	/**
	 * Handles {@link NoResourceFoundException}, returning HTTP 404 NOT FOUND.
	 * <p>
	 * Prevents static resource requests (e.g. {@code /favicon.ico}) from being caught
	 * by the catch-all handler and logged as unexpected errors.
	 * </p>
	 *
	 * @param ex the no-resource-found exception thrown by Spring's resource handling
	 * @return a {@link ResponseEntity} containing the error status and message
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
		int httpStatus = HttpStatus.NOT_FOUND.value();
		return ResponseEntity.status(httpStatus)
				.body(new ErrorResponse(httpStatus, ErrorCode.ROOM_NOT_FOUND.name(), ex.getMessage()));
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
	public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
		log.error("UNKNOWN_ERROR:: {}", ex.getMessage(), ex);
		int httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
		return ResponseEntity.status(httpStatus)
				.body(new ErrorResponse(httpStatus, ErrorCode.INTERNAL_ERROR.name(), "If the error persists, open a ticket."));
	}

	/**
	 * Resolves the HTTP status from the {@link ResponseStatus} annotation on the exception class.
	 *
	 * @param ex the bingo exception
	 * @return the resolved HTTP status, or 500 if no annotation is present
	 */
	private HttpStatus resolveStatus(BingoException ex) {
		ResponseStatus annotation = ex.getClass().getAnnotation(ResponseStatus.class);
		return annotation != null ? annotation.value() : HttpStatus.INTERNAL_SERVER_ERROR;
	}

	/**
	 * Maps Spring's validation annotation code to a short field-level code.
	 *
	 * @param fe the Spring validation field error
	 * @return the mapped code string (e.g. "NOT_BLANK", "SIZE", "MIN", "MAX")
	 */
	private String mapValidationCode(org.springframework.validation.FieldError fe) {
		String code = fe.getCode();
		if (code == null) return "INVALID";
		return switch (code) {
			case "NotBlank" -> "NOT_BLANK";
			case "NotNull" -> "NOT_NULL";
			case "Size" -> "SIZE";
			case "Min" -> "MIN";
			case "Max" -> "MAX";
			default -> code.toUpperCase();
		};
	}
}
