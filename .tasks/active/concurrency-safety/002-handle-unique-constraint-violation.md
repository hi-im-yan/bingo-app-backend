# 002 — Handle DataIntegrityViolationException for Room Name Uniqueness

## What to build
Catch `DataIntegrityViolationException` thrown when a duplicate room name violates the DB unique constraint, and return a clean 409 Conflict instead of a 500 Internal Server Error.

## Acceptance Criteria
- [ ] `DataIntegrityViolationException` handled in `GlobalExceptionHandler`
- [ ] Returns HTTP 409 with meaningful error message (e.g., "A room with this name already exists")
- [ ] Unit test for the new handler method
- [ ] Existing tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `GlobalExceptionHandler.java` | Add `@ExceptionHandler(DataIntegrityViolationException.class)` method returning 409 |

### Implementation Details

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ApiResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
	log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
	return ResponseEntity
		.status(HttpStatus.CONFLICT)
		.body(new ApiResponse("A resource with the given unique identifier already exists."));
}
```

Use a generic message rather than leaking DB constraint names. Log at WARN level (not ERROR — this is a user-caused conflict, not a system error).

### Conventions
- Follow existing handler pattern in GlobalExceptionHandler (ApiResponse wrapper, @ExceptionHandler annotation)
- Import `org.springframework.dao.DataIntegrityViolationException`

## TDD Sequence
1. Write unit test: mock a `DataIntegrityViolationException` being thrown, assert 409 + message
2. Add the handler to `GlobalExceptionHandler`
3. Run `mvn test`

## Done Definition
Duplicate room name creation returns 409 instead of 500. Handler is tested. No regressions.
