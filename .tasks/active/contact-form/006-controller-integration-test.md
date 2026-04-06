# 006 — ContactController + Integration Test

## What to build
REST controller for `POST /api/v1/contact` with OpenAPI docs, and a full integration test using REST Assured.

## Acceptance Criteria
- [ ] `POST /api/v1/contact` with valid payload returns 200 + ContactMessageDTO
- [ ] Returns 400 when name is blank
- [ ] Returns 400 when content is blank
- [ ] Returns 200 with no email and no phone (anonymous)
- [ ] Returns 200 with email only
- [ ] Returns 200 with phone only
- [ ] Returns 400 when email format is invalid
- [ ] Record persisted to database after successful POST
- [ ] OpenAPI annotations present (@Tag, @Operation, @ApiResponses)
- [ ] Integration test covers all above cases
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `ContactController.java` | `com.yanajiki.application.bingoapp.api` | REST controller |
| `ContactControllerIntegrationTest.java` | `com.yanajiki.application.bingoapp.api` (test) | Integration test |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `api/RoomController.java` | @RestController, @RequestMapping, OpenAPI annotations, @Valid |
| `api/RoomControllerIntegrationTest.java` | @SpringBootTest, REST Assured given/when/then, @AfterEach teardown |

### Implementation Details

**Controller:**
```java
@Tag(name = "Contact", description = "Contact form submission")
@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

	private final ContactService contactService;

	@Operation(
		summary = "Submit a contact message",
		description = "Saves the message and fires an async Discord notification."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Message received"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "Validation error"
		)
	})
	@PostMapping
	public ContactMessageDTO submit(@Valid @RequestBody ContactForm form) {
		return contactService.submit(form);
	}
}
```

**Integration test structure:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ContactControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private ContactMessageRepository contactMessageRepository;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
	}

	@AfterEach
	void tearDown() {
		contactMessageRepository.deleteAll();
	}

	@Nested
	@DisplayName("POST /api/v1/contact")
	class PostContact {

		@Test
		@DisplayName("returns 200 with anonymous submission (no email, no phone)")
		void shouldReturn200Anonymous() { ... }

		@Test
		@DisplayName("returns 200 with email only")
		void shouldReturn200WithEmail() { ... }

		@Test
		@DisplayName("returns 200 with phone only")
		void shouldReturn200WithPhone() { ... }

		@Test
		@DisplayName("returns 200 with both email and phone")
		void shouldReturn200WithBoth() { ... }

		@Test
		@DisplayName("returns 400 when name is blank")
		void shouldReturn400WhenNameBlank() { ... }

		@Test
		@DisplayName("returns 400 when content is blank")
		void shouldReturn400WhenContentBlank() { ... }

		@Test
		@DisplayName("returns 400 when email format is invalid")
		void shouldReturn400WhenEmailInvalid() { ... }

		@Test
		@DisplayName("persists to database")
		void shouldPersistToDatabase() { ... }
	}
}
```

Discord webhook URL is blank in dev/test profile, so `DiscordNotifier.notify()` short-circuits — no mocking needed in integration tests.

### Conventions (from project CLAUDE.md)
- `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`, `@Slf4j`
- OpenAPI: `@Tag`, `@Operation`, `@ApiResponses`
- `@Valid @RequestBody` for form validation
- Integration tests: `@SpringBootTest(RANDOM_PORT)`, `@ActiveProfiles("test")`, REST Assured
- `@AfterEach` cleanup with `repository.deleteAll()`
- Tabs for indentation

## TDD Sequence
1. Write `ContactControllerIntegrationTest` — all test cases
2. Write `ContactController` — make tests pass
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
