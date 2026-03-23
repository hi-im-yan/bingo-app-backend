# 002 — RoomService.correctLastNumber() + Unit Tests

## What to build
Add a `correctLastNumber` method to `RoomService` that replaces the last drawn number in a MANUAL mode room. Returns both the updated `RoomDTO` (player view) and a `NumberCorrectionDTO` for the notification broadcast.

## Acceptance Criteria
- [ ] `RoomService.correctLastNumber(sessionCode, creatorHash, newNumber)` exists
- [ ] Returns a result containing both `RoomDTO` (player view) and `NumberCorrectionDTO`
- [ ] Validates: room exists, creator authenticated, MANUAL mode, drawn list not empty, new number in range, new number not already drawn
- [ ] Replaces last element in `drawnNumbers`, saves entity
- [ ] `NumberCorrectionDTO` contains correct old/new numbers, labels, and message
- [ ] Unit tests cover all validation branches
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Package/Path | What to change |
|------|-------------|----------------|
| `RoomService.java` | `com.yanajiki.application.bingoapp.service` | Add `correctLastNumber` method |

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `CorrectionResult.java` | `com.yanajiki.application.bingoapp.service` | Simple record to hold both RoomDTO and NumberCorrectionDTO |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomService.java` | Existing method patterns: auth lookup, validation, save, DTO return |
| `RoomServiceTest.java` | Mockito setup, `@Nested`/`@DisplayName`, stubStandardMapper(), buildForm() |
| `NumberCorrectionDTO.java` | The `of` factory method signature (from task 001) |
| `RoomEntity.java` | `drawnNumbers` is `List<Integer>` (mutable ArrayList), field access via getter |

### Implementation Details

**CorrectionResult.java** — simple wrapper:

```java
package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.response.NumberCorrectionDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;

/**
 * Holds the result of a number correction: the updated room state and the correction notification.
 *
 * @param roomDTO       the updated room in player view (for state broadcast)
 * @param correctionDTO the correction details (for notification broadcast)
 */
public record CorrectionResult(RoomDTO roomDTO, NumberCorrectionDTO correctionDTO) {
}
```

**RoomService.java — add method:**

```java
/**
 * Corrects the last drawn number in a MANUAL mode room.
 * <p>
 * Validates the creator, ensures the room is in MANUAL mode, checks that at least one number
 * has been drawn, and that the new number is within range and not already drawn.
 * Replaces the last element in the drawn numbers list and persists the change.
 * </p>
 *
 * @param sessionCode the public session code of the room
 * @param creatorHash the creator's authentication hash
 * @param newNumber   the corrected number to replace the last drawn number
 * @return a {@link CorrectionResult} containing the updated player-view DTO and correction notification
 * @throws RoomNotFoundException    if no room matches the given session code and creator hash
 * @throws IllegalArgumentException if the room is not MANUAL, or the new number is invalid
 * @throws IllegalStateException    if no numbers have been drawn yet
 */
