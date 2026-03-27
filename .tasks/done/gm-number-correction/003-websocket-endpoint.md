# 003 — WebSocket Endpoint + Dual Broadcast

## What to build
Add a new `@MessageMapping("/correct-number")` endpoint to `WebSocketController` that handles GM correction requests and broadcasts both the updated room state and the correction notification to subscribed clients.

## Acceptance Criteria
- [ ] `@MessageMapping("/correct-number")` exists in `WebSocketController`
- [ ] Calls `RoomService.correctLastNumber()` with form data
- [ ] Broadcasts updated `RoomDTO` to `/room/{sessionCode}` (existing state topic)
- [ ] Broadcasts `NumberCorrectionDTO` to `/room/{sessionCode}/corrections` (notification topic)
- [ ] Unit test verifies both broadcasts happen with correct payloads
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Package/Path | What to change |
|------|-------------|----------------|
| `WebSocketController.java` | `com.yanajiki.application.bingoapp.websocket` | Add `correctNumber` method |

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `WebSocketControllerTest.java` | `com.yanajiki.application.bingoapp.websocket` (test) | Unit test for the new endpoint |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `WebSocketController.java` | Existing method patterns: extract fields from form, call service, build topic, broadcast |
| `CorrectNumberForm.java` | Field names: `getSessionCode()`, `getCreatorHash()`, `getNewNumber()` (from task 001) |
| `CorrectionResult.java` | `roomDTO()` and `correctionDTO()` accessor methods (from task 002) |

### Implementation Details

**WebSocketController.java — add method:**

```java
/**
 * Handles a number correction request from the game master.
 * <p>
 * Delegates to {@link RoomService#correctLastNumber} to validate the creator, replace the
 * last drawn number, and persist. Broadcasts the updated player-view room state to
 * {@code /room/{sessionCode}} and a correction notification to
 * {@code /room/{sessionCode}/corrections} so connected clients can display the change.
 * </p>
 *
 * @param message the incoming message containing session code, creator hash, and corrected number
 */
@MessageMapping("/correct-number")
public void correctNumber(CorrectNumberForm message) {
	String sessionCode = message.getSessionCode();
	log.info("Number correction requested for room '{}'", sessionCode);

	CorrectionResult result = roomService.correctLastNumber(
			sessionCode, message.getCreatorHash(), message.getNewNumber());

	String roomTopic = "/room/" + sessionCode;
	String correctionTopic = "/room/" + sessionCode + "/corrections";

	log.debug("Broadcasting correction for room '{}': {}", sessionCode, result.correctionDTO().message());
	messagingTemplate.convertAndSend(roomTopic, result.roomDTO());
	messagingTemplate.convertAndSend(correctionTopic, result.correctionDTO());
}
```

Add imports:
```java
import com.yanajiki.application.bingoapp.websocket.form.CorrectNumberForm;
import com.yanajiki.application.bingoapp.service.CorrectionResult;
```

**WebSocketControllerTest.java — unit test:**

Use Mockito to mock `RoomService` and `SimpMessagingTemplate`. Verify the correct methods are called with correct arguments.

```java
package com.yanajiki.application.bingoapp.websocket;

import com.yanajiki.application.bingoapp.api.response.NumberCorrectionDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.game.DrawMode;
import com.yanajiki.application.bingoapp.service.CorrectionResult;
import com.yanajiki.application.bingoapp.service.RoomService;
import com.yanajiki.application.bingoapp.websocket.form.CorrectNumberForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

	@Mock
	private RoomService roomService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private WebSocketController webSocketController;
```

Test cases under `@Nested @DisplayName("correctNumber")`:

1. **Broadcasts updated RoomDTO to room topic** — Set up form with sessionCode="ABC123", creatorHash="hash", newNumber=12. Mock service to return a CorrectionResult. Call `correctNumber(form)`. Verify `messagingTemplate.convertAndSend("/room/ABC123", result.roomDTO())`.

2. **Broadcasts NumberCorrectionDTO to corrections topic** — Same setup. Verify `messagingTemplate.convertAndSend("/room/ABC123/corrections", result.correctionDTO())`.

3. **Calls service with correct arguments** — Verify `roomService.correctLastNumber("ABC123", "hash", 12)`.

These can be combined into a single test method or split — implementer's choice following `@DisplayName` clarity.

To build the mock `CorrectionResult`:
```java
RoomDTO roomDTO = new RoomDTO("Test Room", "desc", "ABC123", null, List.of(5, 12), List.of("X-5", "X-12"), DrawMode.MANUAL);
NumberCorrectionDTO correctionDTO = NumberCorrectionDTO.of(42, "X-42", 12, "X-12");
CorrectionResult result = new CorrectionResult(roomDTO, correctionDTO);

when(roomService.correctLastNumber("ABC123", "hash", 12)).thenReturn(result);
```

### Conventions (from project CLAUDE.md)
- Java 21, Lombok
- Tabs for indentation
- Javadoc on class and public methods
- Mockito for unit tests, `@ExtendWith(MockitoExtension.class)`
- `@Nested` + `@DisplayName` test organization
- Controllers are thin — no business logic, only delegate to service and broadcast

## TDD Sequence
1. Write `WebSocketControllerTest.java` — test cases for `correctNumber`
2. Add `correctNumber` method to `WebSocketController.java` — make tests pass
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Endpoint wired, both broadcasts verified by test. Tests green. No compilation warnings.
