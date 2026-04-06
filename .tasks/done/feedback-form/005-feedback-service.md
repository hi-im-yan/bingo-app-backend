# 005 — FeedbackService

## What to build
Service layer that receives a FeedbackForm, saves it as a FeedbackMessageEntity, triggers async Discord notification, and returns a FeedbackMessageDTO.

## Acceptance Criteria
- [ ] `FeedbackService.submit(FeedbackForm)` saves entity to database
- [ ] Returns `FeedbackMessageDTO` with all fields populated
- [ ] Calls `DiscordNotifier.notify()` with the saved entity
- [ ] Unit test covers: save + return DTO, Discord notifier called, null email/phone handled
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `FeedbackService.java` | `com.yanajiki.application.bingoapp.service` | Business logic |
| `FeedbackServiceTest.java` | `com.yanajiki.application.bingoapp.service` (test) | Unit test |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `service/RoomService.java` | Service structure, logging, constructor injection |
| `service/RoomServiceTest.java` | @Mock, @InjectMocks, @Nested, @DisplayName pattern |

### Implementation Details
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

	private final FeedbackMessageRepository repository;
	private final DiscordNotifier discordNotifier;

	public FeedbackMessageDTO submit(FeedbackForm form) {
		log.info("Saving feedback message from '{}'", form.getName());

		FeedbackMessageEntity entity = FeedbackMessageEntity.create(
			form.getName(),
			form.getEmail(),
			form.getPhone(),
			form.getContent()
		);

		FeedbackMessageEntity saved = repository.save(entity);
		log.info("Feedback message saved with id={}", saved.getId());

		discordNotifier.notify(saved);

		return FeedbackMessageDTO.fromEntity(saved);
	}
}
```

**Test structure:**
```java
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

	@Mock FeedbackMessageRepository repository;
	@Mock DiscordNotifier discordNotifier;
	@InjectMocks FeedbackService feedbackService;

	@Nested
	@DisplayName("submit")
	class Submit {

		@Test
		@DisplayName("saves entity and returns DTO with all fields")
		void shouldSaveAndReturnDTO() { ... }

		@Test
		@DisplayName("delegates notification to DiscordNotifier")
		void shouldCallDiscordNotifier() { ... }

		@Test
		@DisplayName("handles null email gracefully")
		void shouldHandleNullEmail() { ... }

		@Test
		@DisplayName("handles null phone gracefully")
		void shouldHandleNullPhone() { ... }
	}
}
```

Use `when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0))` to return the entity. Use `ArgumentCaptor` to verify saved entity fields.

### Conventions (from project CLAUDE.md)
- `@Service`, `@RequiredArgsConstructor`, `@Slf4j`
- Unit tests: `@ExtendWith(MockitoExtension.class)`, `@Nested`, `@DisplayName`
- Tabs for indentation

## TDD Sequence
1. Write `FeedbackServiceTest` — all test cases
2. Write `FeedbackService` — make tests pass
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
