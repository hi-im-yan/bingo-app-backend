# 003 — Service Methods + Unit Tests

## What to build
Add player join and player list methods to RoomService. `joinRoom` validates the room exists and player name is unique, persists the player, and returns the PlayerDTO. `getPlayersByRoom` returns the player list (creator-only).

## Acceptance Criteria
- [ ] `joinRoom(sessionCode, playerName)` saves player and returns PlayerDTO
- [ ] Throws RoomNotFoundException if session code invalid
- [ ] Throws ConflictException if player name already exists in room
- [ ] `getPlayersByRoom(sessionCode, creatorHash)` returns List<PlayerDTO>
- [ ] Throws RoomNotFoundException if session code or creator hash invalid
- [ ] Unit tests cover all happy paths and error cases
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Changes |
|------|---------|
| `RoomService.java` | Add `joinRoom` and `getPlayersByRoom` methods; inject `PlayerRepository` |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomService.java` | Existing method patterns, exception usage, repository calls |
| `RoomServiceTest.java` | @Nested, @DisplayName, Mockito patterns, assertion style |
| `PlayerEntity.java` | Factory method signature (from task 001) |
| `PlayerRepository.java` | Query method signatures (from task 001) |
| `PlayerDTO.java` | Factory method signature (from task 002) |

### Implementation Details

**New dependency in RoomService**:
```java
private final PlayerRepository playerRepository;
```
Add to constructor (Lombok @RequiredArgsConstructor handles this if used, otherwise add manually).

**joinRoom method**:
```java
/**
 * Registers a player in a bingo room.
 *
 * @param sessionCode the room's session code
 * @param playerName the player's display name
 * @return PlayerDTO with the registered player's data
 * @throws RoomNotFoundException if the room does not exist
 * @throws ConflictException if the player name is already taken in this room
 */
public PlayerDTO joinRoom(String sessionCode, String playerName) {
	RoomEntity room = roomRepository.findBySessionCode(sessionCode)
		.orElseThrow(() -> new RoomNotFoundException("Room not found with session code: " + sessionCode));

	if (playerRepository.existsByNameAndRoomEntity(playerName, room)) {
		throw new ConflictException("Player name '" + playerName + "' is already taken in this room");
	}

	PlayerEntity player = PlayerEntity.create(playerName, room);
	playerRepository.save(player);

	return PlayerDTO.fromEntity(player);
}
```

**getPlayersByRoom method**:
```java
/**
 * Returns all players in a room. Creator-only operation.
 *
 * @param sessionCode the room's session code
 * @param creatorHash the creator's authentication hash
 * @return list of PlayerDTOs for all players in the room
 * @throws RoomNotFoundException if the room or creator hash is invalid
 */
public List<PlayerDTO> getPlayersByRoom(String sessionCode, String creatorHash) {
	RoomEntity room = roomRepository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
		.orElseThrow(() -> new RoomNotFoundException("Room not found or invalid creator hash"));

	return playerRepository.findByRoomEntity(room).stream()
		.map(PlayerDTO::fromEntity)
		.toList();
}
```

**Unit tests to write** (in RoomServiceTest.java, new @Nested classes):

```
@Nested @DisplayName("Join Room")
- should register player and return PlayerDTO
- should throw RoomNotFoundException when session code is invalid
- should throw ConflictException when player name is duplicate in room

@Nested @DisplayName("Get Players By Room")
- should return list of PlayerDTOs for valid creator
- should return empty list when no players have joined
- should throw RoomNotFoundException when creator hash is invalid
```

### Conventions (from project CLAUDE.md)
- Tabs for indentation, camelCase naming
- SLF4J logging for important operations (log player join at INFO level)
- Javadoc on all public methods
- Unit tests: @ExtendWith(MockitoExtension.class), @Mock, @InjectMocks
- @Nested + @DisplayName for test organization
- ConflictException for duplicate resources (existing pattern)

## TDD Sequence
1. Write unit tests for `joinRoom` (happy path + error cases)
2. Write unit tests for `getPlayersByRoom` (happy path + error cases)
3. Implement `joinRoom` in RoomService — make tests pass
4. Implement `getPlayersByRoom` in RoomService — make tests pass
5. Run full test suite — all tests must pass

## Done Definition
All acceptance criteria checked. All unit tests green. No existing tests broken. No compilation warnings.
