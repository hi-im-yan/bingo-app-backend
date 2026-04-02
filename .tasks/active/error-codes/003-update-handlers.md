# 003 — Update GlobalExceptionHandler + Create WebSocketErrorHandler

## What to build
Update `GlobalExceptionHandler` to return `ErrorResponse` (with code) instead of `ApiResponse`. Create a new `WebSocketErrorHandler` that catches exceptions in `@MessageMapping` methods and sends structured JSON error bodies to the client via STOMP.

## Acceptance Criteria
- [ ] `GlobalExceptionHandler` returns `ErrorResponse` with `code` field for all error types
- [ ] `BingoException` handler extracts `ErrorCode` from the exception
- [ ] `MethodArgumentNotValidException` handler returns `VALIDATION_ERROR` with `fields` array
- [ ] Catch-all handler returns `INTERNAL_ERROR`
- [ ] `WebSocketErrorHandler` exists as a `@ControllerAdvice` with `@MessageExceptionHandler`
- [ ] WebSocket errors send JSON to `/user/queue/errors` (user-specific destination)
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY

| File | Change |
|------|--------|
| `src/main/java/com/yanajiki/application/bingoapp/exception/GlobalExceptionHandler.java` | Return `ErrorResponse` instead of `ApiResponse`, add `BingoException` handler |

### Files to CREATE

| File | Package | Purpose |
|------|---------|---------|
| `WebSocketErrorHandler.java` | `com.yanajiki.application.bingoapp.websocket` | `@ControllerAdvice` with `@MessageExceptionHandler` for WS errors |

### Files to READ (for patterns)

| File | Why |
|------|-----|
| `src/main/java/com/yanajiki/application/bingoapp/exception/GlobalExceptionHandler.java` | Current handler structure |
| `src/main/java/com/yanajiki/application/bingoapp/api/response/ErrorResponse.java` | New response shape (created in 001) |
| `src/main/java/com/yanajiki/application/bingoapp/exception/BingoException.java` | Base exception (created in 001) |
| `src/main/java/com/yanajiki/application/bingoapp/exception/ErrorCode.java` | Enum values |
| `src/main/java/com/yanajiki/application/bingoapp/websocket/WebSocketController.java` | Current WS controller pattern |

### Implementation Details

**GlobalExceptionHandler — updated structure:**

Replace every handler to return `ErrorResponse` instead of `ApiResponse`. Consolidate exception handling:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles all BingoException subtypes (ConflictException, RoomNotFoundException, BadRequestException).
	 * Extracts the ErrorCode from the exception and maps to the appropriate HTTP status.
	 */
	@ExceptionHandler(BingoException.class)
	public ResponseEntity<ErrorResponse> handleBingoException(BingoException ex) {
		HttpStatus status = resolveStatus(ex);
		return ResponseEntity
			.status(status)
			.body(new ErrorResponse(status.value(), ex.getErrorCode().name(), ex.getMessage()));
	}

	/**
	 * Handles bean-validation failures, returning VALIDATION_ERROR with per-field details.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		// Build fields list from FieldErrors
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

	// Keep NoResourceFoundException handler → ROOM_NOT_FOUND code
	// Keep catch-all Exception handler → INTERNAL_ERROR code

	private HttpStatus resolveStatus(BingoException ex) {
		// Use @ResponseStatus annotation on the exception class
		ResponseStatus annotation = ex.getClass().getAnnotation(ResponseStatus.class);
		return annotation != null ? annotation.value() : HttpStatus.INTERNAL_SERVER_ERROR;
	}

	/**
	 * Maps Spring's validation annotation code to a short field-level code.
	 * e.g. "NotBlank" → "NOT_BLANK", "Size" → "SIZE", "Min" → "MIN", "Max" → "MAX", "NotNull" → "NOT_NULL"
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
```

**IMPORTANT for FieldError import:** The `FieldError` in the `fields` list is `com.yanajiki.application.bingoapp.api.response.FieldError` (our custom record). Spring's `org.springframework.validation.FieldError` is used only in the handler method internals. Use the fully-qualified name for Spring's FieldError to avoid ambiguity, OR alias via local variable.

**WebSocketErrorHandler:**

```java
/**
 * Catches exceptions thrown by @MessageMapping handlers and sends structured
 * JSON error responses to the caller's personal error queue.
 */
@ControllerAdvice
public class WebSocketErrorHandler {

	private static final Logger log = LoggerFactory.getLogger(WebSocketErrorHandler.class);

	/**
	 * Handles BingoException subtypes (BadRequestException, ConflictException, RoomNotFoundException)
	 * thrown during WebSocket message handling.
	 */
	@MessageExceptionHandler(BingoException.class)
	@SendToUser("/queue/errors")
	public ErrorResponse handleBingoException(BingoException ex) {
		log.warn("WebSocket error [{}]: {}", ex.getErrorCode(), ex.getMessage());
		HttpStatus status = resolveStatus(ex);
		return new ErrorResponse(status.value(), ex.getErrorCode().name(), ex.getMessage());
	}

	/**
	 * Catches any unexpected exception during WebSocket message handling.
	 */
	@MessageExceptionHandler(Exception.class)
	@SendToUser("/queue/errors")
	public ErrorResponse handleUnknown(Exception ex) {
		log.error("Unexpected WebSocket error", ex);
		return new ErrorResponse(500, ErrorCode.INTERNAL_ERROR.name(), "If the error persists, open a ticket.");
	}

	private HttpStatus resolveStatus(BingoException ex) {
		ResponseStatus annotation = ex.getClass().getAnnotation(ResponseStatus.class);
		return annotation != null ? annotation.value() : HttpStatus.INTERNAL_SERVER_ERROR;
	}
}
```

**IMPORTANT on @SendToUser:** This sends to `/user/{sessionId}/queue/errors`. The frontend must subscribe to `/user/queue/errors` (Spring auto-prepends `/user/{sessionId}`). For this to work, the WebSocket config must have the user destination prefix configured. Read `WebSocketConfig.java` — if `setUserDestinationPrefix` is not set, it defaults to `/user`, which is fine.

Also update `WebSocketConfig.java` to enable user destinations if not already done. The `enableSimpleBroker` call may need `/user` added, OR `setUserDestinationPrefix("/user")` added. Check the config and add if missing.

### Conventions
- Tabs for indentation, camelCase naming
- Javadoc on all public methods
- SLF4J for logging
- `@RestControllerAdvice` for REST, `@ControllerAdvice` for WS

## TDD Sequence
1. Read existing `GlobalExceptionHandler.java`
2. Update it to use `ErrorResponse` and `BingoException` handler
3. Create `WebSocketErrorHandler.java`
4. Update `WebSocketConfig.java` if needed for user destinations
5. Run `mvn test`

## Done Definition
Both handlers compile. REST errors include `code` field. WebSocket errors send structured JSON to `/user/queue/errors`. All tests pass.
