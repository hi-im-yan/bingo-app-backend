# 002 — PlayerDTO + JoinRoomForm

## What to build
Create the response DTO for player data and the WebSocket form for joining a room. PlayerDTO is a record used in REST responses and WS broadcasts. JoinRoomForm is the STOMP message payload.

## Acceptance Criteria
- [ ] PlayerDTO record with name and joinDateTime fields
- [ ] PlayerDTO has factory method from PlayerEntity
- [ ] JoinRoomForm with validation annotations
- [ ] JSON property names use kebab-case for consistency with existing forms
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `PlayerDTO.java` | `com.yanajiki.application.bingoapp.api.response` | Response record for player data |
| `JoinRoomForm.java` | `com.yanajiki.application.bingoapp.websocket.form` | WS payload for joining a room |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomDTO.java` | Record structure, factory method pattern, @JsonInclude |
| `AddNumberForm.java` | Lombok form, @JsonProperty, @NotBlank annotations |
| `DrawNumberForm.java` | Same WS form pattern |

### Implementation Details

**PlayerDTO.java**:
```java
/**
 * Response record representing a player in a bingo room.
 */
public record PlayerDTO(
	String name,
	LocalDateTime joinDateTime
) {
	/**
	 * Creates a PlayerDTO from a PlayerEntity.
	 */
	public static PlayerDTO fromEntity(PlayerEntity entity) {
		return new PlayerDTO(
			entity.getName(),
			entity.getJoinDateTime()
		);
	}
}
```

**JoinRoomForm.java**:
```java
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class JoinRoomForm {

	@NotBlank
	@Size(max = 50)
	@JsonProperty("player-name")
	private String playerName;

	@NotBlank
	@JsonProperty("session-code")
	private String sessionCode;
}
```

### Conventions (from project CLAUDE.md)
- Records for DTOs, Lombok for forms (mutable)
- @JsonProperty with kebab-case for WS forms (matches AddNumberForm/DrawNumberForm pattern)
- Javadoc on records and factory methods
- Tabs for indentation

## TDD Sequence
1. Write PlayerDTO record with factory method
2. Write JoinRoomForm with validation annotations
3. Run test suite — ensure compilation and no regressions

## Done Definition
All acceptance criteria checked. DTO and form compile. No existing tests broken.
