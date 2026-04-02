package com.yanajiki.application.bingoapp.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Structured error response body returned by the global exception handler.
 * <p>
 * Replaces the generic {@link ApiResponse} for error cases, adding a machine-readable
 * {@code code} field (mapped from {@link com.yanajiki.application.bingoapp.exception.ErrorCode})
 * and an optional list of per-field validation errors.
 * </p>
 *
 * @param status  the HTTP status code (e.g. 400, 404, 409)
 * @param code    machine-readable error code identifying the error condition
 * @param message human-readable description of the error
 * @param fields  optional list of field-level validation errors; {@code null} when not applicable
 */
public record ErrorResponse(
	int status,
	String code,
	String message,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	List<FieldError> fields
) {

	/**
	 * Convenience constructor for errors without field-level detail.
	 *
	 * @param status  the HTTP status code
	 * @param code    machine-readable error code
	 * @param message human-readable description
	 */
	public ErrorResponse(int status, String code, String message) {
		this(status, code, message, null);
	}
}
