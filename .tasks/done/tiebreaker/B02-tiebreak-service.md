# B02 — TiebreakService

## What to build
Service that manages tiebreaker lifecycle: start, draw per slot, resolve winner. Holds active tiebreakers in a `ConcurrentHashMap<String, TiebreakState>` keyed by session code. Draws random numbers from the undrawn pool without adding them to the room's drawnNumbers.

## Acceptance Criteria
- [ ] `startTiebreak(sessionCode, creatorHash, playerCount)` creates state and returns `TiebreakDTO` with status STARTED
- [ ] `drawForSlot(sessionCode, creatorHash, slot)` picks random undrawn number, returns updated `TiebreakDTO`
- [ ] When all slots drawn, status is FINISHED and winnerSlot is set
- [ ] Validates: room exists, AUTOMATIC mode, creator auth, player count 2–6
- [ ] Validates: no active tiebreaker when starting, active tiebreaker when drawing
- [ ] Validates: slot in range, slot not already drawn
- [ ] Tiebreaker numbers come from undrawn pool but are NOT added to room's drawnNumbers
- [ ] `clearTiebreak(sessionCode)` removes active state (called after FINISHED to clean up)
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package | Purpose |
|------|---------|---------|
| `TiebreakService.java` | `com.yanajiki.application.bingoapp.service` | Tiebreaker business logic |
| `TiebreakServiceTest.java` | `com.yanajiki.application.bingoapp.service` (test) | Unit tests |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomService.java` | `selectRandomNumber` method (random from undrawn pool), auth pattern with `findBySessionCodeAndCreatorHash` |
| `RoomServiceTest.java` | Test structure: @ExtendWith(MockitoExtension), @Mock, @InjectMocks, @Nested, @DisplayName, assertThat/assertThatThrownBy |
| `TiebreakState.java` | State model from B01 |

### Implementation Details

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class TiebreakService {
	private final RoomRepository roomRepository;
	private final NumberLabelMapper numberLabelMapper;
	private final ConcurrentHashMap<String, TiebreakState> activeTiebreaks = new ConcurrentHashMap<>();

	/** Start a tiebreaker for the given room. */
	public TiebreakDTO startTiebreak(String sessionCode, String creatorHash, int playerCount) {
		// 1. Validate room exists + creator auth
		// 2. Validate AUTOMATIC mode
		// 3. Validate playerCount 2–6
		// 4. Validate no active tiebreaker for this room
		// 5. Create TiebreakState, store in map
		// 6. Return TiebreakDTO.from(state, numberLabelMapper)
	}

	/** Draw a random undrawn number for the given slot. */
	public TiebreakDTO drawForSlot(String sessionCode, String creatorHash, int slot) {
		// 1. Validate room exists + creator auth
		// 2. Validate active tiebreaker exists
		// 3. Validate slot in range [1, state.playerCount]
		// 4. Validate slot not already drawn
		// 5. Get undrawn pool from room entity (same logic as RoomService.selectRandomNumber)
		//    ALSO exclude numbers already drawn in this tiebreaker
		// 6. Pick random from pool using SecureRandom
		// 7. state.addDraw(slot, number)
		// 8. Return TiebreakDTO.from(state, numberLabelMapper)
		//    NOTE: do NOT call entity.addDrawnNumber() — tiebreaker numbers are ephemeral
	}

	/** Remove active tiebreaker state for a room. */
	public void clearTiebreak(String sessionCode) {
		activeTiebreaks.remove(sessionCode);
	}

	/** Check if a room has an active tiebreaker. */
	public boolean hasActiveTiebreak(String sessionCode) {
		return activeTiebreaks.containsKey(sessionCode);
	}
}
```

**Random number selection for tiebreaker:**
The pool is `[1–75] minus room.drawnNumbers minus tiebreakState.draws.values()`. This ensures tiebreaker draws don't collide with each other or with already-drawn game numbers.

### Conventions
- Lombok: `@RequiredArgsConstructor`, `@Slf4j`
- SLF4J logging at INFO for actions, DEBUG for lookups
- Throw `RoomNotFoundException` for missing rooms
- Throw `IllegalArgumentException` for validation failures
- Throw `IllegalStateException` for state violations (e.g., no active tiebreaker)
- Javadoc on all public methods
- Tabs for indentation

## TDD Sequence
1. Write `TiebreakServiceTest` with @Nested groups:
   - `StartTiebreak`: valid start, room not found, wrong mode, bad player count, already active
   - `DrawForSlot`: valid draw, no active tiebreak, slot out of range, slot already drawn, all slots complete → FINISHED + winnerSlot
   - `ClearTiebreak`: removes state
2. Implement `TiebreakService` — make tests pass
3. Run full test suite

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
