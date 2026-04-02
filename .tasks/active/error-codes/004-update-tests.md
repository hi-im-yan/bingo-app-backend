# 004 — Update All Tests for Error Code Assertions

## What to build
Update existing unit and integration tests to assert the new exception types (`BadRequestException` instead of `IllegalArgumentException`/`IllegalStateException`) and verify the `code` field in error responses.

## Acceptance Criteria
- [ ] `RoomServiceTest` asserts `BadRequestException` where it previously asserted `IllegalArgumentException` or `IllegalStateException`
- [ ] `TiebreakServiceTest` — same
- [ ] `RoomControllerIntegrationTest` asserts `code` field in error responses
- [ ] `PlayerJoinIntegrationTest` asserts `code` field in error responses
- [ ] `NumberCorrectionIntegrationTest` — update if it asserts error types
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY

| File | Change |
|------|--------|
| `src/test/java/com/yanajiki/application/bingoapp/service/RoomServiceTest.java` | Change `IllegalArgumentException.class` → `BadRequestException.class`, `IllegalStateException.class` → `BadRequestException.class` |
| `src/test/java/com/yanajiki/application/bingoapp/service/TiebreakServiceTest.java` | Same pattern |
| `src/test/java/com/yanajiki/application/bingoapp/api/RoomControllerIntegrationTest.java` | Add `.body("code", equalTo("ROOM_NAME_TAKEN"))` etc. to error assertions |
| `src/test/java/com/yanajiki/application/bingoapp/api/PlayerJoinIntegrationTest.java` | Add `.body("code", ...)` assertions |
| `src/test/java/com/yanajiki/application/bingoapp/service/NumberCorrectionIntegrationTest.java` | Check and update if needed |
| `src/test/java/com/yanajiki/application/bingoapp/websocket/WebSocketControllerTest.java` | Check and update if needed |

### Files to READ (for context)

| File | Why |
|------|-----|
| All test files listed above | Read FIRST to understand current assertions |
| `src/main/java/com/yanajiki/application/bingoapp/exception/ErrorCode.java` | Correct enum names for assertions |
| `src/main/java/com/yanajiki/application/bingoapp/exception/BadRequestException.java` | New exception class for assertions |

### Implementation Details

**Unit test changes (RoomServiceTest, TiebreakServiceTest):**

Find every `assertThatThrownBy(...)` that expects `IllegalArgumentException.class` or `IllegalStateException.class` and change to `BadRequestException.class`. Also add `.hasFieldOrPropertyWithValue("errorCode", ErrorCode.XXX)` where useful.

Example:
```java
// Before
assertThatThrownBy(() -> roomService.drawNumber(code, hash, 99))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("must be between");

// After
assertThatThrownBy(() -> roomService.drawNumber(code, hash, 99))
    .isInstanceOf(BadRequestException.class)
    .hasMessageContaining("must be between");
```

**Integration test changes (RoomControllerIntegrationTest, PlayerJoinIntegrationTest):**

Add `.body("code", equalTo("XXX"))` assertions to existing error test cases.

Example:
```java
// Before
.then()
    .statusCode(409)
    .body("status", equalTo(409))
    .body("message", notNullValue());

// After
.then()
    .statusCode(409)
    .body("status", equalTo(409))
    .body("code", equalTo("ROOM_NAME_TAKEN"))
    .body("message", notNullValue());
```

**Mapping for integration tests:**

| Test scenario | Expected code |
|--------------|---------------|
| Create room — duplicate name | `ROOM_NAME_TAKEN` |
| Create room — blank name | `VALIDATION_ERROR` |
| Get room — not found | `ROOM_NOT_FOUND` |
| Delete room — not found | `ROOM_NOT_FOUND` |
| Delete room — no creator hash | `ROOM_NOT_FOUND` |
| Get players — not found | `ROOM_NOT_FOUND` |
| Join room — room not found | `ROOM_NOT_FOUND` |
| Join room — invalid creator hash | `ROOM_NOT_FOUND` |

### Conventions
- `@Nested` + `@DisplayName` organization
- AssertJ for unit tests, RestAssured for integration
- Tabs for indentation

## TDD Sequence
1. Read ALL test files to understand current assertions
2. Update unit tests (exception type changes)
3. Update integration tests (add `code` field assertions)
4. Run `mvn test` — all must pass

## Done Definition
All tests updated and green. Every error response assertion includes the `code` field.
