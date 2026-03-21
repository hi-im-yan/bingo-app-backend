# 005 — Integration Tests for Automatic Draw

## What to build
Write integration tests covering the full automatic draw feature: room creation with AUTOMATIC mode, the draw endpoint behavior, and mode enforcement at the API level. Tests use RestAssured with H2 on dev profile.

## Acceptance Criteria
- [ ] Test: create room with `drawMode: AUTOMATIC` — response includes drawMode
- [ ] Test: create room without drawMode — defaults to MANUAL in response
- [ ] Test: GET room returns drawMode in response (both creator and player views)
- [ ] All new tests pass alongside existing tests
- [ ] No existing tests broken

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `RoomControllerIntegrationTest.java` | Add new @Nested class for automatic draw mode tests. Update existing tests if drawMode field needs asserting |

### Files to READ (for patterns — do NOT modify unless listed above)
| File | What to copy |
|------|-------------|
| `RoomControllerIntegrationTest.java` | RestAssured patterns, given/when/then style, test setup |
| `CreateRoomForm.java` | Current form fields for request body construction |
| `RoomDTO.java` | Response fields for assertion |

### Implementation Details

**New tests to add in `RoomControllerIntegrationTest.java`:**

```java
@Nested
@DisplayName("Automatic Draw Mode")
class AutomaticDrawMode {

    @Test
    @DisplayName("should create room with AUTOMATIC draw mode")
    void shouldCreateRoomWithAutomaticDrawMode() {
        // POST /api/v1/room with {"name": "Auto Room", "drawMode": "AUTOMATIC"}
        // Assert response contains drawMode: "AUTOMATIC"
    }

    @Test
    @DisplayName("should default to MANUAL draw mode when not specified")
    void shouldDefaultToManualDrawMode() {
        // POST /api/v1/room with {"name": "Default Room"}
        // Assert response contains drawMode: "MANUAL"
    }

    @Test
    @DisplayName("should return drawMode in GET room response for creator")
    void shouldReturnDrawModeInCreatorView() {
        // Create automatic room, then GET with X-Creator-Hash
        // Assert drawMode: "AUTOMATIC" in response
    }

    @Test
    @DisplayName("should return drawMode in GET room response for player")
    void shouldReturnDrawModeInPlayerView() {
        // Create automatic room, then GET without X-Creator-Hash
        // Assert drawMode: "AUTOMATIC" in response
    }
}
```

**Note on WebSocket testing:**
WebSocket integration tests are complex and out of scope for this task. The WebSocket draw functionality is covered by:
- Unit tests on `RoomService.drawRandomNumber()` (task 003)
- Controller is thin (just delegates to service + broadcasts)

### Conventions (from project CLAUDE.md)
- RestAssured with BDD given/when/then style
- Business-language test names with @DisplayName
- @Nested for grouping related tests
- H2 in-memory DB (dev profile) for integration tests
- `mvn test` runs automatically via PostToolUse hook — do NOT run it manually

## TDD Sequence
1. Write integration tests first (they will fail)
2. If any adjustments needed in implementation, make them
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
