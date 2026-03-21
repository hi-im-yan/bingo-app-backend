# 004 — WebSocket + REST Endpoints

## What to build
Add a STOMP WebSocket endpoint for players to join a room (broadcasts join event to room topic) and a REST endpoint for the creator to list all players in their room.

## Acceptance Criteria
- [ ] WS `/app/join-room` accepts JoinRoomForm and broadcasts PlayerDTO to `/room/{sessionCode}/players`
- [ ] WS endpoint handles RoomNotFoundException and ConflictException gracefully
- [ ] REST `GET /api/v1/room/{session-code}/players` returns List<PlayerDTO>
- [ ] REST endpoint requires X-Creator-Hash header
- [ ] Swagger annotations on REST endpoint
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Changes |
|------|---------|
| `WebSocketController.java` | Add `@MessageMapping("/join-room")` method |
| `RoomController.java` | Add `GET /players` endpoint |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `WebSocketController.java` | Existing @MessageMapping pattern, SimpMessagingTemplate usage |
| `RoomController.java` | @Operation, @ApiResponses, @RequestHeader pattern |
| `JoinRoomForm.java` | Form field names (from task 002) |

### Implementation Details

**WebSocketController — new method**:
```java
/**
 * Handles player joining a bingo room.
 * Broadcasts the new player's data to all subscribers of the room's player topic.
 *
 * @param form the join room form containing session code and player name
 */
@MessageMapping("/join-room")
public void joinRoom(@Valid JoinRoomForm form) {
	log.info("Player '{}' joining room '{}'", form.getPlayerName(), form.getSessionCode());

	PlayerDTO player = roomService.joinRoom(form.getSessionCode(), form.getPlayerName());

	messagingTemplate.convertAndSend(
		"/room/" + form.getSessionCode() + "/players",
		player
	);
}
```

**RoomController — new endpoint**:
```java
/**
 * Returns all players in a bingo room. Creator-only operation.
 *
 * @param sessionCode the room's session code
 * @param creatorHash the creator's authentication hash
 * @return list of players in the room
 */
@Operation(summary = "List players in a room", description = "Returns all players who have joined the room. Requires creator authentication.")
@ApiResponses(value = {
	@ApiResponse(responseCode = "200", description = "Player list retrieved successfully"),
	@ApiResponse(responseCode = "404", description = "Room not found or invalid creator hash")
})
@GetMapping("/{session-code}/players")
public List<PlayerDTO> getPlayers(
	@PathVariable("session-code") String sessionCode,
	@RequestHeader("X-Creator-Hash") String creatorHash
) {
	return roomService.getPlayersByRoom(sessionCode, creatorHash);
}
```

### Conventions (from project CLAUDE.md)
- Controllers are thin — delegate everything to RoomService
- Swagger @Operation + @ApiResponses on all REST endpoints
- SLF4J logging at INFO level for incoming requests
- WS broadcasts use SimpMessagingTemplate.convertAndSend
- Tabs for indentation, Javadoc on all public methods

## TDD Sequence
1. Add WebSocket `/join-room` method to WebSocketController
2. Add REST `/players` endpoint to RoomController
3. Run test suite — all tests must pass

## Done Definition
All acceptance criteria checked. Endpoints compile. Swagger docs render correctly. No existing tests broken.
