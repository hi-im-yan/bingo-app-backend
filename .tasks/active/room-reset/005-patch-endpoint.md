# 005 — PATCH /api/v1/room/{session-code} endpoint + broadcast + integration test

## What to build
Thin controller endpoint that delegates to `RoomService.updateRoom` (from task 004) and
broadcasts the updated player-view `RoomDTO` to connected WebSocket clients on the
existing room topic.

## Acceptance Criteria
- [ ] `PATCH /api/v1/room/{session-code}` requires `X-Creator-Hash` header
- [ ] Accepts JSON body deserialized into `UpdateRoomForm`; missing/empty body is valid
      (treated as no-op update — still saves and broadcasts)
- [ ] Returns `200 OK` with player-view `RoomDTO` (no `creatorHash`)
- [ ] Missing/wrong `X-Creator-Hash` → `404 ROOM_NOT_FOUND`
- [ ] Unknown session code → `404 ROOM_NOT_FOUND`
- [ ] Description longer than 255 chars → `400 VALIDATION_ERROR` with per-field details
      (handled automatically by `GlobalExceptionHandler` via `@Valid` + `MethodArgumentNotValidException`)
- [ ] On success, broadcasts the `RoomDTO` to `/room/{sessionCode}` via
      `SimpMessagingTemplate.convertAndSend`
- [ ] OpenAPI `@Operation` + `@ApiResponses` annotations match existing endpoint style
- [ ] RestAssured integration test (BDD given/when/then) covers: update description
      success, empty string clears, null/missing preserves, wrong hash, unknown session,
      oversize validation

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `api/RoomController.java` | Add `update(@PathVariable ..., @RequestHeader ..., @Valid @RequestBody UpdateRoomForm ...)` handler; inject `SimpMessagingTemplate` if not already present (task 002 may have added it) |
| `api/RoomControllerIntegrationTest.java` | Add PATCH test scenarios (given/when/then) |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `api/RoomController.java` — `delete` handler | Required `X-Creator-Hash` header signature |
| `api/RoomController.java` — existing `@Operation` annotations | Swagger documentation style |
| `websocket/WebSocketController.java` — `drawNumber` broadcast | `messagingTemplate.convertAndSend("/room/" + sessionCode, dto)` pattern |
| `api/RoomController.java` — `create` handler | `@Valid` body annotation + form usage |
| Existing RestAssured ITs in `api/` package | BDD given/when/then style, fixture setup |

### Implementation Sketch
```java
@PatchMapping("/{session-code}")
@Operation(summary = "Update room info (creator only) — PATCH semantic: null/missing fields are unchanged")
@ApiResponses({
	@ApiResponse(responseCode = "200", description = "Room updated successfully"),
	@ApiResponse(responseCode = "400", description = "Validation failed"),
	@ApiResponse(responseCode = "404", description = "Room not found or wrong creator hash")
})
public RoomDTO update(
		@PathVariable("session-code") String sessionCode,
		@RequestHeader("X-Creator-Hash") String creatorHash,
		@Valid @RequestBody(required = false) UpdateRoomForm form) {

	UpdateRoomForm body = form == null ? new UpdateRoomForm() : form;
	RoomDTO dto = roomService.updateRoom(sessionCode, creatorHash, body);
	messagingTemplate.convertAndSend("/room/" + sessionCode, dto);
	return dto;
}
```

### Conventions
- Controller stays thin — no business logic; the service validates and mutates.
- Broadcasting from the controller mirrors task 002's reset endpoint: `WebSocketController`
  broadcasts WS-triggered updates, `RoomController` broadcasts REST-triggered updates.
- Check if `SimpMessagingTemplate` is already a field on `RoomController` (task 002
  likely added it); reuse via constructor injection.
- Tabs, Lombok where applicable, OpenAPI annotations.

## TDD Sequence
1. Write integration test scenarios: create a room with initial description, PATCH with
   new description, assert 200 + updated description; then empty-string-clears,
   null-preserves, wrong-hash, unknown-session, oversize validation
2. Add the controller method + broadcast wiring
3. Tests pass via PostToolUse hook

## Notes
- **Optional STOMP broadcast assertion**: if existing integration tests establish a
  STOMP client pattern (check any `WebSocketIntegrationTest*`), add an assertion that
  the updated DTO is received on `/room/{sessionCode}`. If not, rely on unit-level
  verification of `messagingTemplate.convertAndSend` and skip STOMP in the integration test.
- Manual smoke test: PATCH during an active tiebreak — must succeed (200). PATCH with
  `description` length 256 — must fail 400.

## Done Definition
All acceptance criteria checked. Integration tests green. Endpoint visible in Swagger UI.
