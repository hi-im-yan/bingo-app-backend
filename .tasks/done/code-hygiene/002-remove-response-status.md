# 002 — Remove @ResponseStatus from Custom Exceptions

## What to build
Remove `@ResponseStatus` annotations from `ConflictException` and `RoomNotFoundException`. These are dead code — `GlobalExceptionHandler` handles both exceptions explicitly and sets the status code there.

## Acceptance Criteria
- [ ] `@ResponseStatus` removed from `ConflictException`
- [ ] `@ResponseStatus` removed from `RoomNotFoundException`
- [ ] Unused `@ResponseStatus` / `HttpStatus` imports removed
- [ ] All tests pass (`mvn test`) — behavior unchanged since GlobalExceptionHandler controls status codes

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `ConflictException.java` | Remove `@ResponseStatus(HttpStatus.CONFLICT)` and unused imports |
| `RoomNotFoundException.java` | Remove `@ResponseStatus(HttpStatus.NOT_FOUND)` and unused imports |

## Done Definition
No `@ResponseStatus` on custom exceptions. GlobalExceptionHandler remains the single source of truth for HTTP status mapping. Tests green.
