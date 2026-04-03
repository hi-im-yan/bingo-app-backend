package com.yanajiki.application.bingoapp.exception;

import com.yanajiki.application.bingoapp.api.response.ErrorResponse;
import com.yanajiki.application.bingoapp.api.response.FieldError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

	@ExceptionHandler(BingoException.class)
	public ResponseEntity<ErrorResponse> handleBingoException(BingoException ex) {
		HttpStatus status = resolveStatus(ex);
		return ResponseEntity
				.status(status)
				.body(new ErrorResponse(status.value(), ex.getErrorCode().name(), ex.getMessage()));
	}

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

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
		int httpStatus = HttpStatus.NOT_FOUND.value();
		return ResponseEntity.status(httpStatus)
				.body(new ErrorResponse(httpStatus, ErrorCode.ROOM_NOT_FOUND.name(), ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
		log.error("UNKNOWN_ERROR:: {}", ex.getMessage(), ex);
		int httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
		return ResponseEntity.status(httpStatus)
				.body(new ErrorResponse(httpStatus, ErrorCode.INTERNAL_ERROR.name(), "If the error persists, open a ticket."));
	}

	private HttpStatus resolveStatus(BingoException ex) {
		if (ex instanceof RoomNotFoundException) return HttpStatus.NOT_FOUND;
		if (ex instanceof ConflictException) return HttpStatus.CONFLICT;
		if (ex instanceof BadRequestException) return HttpStatus.BAD_REQUEST;
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}

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
