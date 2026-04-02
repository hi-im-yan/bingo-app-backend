package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.database.PlayerRepository;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.ConflictException;
import com.yanajiki.application.bingoapp.service.RoomService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for the player join tracker feature.
 * <p>
 * Boots the full Spring context on a random port, backed by an in-memory H2 database
 * (test profile). Each test is isolated: players are purged first (FK), then rooms.
 * </p>
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
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
		// Players must be deleted before rooms due to FK constraint
		playerRepository.deleteAll();
		roomRepository.deleteAll();
	}

	// ─── Helper ──────────────────────────────────────────────────────────────────

	/**
	 * Creates a room via REST and returns an array containing [sessionCode, creatorHash].
	 *
	 * @param roomName the room name to create
	 * @return String array where [0] = sessionCode, [1] = creatorHash
	 */
	private String[] createRoom(String roomName) {
		var response = given()
			.contentType(ContentType.JSON)
			.body("""
				{
					"name": "%s"
				}
				""".formatted(roomName))
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200)
			.extract()
			.response();

		return new String[]{
			response.path("sessionCode"),
			response.path("creatorHash")
		};
	}

	// ─── GET /api/v1/room/{session-code}/players ─────────────────────────────────

	@Nested
	@DisplayName("GET /api/v1/room/{session-code}/players")
	class GetPlayers {

		/**
		 * A newly created room with no players should return HTTP 200 and an empty array.
		 */
		@Test
		@DisplayName("should return empty list when no players have joined")
		void shouldReturnEmptyListWhenNoPlayersJoined() {
			// Arrange: create a room
			String[] credentials = createRoom("Empty Player Room");
			String sessionCode = credentials[0];
			String creatorHash = credentials[1];

			// Act & Assert
			given()
				.header("X-Creator-Hash", creatorHash)
			.when()
				.get("/api/v1/room/{sessionCode}/players", sessionCode)
			.then()
				.statusCode(200)
				.body("$", hasSize(0));
		}

		/**
		 * After two players join, the endpoint should return HTTP 200 with an array of size 2
		 * containing both player names.
		 */
		@Test
		@DisplayName("should return player list after players join")
		void shouldReturnPlayerListAfterPlayersJoin() {
			// Arrange: create a room and join two players
			String[] credentials = createRoom("Room With Players");
			String sessionCode = credentials[0];
			String creatorHash = credentials[1];

			roomService.joinRoom(sessionCode, "Alice");
			roomService.joinRoom(sessionCode, "Bob");

			// Act & Assert
			given()
				.header("X-Creator-Hash", creatorHash)
			.when()
				.get("/api/v1/room/{sessionCode}/players", sessionCode)
			.then()
				.statusCode(200)
				.body("$", hasSize(2))
				.body("name", hasItems("Alice", "Bob"));
		}

		/**
		 * Using a session code that does not match any room returns HTTP 404 NOT FOUND.
		 */
		@Test
		@DisplayName("should return 404 when session code is invalid")
		void shouldReturn404WhenSessionCodeIsInvalid() {
			given()
				.header("X-Creator-Hash", "any-hash")
			.when()
				.get("/api/v1/room/{sessionCode}/players", "INVALID")
			.then()
				.statusCode(404)
				.body("status", equalTo(404))
				.body("code", equalTo("ROOM_NOT_FOUND"))
				.body("message", notNullValue());
		}

		/**
		 * Using the wrong creator hash for a valid room returns HTTP 404 NOT FOUND.
		 */
		@Test
		@DisplayName("should return 404 when creator hash is invalid")
		void shouldReturn404WhenCreatorHashIsInvalid() {
			// Arrange: create a room
			String[] credentials = createRoom("Hash Mismatch Room");
			String sessionCode = credentials[0];

			// Act & Assert: request with wrong hash
			given()
				.header("X-Creator-Hash", "wrong-hash")
			.when()
				.get("/api/v1/room/{sessionCode}/players", sessionCode)
			.then()
				.statusCode(404)
				.body("status", equalTo(404))
				.body("code", equalTo("ROOM_NOT_FOUND"))
				.body("message", notNullValue());
		}
	}

	// ─── Player Join ──────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Player Join")
	class PlayerJoin {

		/**
		 * Joining a room with a unique player name should succeed,
		 * and the player should appear in the creator's player list.
		 */
		@Test
		@DisplayName("should register player successfully")
		void shouldRegisterPlayerSuccessfully() {
			// Arrange: create a room and join a player via service
			String[] credentials = createRoom("Charlie's Room");
			String sessionCode = credentials[0];
			String creatorHash = credentials[1];

			roomService.joinRoom(sessionCode, "Charlie");

			// Act & Assert: player should appear in the list
			given()
				.header("X-Creator-Hash", creatorHash)
			.when()
				.get("/api/v1/room/{sessionCode}/players", sessionCode)
			.then()
				.statusCode(200)
				.body("name", hasItem("Charlie"));
		}

		/**
		 * Attempting to join a room with a player name that is already taken in that room
		 * must throw a {@link ConflictException} (HTTP 409).
		 */
		@Test
		@DisplayName("should reject duplicate player name in same room")
		void shouldRejectDuplicatePlayerNameInSameRoom() {
			// Arrange: create a room and join "Alice" once
			String[] credentials = createRoom("Duplicate Name Room");
			String sessionCode = credentials[0];

			roomService.joinRoom(sessionCode, "Alice");

			// Act & Assert: joining again with the same name throws ConflictException
			assertThatThrownBy(() -> roomService.joinRoom(sessionCode, "Alice"))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("already taken");
		}
	}
}
