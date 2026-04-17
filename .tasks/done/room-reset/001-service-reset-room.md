# 001 — Service: resetRoom

## What to build
Add a service method that clears a room's drawn numbers after validating creator
ownership and confirming no tiebreak is active. Unit tests first.

## Acceptance Criteria
- [ ] `RoomService.resetRoom(String sessionCode, String creatorHash)` returns `RoomDTO`
      in **player view** (no `creatorHash`)
- [ ] Throws `RoomNotFoundException(ROOM_NOT_FOUND)` when session code is unknown OR
      creator hash does not match
- [ ] Throws `BadRequestException(TIEBREAK_ALREADY_ACTIVE)` when a tiebreak is in
      progress for the room
- [ ] On success, the entity's `drawnNumbers` list is cleared and the entity is saved
      (`@UpdateTimestamp` resets TTL as a side effect — noted in javadoc)
- [ ] Works on a room with zero drawn numbers (no-op but valid, still saves and broadcasts)
- [ ] Unit test covers: success, unknown session, wrong hash, tiebreak-active, empty-draws
- [ ] All tests pass (`mvn test` runs via project hook — do not invoke manually)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `service/RoomService.java` | Add `resetRoom(String sessionCode, String creatorHash)` method with full javadoc |
| `service/RoomServiceTest.java` | Add `@Nested class ResetRoomTests` with the five unit tests above |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `service/RoomService.java:170-187` (`drawNumber`) | Creator-hash lookup + player-view DTO return pattern |
| `service/RoomService.java:205-237` (`correctLastNumber`) | List mutation + save pattern |
| `service/TiebreakService.java:144-146` (`hasActiveTiebreak`) | Tiebreak state check |
| `api/RoomDTO.java:72-83` (`fromEntityToPlayer`) | Player-view DTO shape |

### Implementation Sketch
```java
public RoomDTO resetRoom(String sessionCode, String creatorHash) {
    log.info("Resetting drawn numbers in room '{}'", sessionCode);

    RoomEntity entity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
        .orElseThrow(() -> new RoomNotFoundException(ErrorCode.ROOM_NOT_FOUND, "Room not found."));

    if (tiebreakService.hasActiveTiebreak(sessionCode)) {
        throw new BadRequestException(ErrorCode.TIEBREAK_ALREADY_ACTIVE,
            "Cannot reset the room while a tiebreaker is in progress");
    }

    entity.getDrawnNumbers().clear();
    repository.save(entity);

    log.info("Room '{}' reset — drawn numbers cleared", sessionCode);
    return RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
}
```

### Conventions
- Verify `TiebreakService` is already injected into `RoomService`. If not, add it via
  Lombok `@RequiredArgsConstructor` + final field (match existing injection style).
- Full javadoc with `@param`, `@return`, `@throws` blocks per project convention.
- Note in javadoc: "Saving the entity also resets the TTL via `@UpdateTimestamp`."
- Never use raw `IllegalArgumentException` / `IllegalStateException`.

## TDD Sequence
1. Write the five unit test cases in `RoomServiceTest.ResetRoomTests` with Mockito mocks
2. Run tests — all fail (method doesn't exist)
3. Implement `resetRoom` to make tests pass
4. Verify JaCoCo coverage ≥ 80%

## Done Definition
All acceptance criteria checked. Tests green via the PostToolUse hook.
