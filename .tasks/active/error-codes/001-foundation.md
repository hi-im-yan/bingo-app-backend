# 001 — Foundation: ErrorCode Enum, ErrorResponse, BingoException Hierarchy

## What to build
Create the error code enum, new error response records, and refactor the exception hierarchy so every throwable error carries a machine-readable code. This is the foundation all other tasks depend on.

## Acceptance Criteria
- [ ] `ErrorCode` enum exists with all 16 codes from the spec
- [ ] `ErrorResponse` record has `status`, `code`, `message`, and optional `fields`
- [ ] `FieldError` record has `field` and `code` (String fields)
- [ ] `BingoException` abstract base class carries `ErrorCode` + message
- [ ] `ConflictException` extends `BingoException` (not RuntimeException)
- [ ] `RoomNotFoundException` extends `BingoException` (not RuntimeException)
- [ ] `BadRequestException` extends `BingoException` — new class for 400 errors
- [ ] Existing code still compiles (exceptions keep same constructor signatures as bridge)
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE

| File | Package | Purpose |
|------|---------|---------|
| `ErrorCode.java` | `com.yanajiki.application.bingoapp.exception` | Enum with all error codes |
| `ErrorResponse.java` | `com.yanajiki.application.bingoapp.api.response` | Error response record |
| `FieldError.java` | `com.yanajiki.application.bingoapp.api.response` | Per-field validation error record |
| `BingoException.java` | `com.yanajiki.application.bingoapp.exception` | Abstract base exception |
| `BadRequestException.java` | `com.yanajiki.application.bingoapp.exception` | 400-level exception replacing IllegalArgumentException/IllegalStateException |

### Files to MODIFY

| File | Change |
|------|--------|
| `ConflictException.java` | Extend `BingoException` instead of `RuntimeException`, keep `@ResponseStatus` |
| `RoomNotFoundException.java` | Extend `BingoException` instead of `RuntimeException`, keep `@ResponseStatus` |

### Files to READ (for patterns — do NOT modify)

| File | What to copy |
|------|-------------|
| `src/main/java/com/yanajiki/application/bingoapp/api/response/ApiResponse.java` | Record pattern |
| `src/main/java/com/yanajiki/application/bingoapp/exception/ConflictException.java` | Current exception shape |
| `src/main/java/com/yanajiki/application/bingoapp/exception/RoomNotFoundException.java` | Current exception shape |

### Implementation Details

**ErrorCode enum** — all values:
```java
public enum ErrorCode {
	// Room
	ROOM_NOT_FOUND,
	ROOM_NAME_TAKEN,

	// Draw
	DRAW_MODE_MISMATCH,
	NUMBER_OUT_OF_RANGE,
	NUMBER_ALREADY_DRAWN,
	ALL_NUMBERS_DRAWN,
	NO_NUMBERS_DRAWN,

	// Tiebreaker
	TIEBREAK_INVALID_PLAYER_COUNT,
	TIEBREAK_NOT_ENOUGH_NUMBERS,
	TIEBREAK_ALREADY_ACTIVE,
	TIEBREAK_NOT_ACTIVE,
	TIEBREAK_INVALID_SLOT,
	TIEBREAK_SLOT_ALREADY_DRAWN,
	TIEBREAK_NO_NUMBERS_REMAINING,

	// Player
	PLAYER_NAME_TAKEN,

	// Generic
	VALIDATION_ERROR,
	INTERNAL_ERROR
}
```

**BingoException** — abstract base:
```java
@Getter
public abstract class BingoException extends RuntimeException {
	private final ErrorCode errorCode;

	protected BingoException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
```

**BadRequestException:**
```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends BingoException {
	public BadRequestException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
```

**ConflictException** — update to extend BingoException:
```java
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends BingoException {
	public ConflictException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
```

**RoomNotFoundException** — update to extend BingoException:
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RoomNotFoundException extends BingoException {
	public RoomNotFoundException(ErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
```

**ErrorResponse record:**
```java
public record ErrorResponse(
	int status,
	String code,
	String message,
	@JsonInclude(JsonInclude.Include.NON_NULL)
	List<FieldError> fields
) {
	public ErrorResponse(int status, String code, String message) {
		this(status, code, message, null);
	}
}
```

**FieldError record:**
```java
public record FieldError(String field, String code) {}
```

**IMPORTANT:** When modifying `ConflictException` and `RoomNotFoundException`, you MUST also update every call site that constructs these exceptions. Search for `new ConflictException(` and `new RoomNotFoundException(` across the entire codebase and update each to pass the appropriate `ErrorCode` as the first argument. The files that construct these exceptions are:
- `RoomService.java` — multiple `new RoomNotFoundException(...)` and `new ConflictException(...)` calls
- `TiebreakService.java` — multiple `new RoomNotFoundException(...)` calls

Update ALL of them to include the `ErrorCode.ROOM_NOT_FOUND`, `ErrorCode.ROOM_NAME_TAKEN`, or `ErrorCode.PLAYER_NAME_TAKEN` as appropriate. Do NOT leave any constructor calls with the old single-argument signature.

### Conventions (from project CLAUDE.md)
- Tabs for indentation, camelCase naming
- Lombok (`@Getter`) for boilerplate
- Javadoc on all classes
- Records for DTOs
- Package: exceptions in `exception/`, responses in `api/response/`

## TDD Sequence
1. Create `ErrorCode` enum — no test needed (enum)
2. Create `BingoException`, `BadRequestException` — no test needed (thin wrappers)
3. Create `ErrorResponse`, `FieldError` records — no test needed (records)
4. Modify `ConflictException`, `RoomNotFoundException` — update ALL call sites in services
5. Run `mvn test` — all existing tests must still pass

## Done Definition
All files created/modified. `mvn test` passes. No compilation errors.
