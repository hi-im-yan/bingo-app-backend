# 004 — Integration Test for GM Number Correction

## What to build
An integration test that verifies the full correction flow end-to-end: create a room, draw a number, correct it, and verify the drawn numbers list is updated correctly in the database.

## Acceptance Criteria
- [ ] Integration test boots full Spring context
- [ ] Tests verify the last drawn number is replaced after correction
- [ ] Tests verify the correction result contains correct old/new data
- [ ] Tests verify error cases (no numbers drawn, AUTOMATIC mode, duplicate)
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `NumberCorrectionIntegrationTest.java` | `com.yanajiki.application.bingoapp.service` (test) | Full-context integration test |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomControllerIntegrationTest.java` | `@SpringBootTest(RANDOM_PORT)`, `@ActiveProfiles("test")`, setup/teardown, RestAssured pattern |
| `RoomService.java` | `correctLastNumber` method signature (from task 002) |
| `RoomEntity.java` | Factory method, `addDrawnNumber`, field access |
| `RoomRepository.java` | Available query methods |

### Implementation Details

**NumberCorrectionIntegrationTest.java:**

Since correction is a WebSocket operation (no REST endpoint), this test calls the service layer directly with a real Spring context and real H2 database.

```java
package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.response.NumberCorrectionDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.game.DrawMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class NumberCorrectionIntegrationTest {

	@Autowired
	private RoomService roomService;

	@Autowired
	private RoomRepository roomRepository;

	@AfterEach
	void tearDown() {
		roomRepository.deleteAll();
	}
```

Test cases under `@Nested @DisplayName("correctLastNumber")`:

1. **Success — replaces last number in drawn list**
   - Create a MANUAL room via `RoomEntity.createEntityObject`, save it
   - Draw two numbers via `roomService.drawNumber` (e.g., 5 then 42)
   - Call `roomService.correctLastNumber(sessionCode, creatorHash, 12)`
   - Assert `result.roomDTO().drawnNumbers()` is `[5, 12]`
   - Assert `result.roomDTO().drawnLabels()` is `["B-5", "B-12"]`
   - Assert `result.correctionDTO().oldNumber()` is 42, `oldLabel` is "N-42"
   - Assert `result.correctionDTO().newNumber()` is 12, `newLabel` is "B-12"
   - Assert `result.correctionDTO().message()` is "GM changed N-42 to B-12"
   - Verify DB: fetch room from repository, assert `drawnNumbers` is `[5, 12]`

2. **Success — correcting single drawn number**
   - Create room, draw number 75
   - Correct to 1
   - Assert drawnNumbers is `[1]`
   - Assert correction message says "GM changed O-75 to B-1"

3. **Error — no numbers drawn yet**
   - Create room, don't draw any numbers
   - Call `correctLastNumber` — expect `IllegalStateException`

4. **Error — AUTOMATIC room rejects correction**
   - Create AUTOMATIC room, save it
   - Call `correctLastNumber` — expect `IllegalArgumentException` with "manual draw mode"

5. **Error — new number already in drawn list (duplicate)**
   - Create room, draw 5 then 42
   - Correct to 5 (already drawn) — expect `IllegalArgumentException` with "already been drawn"

6. **Error — new number out of range**
   - Create room, draw 5
   - Correct to 0 — expect `IllegalArgumentException` with "between 1 and 75"

7. **Error — room not found**
   - Call `correctLastNumber("INVALID", "bad-hash", 1)` — expect `RoomNotFoundException`

To create rooms for testing, use the entity factory and save directly:
```java
RoomEntity entity = RoomEntity.createEntityObject("Test Room", "desc");
RoomEntity saved = roomRepository.save(entity);
String sessionCode = saved.getSessionCode();
String creatorHash = saved.getCreatorHash();
```

To draw numbers, call the service (which handles validation and persistence):
```java
roomService.drawNumber(sessionCode, creatorHash, 5);
roomService.drawNumber(sessionCode, creatorHash, 42);
```

### Conventions (from project CLAUDE.md)
- Java 21
- Tabs for indentation
- `@SpringBootTest(RANDOM_PORT)` + `@ActiveProfiles("test")` for integration tests
- `@Nested` + `@DisplayName` for test organization
- AssertJ for assertions
- Cleanup in `@AfterEach`

## TDD Sequence
1. Write `NumberCorrectionIntegrationTest.java` — all test cases (they should pass since service is already implemented)
2. Run `mvn test` — all tests must pass
3. If any test fails, investigate and fix

## Done Definition
All acceptance criteria checked. Integration tests pass. Full correction flow verified end-to-end. Tests green. No compilation warnings.
