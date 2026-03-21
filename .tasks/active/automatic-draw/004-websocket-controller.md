# 004 — DrawNumberForm + WebSocketController + Mode Enforcement

## What to build
Create `DrawNumberForm` (session-code + creator-hash only, no number). Add new `@MessageMapping("/draw-number")` endpoint to `WebSocketController` that calls `RoomService.drawRandomNumber()` and broadcasts the result. Add mode enforcement: `/add-number` only works for MANUAL rooms, `/draw-number` only works for AUTOMATIC rooms (enforced at service layer already, controller just delegates).

## Acceptance Criteria
- [ ] `DrawNumberForm` record with sessionCode and creatorHash fields, with validation
- [ ] New `/draw-number` WebSocket mapping in `WebSocketController`
- [ ] Broadcasts updated `RoomDTO` (player view) to `/room/{sessionCode}`
- [ ] Existing `/add-number` endpoint unchanged
- [ ] All tests pass

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `DrawNumberForm.java` | `com.yanajiki.application.bingoapp.websocket.form` | WebSocket payload for automatic draw |

### Files to MODIFY
| File | Change |
|------|--------|
| `WebSocketController.java` | Add `drawRandomNumber()` method with `@MessageMapping("/draw-number")` |

### Files to READ (for patterns — do NOT modify unless listed above)
| File | What to copy |
|------|-------------|
| `WebSocketController.java` | Existing `drawNumber()` pattern, messaging template usage |
| `AddNumberForm.java` | Form structure, validation annotations, @JsonProperty naming |

### Implementation Details

**DrawNumberForm.java:**
```java
package com.yanajiki.application.bingoapp.websocket.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket payload for automatic number draw.
 * Only requires session identification and creator authentication — no number needed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DrawNumberForm {

	@NotBlank
	@JsonProperty("session-code")
	private String sessionCode;

	@NotBlank
	@JsonProperty("creator-hash")
	private String creatorHash;
}
```

**WebSocketController addition:**
```java
/**
 * Draws a random number for automatic draw mode rooms.
 * Picks a random undrawn number and broadcasts the updated room state.
 *
 * @param message the draw request containing session code and creator hash
 */
@MessageMapping("/draw-number")
public void drawRandomNumber(DrawNumberForm message) {
    log.info("Automatic draw requested for room: {}", message.getSessionCode());
    RoomDTO roomDTO = roomService.drawRandomNumber(
        message.getSessionCode(), message.getCreatorHash());
    messagingTemplate.convertAndSend(
        "/room/" + message.getSessionCode(), roomDTO);
}
```

### Conventions (from project CLAUDE.md)
- Tabs for indentation, camelCase naming
- Lombok on form classes
- @JsonProperty with kebab-case for WebSocket JSON payloads
- SLF4J logging at INFO level for draw operations
- Javadoc on all classes and public methods
- Controllers are thin — delegate everything to service

## TDD Sequence
1. Create `DrawNumberForm.java`
2. Add `drawRandomNumber()` method to `WebSocketController.java`
3. Run `mvn test` — all tests must pass (integration tests in task 005 will cover this endpoint)

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
