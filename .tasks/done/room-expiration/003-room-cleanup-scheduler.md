# 003 — RoomCleanupScheduler + @EnableScheduling

## What to build
Create a scheduled Spring component that runs periodically, finds rooms whose `updateDateTime` is older than the configured TTL, and deletes them. Enable Spring scheduling on the application class.

## Acceptance Criteria
- [ ] `@EnableScheduling` added to `Application.java`
- [ ] `RoomCleanupScheduler` exists in the `service` package
- [ ] Scheduler runs at a configurable interval (default: 1 hour)
- [ ] TTL is configurable (default: 24 hours)
- [ ] Expired rooms are found, logged, and deleted
- [ ] Active rooms are NOT deleted
- [ ] Unit test covers: rooms deleted, no rooms to delete, mixed scenario
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Package/Path | What to change |
|------|-------------|----------------|
| `Application.java` | `com.yanajiki.application.bingoapp` | Add `@EnableScheduling` |

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `RoomCleanupScheduler.java` | `com.yanajiki.application.bingoapp.service` | Scheduled cleanup component |
| `RoomCleanupSchedulerTest.java` | `com.yanajiki.application.bingoapp.service` (test) | Unit test with mocked repository |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomService.java` | `@Slf4j`, `@RequiredArgsConstructor`, logging style |
| `RoomServiceTest.java` | Mockito setup, `@ExtendWith(MockitoExtension.class)`, `@Nested`/`@DisplayName` |
| `RoomRepository.java` | Available methods including `findByUpdateDateTimeBefore` (from task 002) |

### Implementation Details

**Application.java — add annotation:**
```java
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class Application { ... }
```
Add import: `import org.springframework.scheduling.annotation.EnableScheduling;`

**RoomCleanupScheduler.java:**

```java
package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled task that removes expired bingo rooms.
 * <p>
 * A room is considered expired when its {@code updateDateTime} is older than
 * the configured TTL. The TTL resets on every entity save (i.e., every number draw).
 * Rooms where no number is ever drawn expire based on their creation time.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoomCleanupScheduler {

	private final RoomRepository roomRepository;

	@Value("${room.cleanup.ttl-hours:24}")
	private long ttlHours;

	/**
	 * Finds and deletes all rooms whose {@code updateDateTime} is older than the TTL.
	 * Runs at a fixed rate configured by {@code room.cleanup.interval-ms} (default: 1 hour).
	 */
	@Scheduled(fixedRateString = "${room.cleanup.interval-ms:3600000}")
	public void cleanupExpiredRooms() {
		Instant cutoff = Instant.now().minus(ttlHours, ChronoUnit.HOURS);
		List<RoomEntity> expiredRooms = roomRepository.findByUpdateDateTimeBefore(cutoff);

		if (expiredRooms.isEmpty()) {
			log.debug("No expired rooms found");
			return;
		}

		log.info("Found {} expired room(s) to clean up", expiredRooms.size());
		expiredRooms.forEach(room ->
			log.debug("Deleting expired room '{}' (sessionCode: {}, lastUpdated: {})",
				room.getName(), room.getSessionCode(), room.getUpdateDateTime())
		);

		roomRepository.deleteAll(expiredRooms);
		log.info("Deleted {} expired room(s)", expiredRooms.size());
	}
}
```

**Configuration properties to add to `application.properties`:**
```properties
# Room cleanup scheduler
room.cleanup.ttl-hours=24
room.cleanup.interval-ms=3600000
```

**RoomCleanupSchedulerTest.java:**

Use `@ExtendWith(MockitoExtension.class)` with mocked `RoomRepository`. Inject TTL via ReflectionTestUtils.

Test cases (all under `@Nested @DisplayName("cleanupExpiredRooms")`):

1. **Deletes expired rooms** — Mock `findByUpdateDateTimeBefore` to return a list with 2 rooms. Verify `deleteAll` is called with those rooms.
2. **No expired rooms — skips deletion** — Mock `findByUpdateDateTimeBefore` to return empty list. Verify `deleteAll` is never called.
3. **Passes correct cutoff based on TTL** — Use `ArgumentCaptor<Instant>` on `findByUpdateDateTimeBefore`. Verify the captured cutoff is approximately `Instant.now().minus(ttlHours, HOURS)` (within a 5-second tolerance using AssertJ `isCloseTo`).

### Conventions (from project CLAUDE.md)
- Java 21, Lombok (`@RequiredArgsConstructor`, `@Slf4j`)
- Tabs for indentation
- Javadoc on class and public methods
- SLF4J for logging — INFO for actions taken, DEBUG for details
- Mockito for unit tests, `@Nested` + `@DisplayName` organization
- AssertJ for assertions

## TDD Sequence
1. Write `RoomCleanupSchedulerTest.java` — all test cases
2. Write `RoomCleanupScheduler.java` — make tests pass
3. Add `@EnableScheduling` to `Application.java`
4. Add config properties to `application.properties`
5. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Scheduler is wired, configured, and tested. Tests green. No compilation warnings.
