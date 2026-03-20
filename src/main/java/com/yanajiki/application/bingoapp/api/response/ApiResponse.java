package com.yanajiki.application.bingoapp.api.response;

/**
 * Generic API response envelope used by exception handlers to communicate
 * HTTP status codes and human-readable error messages to callers.
 *
 * @param status  the HTTP status code (e.g. 404, 409, 500)
 * @param message a short, human-readable description of the outcome
 */
public record ApiResponse(int status, String message) {}
