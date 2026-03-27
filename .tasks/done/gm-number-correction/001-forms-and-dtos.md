# 001 — CorrectNumberForm + NumberCorrectionDTO

## What to build
Create the WebSocket message form for the correction request and the notification DTO that gets broadcast to players when a correction happens.

## Acceptance Criteria
- [ ] `CorrectNumberForm` exists with sessionCode, creatorHash, newNumber fields
- [ ] `NumberCorrectionDTO` exists as a record with oldNumber, oldLabel, newNumber, newLabel, message fields
- [ ] Both classes have proper validation annotations and Javadoc
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `CorrectNumberForm.java` | `com.yanajiki.application.bingoapp.websocket.form` | WS message payload for correction request |
| `NumberCorrectionDTO.java` | `com.yanajiki.application.bingoapp.api.response` | Notification payload broadcast to players |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `AddNumberForm.java` | Lombok annotations, `@JsonProperty` kebab-case naming, validation style |
| `RoomDTO.java` | Record pattern, `@JsonInclude`, Javadoc style |

### Implementation Details

**CorrectNumberForm.java** — follows exact same pattern as `AddNumberForm`:

```java
package com.yanajiki.application.bingoapp.websocket.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * WebSocket message payload for correcting the last drawn bingo number.
 * <p>
 * The session code identifies the room, the creator hash authenticates the requester,
 * and the new number is the corrected value to replace the last drawn number
 * (must be between 1 and 75 inclusive).
 * </p>
 */
@NoArgsConstructor
@Getter
@Setter
public class CorrectNumberForm {

	/** The unique code that identifies the bingo room. */
	@NotBlank
	@JsonProperty("session-code")
	private String sessionCode;

	/** The creator's authentication hash, issued when the room was created. */
	@NotBlank
	@JsonProperty("creator-hash")
	private String creatorHash;

	/** The corrected bingo number to replace the last drawn number. Must be between 1 and 75 inclusive. */
	@NotNull
	@Min(1)
	@Max(75)
	@JsonProperty("new-number")
	private Integer newNumber;
}
```

**NumberCorrectionDTO.java** — record in the response package:

```java
package com.yanajiki.application.bingoapp.api.response;

/**
 * Notification DTO broadcast to players when the game master corrects the last drawn number.
 * <p>
 * Contains both the raw numbers and their display labels (e.g., {@code "O-75"}, {@code "B-12"})
 * so the frontend can render a human-readable correction message without additional lookups.
 * </p>
 *
 * @param oldNumber the previously drawn number that was incorrect
 * @param oldLabel  the display label of the old number (e.g., {@code "O-75"})
 * @param newNumber the corrected number replacing the old one
 * @param newLabel  the display label of the new number (e.g., {@code "B-12"})
 * @param message   a human-readable correction message (e.g., {@code "GM changed O-75 to B-12"})
 */
public record NumberCorrectionDTO(
	int oldNumber,
	String oldLabel,
	int newNumber,
	String newLabel,
	String message
) {

	/**
	 * Factory method to create a correction notification from raw numbers and a label mapper.
	 *
	 * @param oldNumber the old (incorrect) number
	 * @param newNumber the new (corrected) number
	 * @param oldLabel  the display label for the old number
	 * @param newLabel  the display label for the new number
	 * @return a new {@link NumberCorrectionDTO} with all fields populated including a formatted message
	 */
	public static NumberCorrectionDTO of(int oldNumber, String oldLabel, int newNumber, String newLabel) {
		String message = "GM changed " + oldLabel + " to " + newLabel;
		return new NumberCorrectionDTO(oldNumber, oldLabel, newNumber, newLabel, message);
	}
}
```

### Conventions (from project CLAUDE.md)
- Java 21, Lombok for forms (not records — forms need setters for deserialization)
- Records for DTOs (immutable response objects)
- Tabs for indentation
- `@JsonProperty` with kebab-case for WS form fields
- Javadoc on all classes and public methods
- Validation annotations: `@NotBlank`, `@NotNull`, `@Min`, `@Max`

## TDD Sequence
1. No unit tests needed for these — they are data classes with no logic (the `of` factory method is trivially testable via the service tests in task 002)
2. Create `CorrectNumberForm.java`
3. Create `NumberCorrectionDTO.java`
4. Run `mvn test` — all existing tests must still pass (no regressions)

## Done Definition
Both classes created with correct annotations, fields, and Javadoc. Existing tests green. No compilation warnings.
