# 001 — Migrate Timestamps from LocalDateTime to Instant

## What to build
Change `createDateTime` and `updateDateTime` in `RoomEntity` from `LocalDateTime` to `Instant` so all timestamps are stored in UTC. Update the factory method, existing tests, and any other code that references these fields.

## Acceptance Criteria
- [ ] `RoomEntity.createDateTime` is `Instant` type
- [ ] `RoomEntity.updateDateTime` is `Instant` type
- [ ] Factory method `createEntityObject` sets both fields using `Instant.now()`
- [ ] `@CreationTimestamp` and `@UpdateTimestamp` work correctly with `Instant` (remove `@Temporal` — not needed for `Instant`)
- [ ] All existing tests pass (`mvn test`)
- [ ] No compilation warnings

## Technical Spec

### Files to MODIFY
| File | Package/Path | What to change |
|------|-------------|----------------|
| `RoomEntity.java` | `com.yanajiki.application.bingoapp.database` | Change field types, imports, factory method |

### Files to READ (for patterns — do NOT modify unless tests fail)
| File | What to check |
|------|--------------|
| `RoomEntityTest.java` | Verify no tests assert on `LocalDateTime` type directly |
| `RoomServiceTest.java` | Verify no tests reference timestamp fields |
| `RoomControllerIntegrationTest.java` | Verify no tests reference timestamp fields |
| `RoomDTO.java` | Verify timestamps are NOT exposed in the DTO (they aren't — no changes needed) |

### Implementation Details

**RoomEntity.java changes:**

1. Replace import:
   - Remove: `import java.time.LocalDateTime;`
   - Add: `import java.time.Instant;`

2. Change field declarations (lines 61-67):
   ```java
   @CreationTimestamp
   private Instant createDateTime;

   @UpdateTimestamp
   private Instant updateDateTime;
   ```
   - Remove both `@Temporal(TemporalType.TIMESTAMP)` annotations — `@Temporal` is for `java.util.Date`/`Calendar`/`LocalDateTime`, not `Instant`. Hibernate 6+ maps `Instant` to `TIMESTAMP WITH TIME ZONE` natively.

3. Update factory method `createEntityObject(String, String, DrawMode)` (lines 90-103):
   ```java
   roomEntity.setCreateDateTime(Instant.now());
   roomEntity.setUpdateDateTime(Instant.now());
   ```

### Conventions (from project CLAUDE.md)
- Java 21, Lombok for boilerplate
- Tabs for indentation
- Javadoc on all classes and public methods
- Tests use dev profile (H2) by default

## TDD Sequence
1. Read existing `RoomEntityTest.java` — confirm no tests break from the type change (they shouldn't — no test asserts on timestamp type)
2. Modify `RoomEntity.java` — change types from `LocalDateTime` to `Instant`, remove `@Temporal`
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. `Instant` used everywhere for timestamps. Tests green. No compilation warnings.
