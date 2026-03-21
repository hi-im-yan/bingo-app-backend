# 002 — CreateRoomForm + RoomDTO Updates

## What to build
Add `drawMode` field to `CreateRoomForm` (optional, defaults to MANUAL) and to `RoomDTO` response. Update `RoomService.createRoom()` to pass draw mode to entity. Update all affected existing tests.

## Acceptance Criteria
- [ ] `CreateRoomForm` has an optional `drawMode` field (defaults to MANUAL when null)
- [ ] `RoomDTO` includes `drawMode` in both creator and player views
- [ ] `RoomService.createRoom()` passes drawMode from form to entity factory
- [ ] All existing tests updated and passing
- [ ] Swagger/OpenAPI docs reflect the new field

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `CreateRoomForm.java` | Add `DrawMode drawMode` field (nullable, service defaults to MANUAL) |
| `RoomDTO.java` | Add `DrawMode drawMode` parameter to record. Update both factory methods |
| `RoomService.java` | In `createRoom()`, read drawMode from form, default to MANUAL if null, pass to entity factory |
| `RoomServiceTest.java` | Update existing tests for new drawMode field in DTOs and entity creation |
| `RoomControllerIntegrationTest.java` | Update assertions to include drawMode in responses |

### Files to READ (for patterns — do NOT modify unless listed above)
| File | What to copy |
|------|-------------|
| `CreateRoomForm.java` | Existing validation annotation patterns |
| `RoomDTO.java` | Record structure, factory method pattern |
| `RoomService.java` | createRoom method flow |
| `RoomServiceTest.java` | Test patterns, mock setup |
| `RoomControllerIntegrationTest.java` | RestAssured assertion patterns |

### Implementation Details

**CreateRoomForm changes:**
```java
// Add field (no @NotNull — it's optional, service handles default)
private DrawMode drawMode;
```

**RoomDTO changes:**
```java
public record RoomDTO(
    String name,
    String description,
    String sessionCode,
    @JsonInclude(JsonInclude.Include.NON_NULL) String creatorHash,
    List<Integer> drawnNumbers,
    List<String> drawnLabels,
    DrawMode drawMode  // new field
) {
    public static RoomDTO fromEntityToCreator(RoomEntity entity, NumberLabelMapper mapper) {
        return new RoomDTO(
            entity.getName(), entity.getDescription(), entity.getSessionCode(),
            entity.getCreatorHash(), entity.getDrawnNumbers(),
            toLabels(entity.getDrawnNumbers(), mapper),
            entity.getDrawMode()
        );
    }
    // Same pattern for fromEntityToPlayer — include drawMode
}
```

**RoomService.createRoom() change:**
```java
DrawMode mode = form.getDrawMode() != null ? form.getDrawMode() : DrawMode.MANUAL;
RoomEntity entity = RoomEntity.createEntityObject(form.getName(), form.getDescription(), mode);
```

### Conventions (from project CLAUDE.md)
- Tabs for indentation, camelCase naming
- Records for DTOs
- Lombok on forms
- Javadoc on all classes and public methods
- @JsonInclude(NON_NULL) for fields hidden in certain views (creatorHash pattern)

## TDD Sequence
1. Update `RoomServiceTest.java` — update mock returns and assertions to include drawMode
2. Update `CreateRoomForm.java`, `RoomDTO.java`, `RoomService.java` to make tests pass
3. Update `RoomControllerIntegrationTest.java` assertions
4. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
