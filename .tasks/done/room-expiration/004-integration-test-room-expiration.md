# 004 — Integration Test for Room Expiration

## What to build
An integration test that verifies the full room expiration flow: create rooms, simulate staleness, trigger the scheduler, and confirm only expired rooms are deleted.

## Acceptance Criteria
- [ ] Integration test boots full Spring context
- [ ] Tests verify expired rooms are deleted after scheduler runs
- [ ] Tests verify active rooms survive the cleanup
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `RoomCleanupIntegrationTest.java` | `com.yanajiki.application.bingoapp.service` (test) | Full-context integration test |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomControllerIntegrationTest.java` | `@SpringBootTest(RANDOM_PORT)`, `@ActiveProfiles("test")`, setup/teardown pattern |
| `RoomCleanupScheduler.java` | The method to invoke directly |
| `RoomRepository.java` | Available query methods |
| `RoomEntity.java` | Field names, factory method |

### Implementation Details

**RoomCleanupIntegrationTest.java:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RoomCleanupIntegrationTest {

	@Autowired
	private RoomCleanupScheduler roomCleanupScheduler;

	@Autowired
	private RoomRepository roomRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@AfterEach
	void tearDown() {
		roomRepository.deleteAll();
	}
```

Use `@Transactional` on test methods that manipulate timestamps via native query, or use `@PersistenceContext` EntityManager directly.

**To simulate stale rooms**, after saving via `roomRepository.save()`, run a native UPDATE query to set `update_date_time` to 25 hours ago (same approach as task 002 tests). Then call `roomCleanupScheduler.cleanupExpiredRooms()` directly — don't rely on the scheduler timer in tests.

**Important:** After native UPDATE, clear the persistence context (`entityManager.clear()`) so subsequent reads hit the DB, not the cache.

Test cases:

1. **Expired room is deleted** — Create a room, set its `updateDateTime` to 25h ago via native query, invoke `cleanupExpiredRooms()`, assert `roomRepository.count()` is 0.

2. **Active room survives cleanup** — Create a room (fresh, updateDateTime is now), invoke `cleanupExpiredRooms()`, assert `roomRepository.count()` is 1.

3. **Mixed — only expired rooms are deleted** — Create 3 rooms. Age 2 of them past TTL. Invoke cleanup. Assert only the fresh room remains (verify by sessionCode).

4. **Room with recent draw survives** — Create a room, age it past TTL, then simulate a draw (add a number + save — this resets `updateDateTime`). Invoke cleanup. Assert room survives.

Use `@Nested` + `@DisplayName` organization. Use AssertJ assertions.

For the native query to update timestamps:
```java
@Transactional
void ageRoom(Long roomId, int hoursAgo) {
    entityManager.createNativeQuery(
        "UPDATE room SET update_date_time = :time WHERE id = :id")
        .setParameter("time", Instant.now().minus(hoursAgo, ChronoUnit.HOURS))
        .setParameter("id", roomId)
        .executeUpdate();
    entityManager.flush();
    entityManager.clear();
}
```

Note: Since this test calls `cleanupExpiredRooms()` directly (not via the scheduler timer), `@EnableScheduling` configuration doesn't affect test reliability.

### Conventions (from project CLAUDE.md)
- Java 21
- Tabs for indentation
- `@SpringBootTest(RANDOM_PORT)` + `@ActiveProfiles("test")` for integration tests
- `@Nested` + `@DisplayName` for test organization
- AssertJ for assertions
- Cleanup in `@AfterEach`

## TDD Sequence
1. Write `RoomCleanupIntegrationTest.java` — all test cases (they should pass since scheduler + query are already implemented)
2. Run `mvn test` — all tests must pass
3. If any test fails, investigate and fix

## Done Definition
All acceptance criteria checked. Integration tests pass. Full cleanup flow verified end-to-end. Tests green. No compilation warnings.
