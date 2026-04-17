# 004 — Service: updateRoom + UpdateRoomForm

## What to build
Add a Jackson-deserializable request form for partial room updates plus a service method
that updates the room's description after validating creator ownership. Unit tests first.

## Acceptance Criteria
- [ ] `UpdateRoomForm` exists in `api/form/` with single field `@Size(max=255) String description`;
      mutable class (not a record) with `@Getter`/`@Setter`/`@NoArgsConstructor`
- [ ] `RoomService.updateRoom(String sessionCode, String creatorHash, UpdateRoomForm form)`
      returns `RoomDTO` in **player view** (no `creatorHash`)
- [ ] Throws `RoomNotFoundException(ROOM_NOT_FOUND)` when session code is unknown OR
      creator hash does not match
- [ ] PATCH semantic: if `form.getDescription() == null` the entity's description is
      left untouched; if `""` it is cleared; otherwise it is set
- [ ] Entity is saved via `repository.save` (triggers `@UpdateTimestamp` TTL reset —
      noted in javadoc)
- [ ] Does NOT check tiebreak state — update is safe during tiebreak
- [ ] Unit tests cover:
  - non-null description → entity updated + saved + player-view DTO returned
  - empty string → description cleared
  - null → description unchanged
  - unknown session → `RoomNotFoundException`
  - wrong hash → `RoomNotFoundException`
  - `repository.save(entity)` verified via Mockito
- [ ] All tests pass (`mvn test` runs via project hook — do not invoke manually)

## Technical Spec

### Files to CREATE
| File | Package | Purpose |
|------|---------|---------|
| `api/form/UpdateRoomForm.java` | `com.yanajiki.application.bingoapp.api.form` | Jackson-deserializable partial-update form |

### Files to MODIFY
| File | Change |
|------|--------|
| `service/RoomService.java` | Add `updateRoom(String sessionCode, String creatorHash, UpdateRoomForm form)` method with full javadoc |
| `service/RoomServiceTest.java` | Add `@Nested class UpdateRoomTests` with the six unit tests above |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `api/form/CreateRoomForm.java` | `@Size(max=255)` annotation, mutable class style, javadoc |
| `service/RoomService.java` — `drawNumber` method | Creator-hash lookup + player-view DTO return pattern |
| `service/RoomService.java` — `correctLastNumber` method | Entity mutation + save pattern |
| `api/response/RoomDTO.java` — `fromEntityToPlayer` | Player-view DTO shape |

### Implementation Sketches

**UpdateRoomForm.java:**
```java
package com.yanajiki.application.bingoapp.api.form;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for partial updates of a bingo room.
 * <p>
 * Mutable class (not a record) so Jackson can deserialize partial JSON bodies.
 * PATCH semantic: a {@code null} field means "no change"; an empty string clears
 * the target field.
 * </p>
 */
@NoArgsConstructor
@Getter
@Setter
public class UpdateRoomForm {

	/** New description for the room. {@code null} = no change; {@code ""} = clear; otherwise update. At most 255 characters. */
	@Size(max = 255)
	private String description;
}
```

**RoomService.updateRoom:**
```java
public RoomDTO updateRoom(String sessionCode, String creatorHash, UpdateRoomForm form) {
	log.info("Updating room '{}'", sessionCode);

	RoomEntity entity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
		.orElseThrow(() -> new RoomNotFoundException(ErrorCode.ROOM_NOT_FOUND, "Room not found."));

	if (form.getDescription() != null) {
		entity.setDescription(form.getDescription());
	}

	repository.save(entity);
	log.info("Room '{}' updated", sessionCode);

	return RoomDTO.fromEntityToPlayer(entity, numberLabelMapper);
}
```

### Conventions
- Full javadoc with `@param`, `@return`, `@throws` per project convention.
- Note in javadoc: "Saving the entity also resets the TTL via `@UpdateTimestamp`."
- Never use raw `IllegalArgumentException` / `IllegalStateException`.
- Tabs for indentation, camelCase, Lombok.

## TDD Sequence
1. Write the six unit test cases in `RoomServiceTest.UpdateRoomTests` with Mockito mocks
2. Run tests — all fail (method doesn't exist)
3. Create `UpdateRoomForm`, implement `updateRoom` to make tests pass
4. Verify JaCoCo coverage ≥ 80%

## Done Definition
All acceptance criteria checked. Tests green via the PostToolUse hook.
