# 006 — FeedbackController + Integration Test

## What to build
REST controller for `POST /api/v1/feedback` with OpenAPI docs, and a full integration test using REST Assured.

## Acceptance Criteria
- [ ] `POST /api/v1/feedback` with valid payload returns 200 + FeedbackMessageDTO
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
| `FeedbackController.java` | `com.yanajiki.application.bingoapp.api` | REST controller |
| `FeedbackControllerIntegrationTest.java` | `com.yanajiki.application.bingoapp.api` (test) | Integration test |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `api/RoomController.java` | @RestController, @RequestMapping, OpenAPI annotations, @Valid |
| `api/RoomControllerIntegrationTest.java` | @SpringBootTest, REST Assured given/when/then, @AfterEach teardown |

### Implementation Details

**Controller:**
```java
@Tag(name = "Feedback", description = "Feedback form submission")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

	private final FeedbackService feedbackService;

	@Operation(
		summary = "Submit a feedback message",
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
	public FeedbackMessageDTO submit(@Valid @RequestBody FeedbackForm form) {
		return feedbackService.submit(form);
	}
}
```

**Integration test structure:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FeedbackControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private FeedbackMessageRepository feedbackMessageRepository;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
	}

	@AfterEach
	void tearDown() {
		feedbackMessageRepository.deleteAll();
	}

	@Nested
	@DisplayName("POST /api/v1/feedback")
	class PostFeedback {

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
1. Write `FeedbackControllerIntegrationTest` — all test cases
2. Write `FeedbackController` — make tests pass
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
