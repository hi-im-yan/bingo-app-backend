# 002 — Add Repository Query for Expired Rooms

## What to build
Add a Spring Data query method to `RoomRepository` that finds all rooms whose `updateDateTime` is before a given `Instant` cutoff. This query will be used by the cleanup scheduler to find expired rooms.

## Acceptance Criteria
- [ ] `RoomRepository` has a `findByUpdateDateTimeBefore(Instant cutoff)` method returning `List<RoomEntity>`
- [ ] Unit test verifies the query method contract
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Package/Path | What to change |
|------|-------------|----------------|
| `RoomRepository.java` | `com.yanajiki.application.bingoapp.database` | Add query method |

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `RoomRepositoryTest.java` | `com.yanajiki.application.bingoapp.database` (test) | Spring Data JPA test for the new query |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomRepository.java` | Existing query method style |
| `RoomEntity.java` | Field names and types (after task 001 migration) |
| `RoomControllerIntegrationTest.java` | `@SpringBootTest` + `@ActiveProfiles("test")` pattern |

### Implementation Details

**RoomRepository.java — add method:**
```java
List<RoomEntity> findByUpdateDateTimeBefore(Instant cutoff);
```
Add import: `import java.time.Instant;` and `import java.util.List;`

**RoomRepositoryTest.java — Spring Data JPA slice test:**

Use `@DataJpaTest` + `@ActiveProfiles("test")` for a lightweight JPA test (no full context boot).

```java
@DataJpaTest
@ActiveProfiles("test")
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TestEntityManager entityManager;
```

Test cases:
1. **Finds expired rooms** — Create a room, use `entityManager` to directly set `updateDateTime` to 25 hours ago, flush. Call `findByUpdateDateTimeBefore(Instant.now().minus(24, ChronoUnit.HOURS))`. Assert the room is returned.
2. **Does not find active rooms** — Create a room (updateDateTime is "now" by default). Call `findByUpdateDateTimeBefore(Instant.now().minus(24, ChronoUnit.HOURS))`. Assert empty result.
3. **Mixed — only returns expired** — Create two rooms, age one past TTL, leave the other fresh. Assert only the expired one is returned.

Use `@Nested` + `@DisplayName` organization. Use AssertJ assertions.

To manipulate `updateDateTime` in tests, after saving the entity via repository, use `entityManager` to run a native update:
```java
entityManager.getEntityManager()
    .createNativeQuery("UPDATE room SET update_date_time = :time WHERE id = :id")
    .setParameter("time", Instant.now().minus(25, ChronoUnit.HOURS))
    .setParameter("id", entity.getId())
    .executeUpdate();
entityManager.flush();
entityManager.clear();
```
This bypasses `@UpdateTimestamp` which would overwrite the value on save.

### Conventions (from project CLAUDE.md)
- Java 21, Lombok
- Tabs for indentation
- `@Nested` + `@DisplayName` for test organization
- AssertJ for assertions
- Tests use dev/test profile (H2)

## TDD Sequence
1. Write `RoomRepositoryTest.java` — all three test cases (they will fail — method doesn't exist yet)
2. Add `findByUpdateDateTimeBefore` to `RoomRepository.java`
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Query method exists and is tested. Tests green. No compilation warnings.
