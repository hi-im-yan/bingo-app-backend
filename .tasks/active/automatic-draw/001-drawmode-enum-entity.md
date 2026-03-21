# 001 — DrawMode Enum + RoomEntity Update

## What to build
Create a `DrawMode` enum (`MANUAL`, `AUTOMATIC`) and add a `drawMode` field to `RoomEntity`. Update the entity factory method to accept draw mode. Update entity tests.

## Acceptance Criteria
- [ ] `DrawMode` enum exists with `MANUAL` and `AUTOMATIC` values
- [ ] `RoomEntity` has a `drawMode` field, persisted as STRING, defaults to `MANUAL`
- [ ] `RoomEntity.createEntityObject()` accepts an optional `drawMode` parameter (overloaded or default)
- [ ] Entity tests cover the new field
- [ ] All existing tests still pass

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `DrawMode.java` | `com.yanajiki.application.bingoapp.game` | Enum with MANUAL, AUTOMATIC values |

### Files to MODIFY
| File | Change |
|------|--------|
| `RoomEntity.java` | Add `drawMode` field with `@Enumerated(EnumType.STRING)`, default `MANUAL`. Add overloaded factory method |
| `RoomEntityTest.java` | Add tests for drawMode field in entity creation |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomEntity.java` | Lombok annotations, entity structure, factory method pattern |
| `RoomEntityTest.java` | Test structure, @Nested, @DisplayName conventions |
| `StandardBingoMapper.java` | Package location reference for game package |

### Implementation Details

**DrawMode.java:**
```java
package com.yanajiki.application.bingoapp.game;

/**
 * Defines the draw mode for a bingo room.
 * MANUAL: creator selects a specific number to draw.
 * AUTOMATIC: server randomly selects the next number from the remaining pool.
 */
public enum DrawMode {
	MANUAL,
	AUTOMATIC
}
```

**RoomEntity changes:**
- Add field:
  ```java
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DrawMode drawMode;
  ```
- Add overloaded factory method:
  ```java
  public static RoomEntity createEntityObject(String name, String description, DrawMode drawMode)
  ```
- Keep existing factory method, default to `DrawMode.MANUAL`:
  ```java
  public static RoomEntity createEntityObject(String name, String description) {
      return createEntityObject(name, description, DrawMode.MANUAL);
  }
  ```

### Conventions (from project CLAUDE.md)
- Tabs for indentation, camelCase naming
- Lombok for boilerplate (@Getter, @Setter, @NoArgsConstructor, etc.)
- Javadoc on all classes and public methods
- JUnit 5 + @Nested + @DisplayName for test organization

## TDD Sequence
1. Write `DrawMode.java` enum (trivial, no test needed for enum)
2. Write new tests in `RoomEntityTest.java` for drawMode field
3. Modify `RoomEntity.java` to make tests pass
4. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
