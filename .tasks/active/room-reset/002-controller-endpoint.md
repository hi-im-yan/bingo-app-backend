# 002 — POST /api/v1/room/{session-code}/reset endpoint + broadcast + integration test

## What to build
Thin controller endpoint that delegates to `RoomService.resetRoom` (from task 001) and
broadcasts the updated player-view `RoomDTO` to connected WebSocket clients on the
existing room topic.

## Acceptance Criteria
- [ ] `POST /api/v1/room/{session-code}/reset` requires `X-Creator-Hash` header
- [ ] Returns `200 OK` with player-view `RoomDTO` (empty `drawnNumbers`, no `creatorHash`)
- [ ] Missing/wrong `X-Creator-Hash` → `404 ROOM_NOT_FOUND`
- [ ] Unknown session code → `404 ROOM_NOT_FOUND`
- [ ] Active tiebreak → `400 TIEBREAK_ALREADY_ACTIVE`
- [ ] On success, broadcasts the same `RoomDTO` to `/room/{sessionCode}` via
      `SimpMessagingTemplate.convertAndSend`
- [ ] OpenAPI `@Operation` + `@ApiResponses` annotations match existing endpoint style
- [ ] RestAssured integration test (BDD given/when/then) covers: success + numbers cleared,
      wrong hash, no hash, unknown session, tiebreak-active

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `api/RoomController.java` | Add `reset(@PathVariable ..., @RequestHeader ...)` handler; inject `SimpMessagingTemplate` if not already present |
| `api/RoomControllerIntegrationTest.java` | Add reset test scenarios (given/when/then) |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `api/RoomController.java:123-130` (DELETE) | Required `X-Creator-Hash` header signature |
| `websocket/WebSocketController.java:46` | `messagingTemplate.convertAndSend("/room/" + sessionCode, dto)` pattern |
| `api/RoomController.java` existing `@Operation` annotations | Swagger documentation style |
| Existing RestAssured ITs in `api/` package | BDD given/when/then style, fixture setup |

### Implementation Sketch
```java
@PostMapping("/{session-code}/reset")
@Operation(summary = "Reset drawn numbers for a room (creator only)")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Room reset successfully"),
    @ApiResponse(responseCode = "400", description = "Active tiebreak blocks reset"),
    @ApiResponse(responseCode = "404", description = "Room not found or wrong creator hash")
})
public RoomDTO reset(
        @PathVariable("session-code") String sessionCode,
        @RequestHeader("X-Creator-Hash") String creatorHash) {

    RoomDTO dto = roomService.resetRoom(sessionCode, creatorHash);
    messagingTemplate.convertAndSend("/room/" + sessionCode, dto);
    return dto;
}
```

### Conventions
- Controller stays thin — no business logic; the service validates and mutates
- Broadcasting from the controller mirrors the existing split: `WebSocketController`
  broadcasts WS-triggered updates, `RoomController` will broadcast REST-triggered updates
- Check if `SimpMessagingTemplate` is already a field on `RoomController`; add via
  constructor injection if missing
- Tabs, Lombok where applicable, OpenAPI annotations

## TDD Sequence
1. Write integration test scenarios: create a room, draw a few numbers, hit `/reset`,
   assert 200 + empty `drawnNumbers`; then the failure cases
2. Add the controller method + broadcast wiring
3. Tests pass via PostToolUse hook

## Notes
- **Optional STOMP broadcast assertion**: check whether existing integration tests
  establish a STOMP client pattern. If yes, add an assertion that the reset DTO is
  received on `/room/{sessionCode}`. If not, rely on the unit-level verification of
  `messagingTemplate.convertAndSend` (Mockito `verify` in a controller unit test) and
  skip STOMP in the integration test.
- Manual smoke test: start a tiebreak via WS, then try to reset — must 400.

## Done Definition
All acceptance criteria checked. Integration tests green. Endpoint visible in Swagger UI.
