# 002 — Update Services to Use BingoException with ErrorCode

## What to build
Replace all `IllegalArgumentException` and `IllegalStateException` throws in RoomService, TiebreakService, and QrCodeService with `BadRequestException` carrying the appropriate `ErrorCode`. The custom exceptions (ConflictException, RoomNotFoundException) were already updated in task 001 with ErrorCode — verify they're being called with the right codes.

## Acceptance Criteria
- [ ] Zero `IllegalArgumentException` throws remain in service layer
- [ ] Zero `IllegalStateException` throws remain in service layer
- [ ] Every `BadRequestException` carries the correct `ErrorCode` per the mapping below
- [ ] Every `ConflictException` carries the correct `ErrorCode`
- [ ] Every `RoomNotFoundException` carries `ROOM_NOT_FOUND`
- [ ] Error messages remain unchanged (same strings as before)
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY

| File | Change |
|------|--------|
| `src/main/java/com/yanajiki/application/bingoapp/service/RoomService.java` | Replace IllegalArgumentException/IllegalStateException with BadRequestException |
| `src/main/java/com/yanajiki/application/bingoapp/service/TiebreakService.java` | Same |
| `src/main/java/com/yanajiki/application/bingoapp/service/QrCodeService.java` | Same |

### Files to READ (for context)

| File | Why |
|------|-----|
| `src/main/java/com/yanajiki/application/bingoapp/exception/ErrorCode.java` | To use correct enum values |
| `src/main/java/com/yanajiki/application/bingoapp/exception/BadRequestException.java` | Constructor signature |

### Error Code Mapping

**RoomService.java:**

| Current throw | ErrorCode | Message (keep as-is) |
|---------------|-----------|---------------------|
| `new IllegalArgumentException("This room uses automatic draw mode")` | `DRAW_MODE_MISMATCH` | same |
| `new IllegalArgumentException("This room uses manual draw mode")` | `DRAW_MODE_MISMATCH` | same |
| `new IllegalArgumentException("Drawn number must be between...")` | `NUMBER_OUT_OF_RANGE` | same |
| `new IllegalArgumentException("Number X has already been drawn...")` | `NUMBER_ALREADY_DRAWN` | same |
| `new IllegalArgumentException("Number correction is only available for manual draw mode rooms")` | `DRAW_MODE_MISMATCH` | same |
| `new IllegalStateException("All numbers have been drawn")` | `ALL_NUMBERS_DRAWN` | same |
| `new IllegalStateException("No numbers have been drawn yet")` | `NO_NUMBERS_DRAWN` | same |

**TiebreakService.java:**

| Current throw | ErrorCode | Message (keep as-is) |
|---------------|-----------|---------------------|
| `new IllegalArgumentException("Tiebreaker is only available for automatic draw mode rooms")` | `DRAW_MODE_MISMATCH` | same |
| `new IllegalArgumentException("Player count must be at least...")` | `TIEBREAK_INVALID_PLAYER_COUNT` | same |
| `new IllegalArgumentException("Player count (...) exceeds available numbers...")` | `TIEBREAK_NOT_ENOUGH_NUMBERS` | same |
| `new IllegalArgumentException("Slot must be between 1 and...")` | `TIEBREAK_INVALID_SLOT` | same |
| `new IllegalArgumentException("Slot X has already drawn")` | `TIEBREAK_SLOT_ALREADY_DRAWN` | same |
| `new IllegalStateException("Room 'X' already has an active tiebreaker")` | `TIEBREAK_ALREADY_ACTIVE` | same |
| `new IllegalStateException("No active tiebreaker for room 'X'")` | `TIEBREAK_NOT_ACTIVE` | same |
| `new IllegalStateException("No numbers remaining for tiebreaker draw")` | `TIEBREAK_NO_NUMBERS_REMAINING` | same |

**QrCodeService.java:**

| Current throw | ErrorCode | Message (keep as-is) |
|---------------|-----------|---------------------|
| `new IllegalStateException("Failed to generate QR code...")` | `INTERNAL_ERROR` | same |

### Conventions
- Tabs for indentation
- Import `BadRequestException` from `com.yanajiki.application.bingoapp.exception`
- Import `ErrorCode` from same package
- Keep all existing error messages exactly as they are

## TDD Sequence
1. Read each service file
2. Replace each `new IllegalArgumentException(...)` with `new BadRequestException(ErrorCode.XXX, ...)`
3. Replace each `new IllegalStateException(...)` with `new BadRequestException(ErrorCode.XXX, ...)`
4. Run `mvn test` — existing tests that assert exception types will need updating (see task 004)

**Note:** Some unit tests may fail because they assert `IllegalArgumentException.class` or `IllegalStateException.class`. That's expected — task 004 will fix the tests. Just make sure the code compiles.

## Done Definition
All service throws use `BadRequestException` or other `BingoException` subtypes. Zero raw JDK exceptions thrown from service layer. Code compiles.
