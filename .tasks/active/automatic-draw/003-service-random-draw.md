# 003 — RoomService.drawRandomNumber() + Unit Tests

## What to build
Add `drawRandomNumber(sessionCode, creatorHash)` method to `RoomService` that picks a random undrawn number from the 1-75 pool. Add mode enforcement: `drawNumber()` rejects calls on AUTOMATIC rooms, `drawRandomNumber()` rejects calls on MANUAL rooms. Write unit tests first (TDD).

## Acceptance Criteria
- [ ] `drawRandomNumber(sessionCode, creatorHash)` selects a random number from remaining pool
- [ ] Returns player-view `RoomDTO` with the new number included
- [ ] Throws `RoomNotFoundException` if room/creator not found
- [ ] Throws `IllegalStateException` if all 75 numbers already drawn
- [ ] Throws `IllegalArgumentException` if called on a MANUAL room ("This room uses manual draw mode")
- [ ] Existing `drawNumber()` throws `IllegalArgumentException` if called on an AUTOMATIC room ("This room uses automatic draw mode")
- [ ] Unit tests cover: happy path, all-numbers-drawn, wrong mode, room not found
- [ ] All existing tests still pass

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `RoomService.java` | Add `drawRandomNumber()` method. Add mode enforcement to both draw methods |
| `RoomServiceTest.java` | Add new @Nested class for automatic draw tests. Add mode enforcement tests to existing draw tests |

### Files to READ (for patterns — do NOT modify unless listed above)
| File | What to copy |
|------|-------------|
| `RoomService.java` | Existing `drawNumber()` pattern, repository usage, validation pattern |
| `RoomServiceTest.java` | Mock setup, @Nested structure, assertion patterns |
| `NumberLabelMapper.java` | `getMinNumber()`, `getMaxNumber()` interface |

### Implementation Details

**RoomService.drawRandomNumber():**
```java
/**
 * Draws a random number from the remaining pool for automatic draw mode rooms.
 *
 * @param sessionCode the room session code
 * @param creatorHash the creator's hash for authentication
 * @return player-view RoomDTO with updated drawn numbers
 * @throws RoomNotFoundException if room not found or creator hash doesn't match
 * @throws IllegalArgumentException if room is not in AUTOMATIC draw mode
 * @throws IllegalStateException if all numbers have been drawn
 */
public RoomDTO drawRandomNumber(String sessionCode, String creatorHash) {
    RoomEntity entity = roomRepository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
        .orElseThrow(() -> new RoomNotFoundException("Room not found"));

    if (entity.getDrawMode() != DrawMode.AUTOMATIC) {
        throw new IllegalArgumentException("This room uses manual draw mode");
    }

    int number = selectRandomNumber(entity);
    entity.addDrawnNumber(number);
    roomRepository.save(entity);
    return RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
}

private int selectRandomNumber(RoomEntity entity) {
    List<Integer> remaining = IntStream.rangeClosed(
            numberLabelMapper.getMinNumber(), numberLabelMapper.getMaxNumber())
        .filter(n -> !entity.getDrawnNumbers().contains(n))
        .boxed()
        .toList();

    if (remaining.isEmpty()) {
        throw new IllegalStateException("All numbers have been drawn");
    }

    return remaining.get(new SecureRandom().nextInt(remaining.size()));
}
```

**Mode enforcement in existing drawNumber():**
Add at the start of `drawNumber()`, after finding the entity:
```java
if (entity.getDrawMode() != DrawMode.MANUAL) {
    throw new IllegalArgumentException("This room uses automatic draw mode");
}
```

**Exception handling note:**
`IllegalStateException` for "all numbers drawn" — add to `GlobalExceptionHandler` mapping to 409 Conflict (or 400 Bad Request — use 409 since it's a state conflict). Actually, map it to 400 since it's a bad request given the current state.

### Conventions (from project CLAUDE.md)
- Tabs for indentation, camelCase naming
- SLF4J for logging
- Javadoc on all public methods
- SecureRandom for randomness (consistent with existing sessionCode generation)
- Unit tests: JUnit 5 + Mockito, @Nested + @DisplayName
- `mvn test` runs automatically via PostToolUse hook — do NOT run it manually

## TDD Sequence
1. Write new tests in `RoomServiceTest.java`:
   - @Nested class `DrawRandomNumber` with: happy path, all-drawn, wrong-mode, room-not-found
   - Add mode enforcement test to existing `DrawNumber` nested class
2. Implement `drawRandomNumber()` and mode enforcement in `RoomService.java`
3. Add `IllegalStateException` handler in `GlobalExceptionHandler.java` → 400 or 409
4. Tests must pass (mvn test runs automatically)

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
