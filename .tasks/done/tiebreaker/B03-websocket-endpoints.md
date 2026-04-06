# B03 — WebSocket Controller Tiebreaker Endpoints

## What to build
Add two `@MessageMapping` endpoints to `WebSocketController` for tiebreaker start and draw. Both broadcast `TiebreakDTO` to `/room/{sessionCode}/tiebreak`. Auto-clear tiebreaker state when status is FINISHED.

## Acceptance Criteria
- [ ] `/app/start-tiebreak` accepts `StartTiebreakForm`, broadcasts `TiebreakDTO` to `/room/{sessionCode}/tiebreak`
- [ ] `/app/tiebreak-draw` accepts `TiebreakDrawForm`, broadcasts `TiebreakDTO` to `/room/{sessionCode}/tiebreak`
- [ ] When tiebreak finishes (status FINISHED), state is cleared after broadcast
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | What to change |
|------|---------------|
| `WebSocketController.java` | Add `startTiebreak` and `tiebreakDraw` methods |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `WebSocketController.java` | Existing `@MessageMapping` pattern, `SimpMessagingTemplate` usage, Javadoc style |
| `WebSocketControllerTest.java` | Test pattern for WS controller (if unit tests exist for controller) |

### Implementation Details

```java
/**
 * Starts a tiebreaker for the given room.
 * Delegates to TiebreakService and broadcasts the initial state.
 */
@MessageMapping("/start-tiebreak")
public void startTiebreak(StartTiebreakForm form) {
	log.info("Tiebreaker started for room '{}' with {} players",
		form.getSessionCode(), form.getPlayerCount());

	TiebreakDTO dto = tiebreakService.startTiebreak(
		form.getSessionCode(), form.getCreatorHash(), form.getPlayerCount());

	messagingTemplate.convertAndSend(
		"/room/" + form.getSessionCode() + "/tiebreak", dto);
}

/**
 * Draws a tiebreaker number for the given slot.
 * Broadcasts updated state. Clears tiebreaker when all slots drawn.
 */
@MessageMapping("/tiebreak-draw")
public void tiebreakDraw(TiebreakDrawForm form) {
	log.info("Tiebreaker draw for room '{}', slot {}",
		form.getSessionCode(), form.getSlot());

	TiebreakDTO dto = tiebreakService.drawForSlot(
		form.getSessionCode(), form.getCreatorHash(), form.getSlot());

	messagingTemplate.convertAndSend(
		"/room/" + form.getSessionCode() + "/tiebreak", dto);

	if ("FINISHED".equals(dto.status())) {
		tiebreakService.clearTiebreak(form.getSessionCode());
		log.info("Tiebreaker finished for room '{}', winner slot {}",
			form.getSessionCode(), dto.winnerSlot());
	}
}
```

**Inject `TiebreakService`** into `WebSocketController`:
```java
private final TiebreakService tiebreakService;
```
Lombok's `@RequiredArgsConstructor` handles the constructor injection — just add the field.

### Conventions
- Javadoc on both methods
- SLF4J logging at INFO
- Consistent with existing controller pattern: delegate to service, broadcast result
- Tabs for indentation

## TDD Sequence
1. Add tests to `WebSocketControllerTest` (or create new test class) for:
   - `startTiebreak` calls service and sends to correct topic
   - `tiebreakDraw` calls service, sends to topic, clears state on FINISHED
2. Implement the two methods in `WebSocketController`
3. Run full test suite

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
