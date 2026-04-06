# 005 — ContactService

## What to build
Service layer that receives a ContactForm, saves it as a ContactMessageEntity, triggers async Discord notification, and returns a ContactMessageDTO.

## Acceptance Criteria
- [ ] `ContactService.submit(ContactForm)` saves entity to database
- [ ] Returns `ContactMessageDTO` with all fields populated
- [ ] Calls `DiscordNotifier.notify()` with the saved entity
- [ ] Unit test covers: save + return DTO, Discord notifier called, null email/phone handled
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `ContactService.java` | `com.yanajiki.application.bingoapp.service` | Business logic |
| `ContactServiceTest.java` | `com.yanajiki.application.bingoapp.service` (test) | Unit test |

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
public class ContactService {

	private final ContactMessageRepository repository;
	private final DiscordNotifier discordNotifier;

	public ContactMessageDTO submit(ContactForm form) {
		log.info("Saving contact message from '{}'", form.getName());

		ContactMessageEntity entity = ContactMessageEntity.create(
			form.getName(),
			form.getEmail(),
			form.getPhone(),
			form.getContent()
		);

		ContactMessageEntity saved = repository.save(entity);
		log.info("Contact message saved with id={}", saved.getId());

		discordNotifier.notify(saved);

		return ContactMessageDTO.fromEntity(saved);
	}
}
```

**Test structure:**
```java
@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

	@Mock ContactMessageRepository repository;
	@Mock DiscordNotifier discordNotifier;
	@InjectMocks ContactService contactService;

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
1. Write `ContactServiceTest` — all test cases
2. Write `ContactService` — make tests pass
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