public CorrectionResult correctLastNumber(String sessionCode, String creatorHash, int newNumber) {
	log.info("Correcting last number in room '{}' to {}", sessionCode, newNumber);

	RoomEntity entity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
			.orElseThrow(() -> new RoomNotFoundException("Room not found."));

	if (entity.getDrawMode() != DrawMode.MANUAL) {
		throw new IllegalArgumentException("Number correction is only available for manual draw mode rooms");
	}

	List<Integer> drawnNumbers = entity.getDrawnNumbers();
	if (drawnNumbers.isEmpty()) {
		throw new IllegalStateException("No numbers have been drawn yet");
	}

	validateDrawnNumber(newNumber, entity);  // reuse existing private method — checks range + duplicate

	int oldNumber = drawnNumbers.get(drawnNumbers.size() - 1);

	// Remove old number before validation would flag newNumber as duplicate
	// Actually: validateDrawnNumber checks if newNumber is already in the list.
	// The old number is still in the list at this point. If newNumber == oldNumber,
	// that's a no-op correction — but validateDrawnNumber would reject it as duplicate.
	// We should remove the old number BEFORE validating, OR handle this explicitly.

	// Approach: remove old number first, then validate, then add new number.
	drawnNumbers.remove(drawnNumbers.size() - 1);

	// Now validate newNumber against the list WITHOUT the old number
	validateDrawnNumber(newNumber, entity);

	drawnNumbers.add(newNumber);
	repository.save(entity);

	String oldLabel = numberLabelMapper.toLabel(oldNumber);
	String newLabel = numberLabelMapper.toLabel(newNumber);

	log.info("Corrected last number in room '{}': {} -> {}", sessionCode, oldLabel, newLabel);

	RoomDTO roomDTO = RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
	NumberCorrectionDTO correctionDTO = NumberCorrectionDTO.of(oldNumber, oldLabel, newNumber, newLabel);

	return new CorrectionResult(roomDTO, correctionDTO);
}
```

**IMPORTANT implementation note:** The existing `validateDrawnNumber` method checks if `newNumber` is already in `drawnNumbers`. Since we're replacing the last number, we must **remove the old number first** before calling `validateDrawnNumber`, otherwise correcting to the same number or any number in the list would have wrong behavior. The approach above handles this correctly: remove old → validate new → add new.

The existing `validateDrawnNumber` private method (already in RoomService):
```java
private void validateDrawnNumber(int number, RoomEntity entity) {
	int min = numberLabelMapper.getMinNumber();
	int max = numberLabelMapper.getMaxNumber();
	if (number < min || number > max) {
		throw new IllegalArgumentException(
			"Drawn number must be between " + min + " and " + max + ", got: " + number
		);
	}
	if (entity.getDrawnNumbers().contains(number)) {
		throw new IllegalArgumentException(
			"Number " + number + " has already been drawn in this room"
		);
	}
}
```

**Unit tests to ADD in RoomServiceTest.java** — new `@Nested` class:

```java
@Nested
@DisplayName("correctLastNumber")
class CorrectLastNumber {
	// Test cases below
}
```

| Test | Setup | Assert |
|------|-------|--------|
| success — replaces last number and returns correction result | Entity with drawnNumbers [5, 42], correct to 12 | drawnNumbers becomes [5, 12], CorrectionResult has old=42/new=12, labels correct, creatorHash null in RoomDTO |
| room not found — throws RoomNotFoundException | Mock returns empty | RoomNotFoundException, no save |
| wrong mode (AUTOMATIC) — throws IllegalArgumentException | Entity with AUTOMATIC mode | IllegalArgumentException with "manual draw mode" message, no save |
| no numbers drawn — throws IllegalStateException | Entity with empty drawnNumbers | IllegalStateException with "No numbers have been drawn" message, no save |
| new number out of range (0) — throws IllegalArgumentException | Entity with drawnNumbers [5], correct to 0 | IllegalArgumentException with "between 1 and 75", no save |
| new number out of range (76) — throws IllegalArgumentException | Entity with drawnNumbers [5], correct to 76 | IllegalArgumentException with "between 1 and 75", no save |
| new number already drawn (duplicate) — throws IllegalArgumentException | Entity with drawnNumbers [5, 42], correct to 5 | IllegalArgumentException with "already been drawn", no save |

For the success test, stub the mapper:
```java
stubStandardMapper(); // reuse existing helper
when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)).thenReturn(Optional.of(entity));
when(repository.save(entity)).thenReturn(entity);
```

Then assert:
```java
CorrectionResult result = roomService.correctLastNumber(sessionCode, creatorHash, 12);

assertThat(result.roomDTO().drawnNumbers()).containsExactly(5, 12);
assertThat(result.roomDTO().creatorHash()).isNull();
assertThat(result.correctionDTO().oldNumber()).isEqualTo(42);
assertThat(result.correctionDTO().oldLabel()).isEqualTo("X-42"); // mocked mapper returns "X-{n}"
assertThat(result.correctionDTO().newNumber()).isEqualTo(12);
assertThat(result.correctionDTO().newLabel()).isEqualTo("X-12");
assertThat(result.correctionDTO().message()).isEqualTo("GM changed X-42 to X-12");
verify(repository).save(entity);
```

### Conventions (from project CLAUDE.md)
- Java 21, Lombok (`@RequiredArgsConstructor`, `@Slf4j`)
- Tabs for indentation
- Javadoc on class and public methods
- SLF4J logging — INFO for actions, DEBUG for details
- Mockito for unit tests, `@ExtendWith(MockitoExtension.class)`
- `@Nested` + `@DisplayName` test organization
- AssertJ for assertions
- Records for immutable data containers

## TDD Sequence
1. Write `CorrectionResult.java` (record — no test needed, trivial)
2. Add all 7 test methods to `RoomServiceTest.java` under `@Nested @DisplayName("correctLastNumber")`
3. Add `correctLastNumber` method to `RoomService.java` — make tests pass
4. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Service method handles all validation branches. 7 new unit tests green. All existing tests still pass. No compilation warnings.
