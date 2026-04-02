package com.yanajiki.application.bingoapp.api.response;

/**
 * Represents a single field-level validation error within an {@link ErrorResponse}.
 * <p>
 * Used when one or more request fields fail validation so that API consumers can
 * identify exactly which field failed and why, without parsing the top-level message.
 * </p>
 *
 * @param field the name of the field that failed validation
 * @param code  a machine-readable code describing the validation failure
 */
public record FieldError(String field, String code) {}
