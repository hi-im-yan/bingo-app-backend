# B01 — TiebreakDTO, Forms, and In-Memory State

## What to build
Create the DTOs, WebSocket form classes, and an in-memory state holder for tiebreaker sessions. One active tiebreaker per room at a time, not persisted to DB.

## Acceptance Criteria
- [ ] `TiebreakDTO` response class with status, playerCount, draws, winnerSlot
- [ ] `StartTiebreakForm` and `TiebreakDrawForm` WebSocket form classes
- [ ] `TiebreakState` in-memory model holding active tiebreaker data per room
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package | Purpose |
|------|---------|---------|
| `TiebreakDTO.java` | `com.yanajiki.application.bingoapp.api.response` | Response DTO broadcast to clients |
| `StartTiebreakForm.java` | `com.yanajiki.application.bingoapp.websocket.form` | WS payload for starting tiebreaker |
| `TiebreakDrawForm.java` | `com.yanajiki.application.bingoapp.websocket.form` | WS payload for drawing a slot |
| `TiebreakState.java` | `com.yanajiki.application.bingoapp.service` | In-memory tiebreaker session state |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `DrawNumberForm.java` | Lombok annotations, @JsonProperty kebab-case, @NotBlank |
| `NumberCorrectionDTO.java` | DTO record pattern |
| `RoomDTO.java` | DTO with static factory methods |

### Implementation Details

**TiebreakDTO** (record):
```java
public record TiebreakDTO(
	String status,          // "STARTED", "IN_PROGRESS", "FINISHED"
	int playerCount,
	List<TiebreakDrawEntry> draws,
	Integer winnerSlot      // null until FINISHED
) {
	public record TiebreakDrawEntry(int slot, int number, String label) {}

	public static TiebreakDTO from(TiebreakState state, NumberLabelMapper mapper) {
		// Build from state, map numbers to labels
	}
}
```

**StartTiebreakForm**:
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StartTiebreakForm {
	@NotBlank @JsonProperty("session-code") private String sessionCode;
	@NotBlank @JsonProperty("creator-hash") private String creatorHash;
	@JsonProperty("player-count") private int playerCount;  // 2–6
}
```

**TiebreakDrawForm**:
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TiebreakDrawForm {
	@NotBlank @JsonProperty("session-code") private String sessionCode;
	@NotBlank @JsonProperty("creator-hash") private String creatorHash;
	private int slot;  // 1-based
}
```

**TiebreakState** (mutable POJO, not an entity):
```java
@Getter
public class TiebreakState {
	private final int playerCount;
	private final Map<Integer, Integer> draws = new LinkedHashMap<>();  // slot → number

	public TiebreakState(int playerCount) { this.playerCount = playerCount; }

	public void addDraw(int slot, int number) { draws.put(slot, number); }
	public boolean isSlotDrawn(int slot) { return draws.containsKey(slot); }
	public boolean isComplete() { return draws.size() == playerCount; }
	public int getWinnerSlot() {
		return draws.entrySet().stream()
			.max(Map.Entry.comparingByValue())
			.orElseThrow()
			.getKey();
	}
	public String getStatus() {
		if (draws.isEmpty()) return "STARTED";
		return isComplete() ? "FINISHED" : "IN_PROGRESS";
	}
}
```

### Conventions (from project CLAUDE.md)
- Lombok on all classes. Records for DTOs where possible.
- `@JsonProperty` with kebab-case for WS form fields (`session-code`, `creator-hash`, `player-count`)
- Javadoc on all public classes and methods
- Tabs for indentation, camelCase naming

## TDD Sequence
1. Write tests for `TiebreakState`: addDraw, isComplete, getWinnerSlot, getStatus transitions
2. Write `TiebreakState` — make tests pass
3. Create `TiebreakDTO`, `StartTiebreakForm`, `TiebreakDrawForm` (no logic to test, just structure)
4. Run full test suite

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
