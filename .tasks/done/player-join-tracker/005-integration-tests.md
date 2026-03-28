# 005 — Integration Tests

## What to build
End-to-end integration tests for the player join tracker feature covering the REST endpoint for listing players and the service-level join flow. Tests validate happy paths and error cases using RestAssured with BDD-style syntax.

## Acceptance Criteria
- [ ] Integration test for GET /api/v1/room/{session-code}/players (creator view)
- [ ] Test: returns empty list for room with no players
- [ ] Test: returns player list after players join
- [ ] Test: returns 404 when session code is invalid
- [ ] Test: returns 404 when creator hash is invalid
- [ ] Test: returns 409 when player name is duplicate in room
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `PlayerJoinIntegrationTest.java` | `com.yanajiki.application.bingoapp.api` (test) | Integration tests for player join feature |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomControllerIntegrationTest.java` | @SpringBootTest setup, RestAssured config, @BeforeEach/@AfterEach, BDD style |
| `RoomController.java` | Endpoint paths for creating rooms (setup step) |

### Implementation Details

**Test class setup**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PlayerJoinIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private PlayerRepository playerRepository;

	@Autowired
	private RoomService roomService;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
	}

	@AfterEach
	void tearDown() {
		playerRepository.deleteAll();
		roomRepository.deleteAll();
	}
}
```

**Test cases** (all use @Nested + @DisplayName):

```
@Nested @DisplayName("GET /api/v1/room/{session-code}/players")

	@Test @DisplayName("should return empty list when no players have joined")
	- Create a room via POST /api/v1/room
	- GET /api/v1/room/{sessionCode}/players with X-Creator-Hash
	- Assert 200 and empty array

	@Test @DisplayName("should return player list after players join")
	- Create a room via POST
	- Join 2 players via roomService.joinRoom(sessionCode, "Alice") and ("Bob")
	- GET /api/v1/room/{sessionCode}/players with X-Creator-Hash
	- Assert 200, array size 2, contains "Alice" and "Bob"

	@Test @DisplayName("should return 404 when session code is invalid")
	- GET /api/v1/room/INVALID/players with any X-Creator-Hash
	- Assert 404

	@Test @DisplayName("should return 404 when creator hash is invalid")
	- Create a room
	- GET /api/v1/room/{sessionCode}/players with wrong X-Creator-Hash
	- Assert 404

@Nested @DisplayName("Player Join")

	@Test @DisplayName("should register player successfully")
	- Create a room
	- Call roomService.joinRoom(sessionCode, "Charlie")
	- GET /api/v1/room/{sessionCode}/players with X-Creator-Hash
	- Assert player "Charlie" is in the list

	@Test @DisplayName("should reject duplicate player name in same room")
	- Create a room
	- Join player "Alice" via roomService.joinRoom
	- Try to join "Alice" again
	- Assert ConflictException (409)
```

### Conventions (from project CLAUDE.md)
- @SpringBootTest(webEnvironment = RANDOM_PORT), @ActiveProfiles("test")
- RestAssured BDD: given().when().then()
- @Nested + @DisplayName for organization
- @BeforeEach sets port, @AfterEach cleans DB (players first, then rooms due to FK)
- H2 in-memory for test profile
- Tabs for indentation

## TDD Sequence
1. Write all integration test cases (they will fail initially since they test the full stack)
2. Run test suite — all tests must pass (implementation from tasks 001-004 should be in place)
3. Fix any integration issues discovered

## Done Definition
All acceptance criteria checked. All integration tests green. Full test suite passes. No compilation warnings.
