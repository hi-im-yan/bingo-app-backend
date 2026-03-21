package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for {@link RoomController}.
 * <p>
 * Boots the full Spring context on a random port, backed by an in-memory H2 database
 * (test profile). Each test is isolated: all rooms are purged after each test method.
 * </p>
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class RoomControllerIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private RoomRepository roomRepository;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
	}

	@AfterEach
	void tearDown() {
		roomRepository.deleteAll();
	}

	// ─── POST /api/v1/room ──────────────────────────────────────────────────────

	/**
	 * A valid creation request without explicit drawMode returns HTTP 200 with drawMode defaulting to MANUAL.
	 */
	@Test
	void shouldCreateRoomSuccessfully() {
		String requestBody = """
			{
				"name": "Friday Night Bingo",
				"description": "Weekly bingo session"
			}
			""";

		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200)
			.body("name", equalTo("Friday Night Bingo"))
			.body("description", equalTo("Weekly bingo session"))
			.body("sessionCode", notNullValue())
			.body("sessionCode", hasLength(6))
			.body("creatorHash", notNullValue())
			.body("drawMode", equalTo("MANUAL"));
	}

	/**
	 * A creation request with explicit AUTOMATIC drawMode returns HTTP 200 with drawMode set to AUTOMATIC.
	 */
	@Test
	void shouldCreateRoomWithAutomaticDrawMode() {
		String requestBody = """
			{
				"name": "Auto Draw Night",
				"description": "Server picks the numbers",
				"drawMode": "AUTOMATIC"
			}
			""";

		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200)
			.body("name", equalTo("Auto Draw Night"))
			.body("drawMode", equalTo("AUTOMATIC"));
	}

	/**
	 * Creating a second room with the same name as an existing room returns HTTP 409 CONFLICT.
	 */
	@Test
	void shouldReturn409WhenCreatingRoomWithDuplicateName() {
		String requestBody = """
			{
				"name": "Duplicate Room"
			}
			""";

		// First creation succeeds
		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200);

		// Second creation with the same name must fail with 409
		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(409)
			.body("status", equalTo(409))
			.body("message", notNullValue());
	}

	/**
	 * Submitting a blank room name returns HTTP 400 BAD REQUEST with a validation error message.
	 */
	@Test
	void shouldReturn400WhenCreatingRoomWithBlankName() {
		String requestBody = """
			{
				"name": "   "
			}
			""";

		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(400)
			.body("status", equalTo(400))
			.body("message", notNullValue());
	}

	// ─── GET /api/v1/room/{session-code} ────────────────────────────────────────

	/**
	 * Fetching an existing room without the X-Creator-Hash header returns HTTP 200
	 * in player view — creatorHash must be absent from the response.
	 */
	@Test
	void shouldFindRoomAsPlayer() {
		// Arrange: create a room and capture its sessionCode
		String sessionCode = given()
			.contentType(ContentType.JSON)
			.body("""
				{
					"name": "Player View Room"
				}
				""")
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200)
			.extract()
			.path("sessionCode");

		// Act & Assert: retrieve without creator header — creatorHash must be absent
		given()
		.when()
			.get("/api/v1/room/{sessionCode}", sessionCode)
		.then()
			.statusCode(200)
			.body("name", equalTo("Player View Room"))
			.body("sessionCode", equalTo(sessionCode))
			.body("$", not(hasKey("creatorHash")));
	}

	/**
	 * Fetching an existing room with a valid X-Creator-Hash header returns HTTP 200
	 * in creator view — creatorHash must be present in the response.
	 */
	@Test
	void shouldFindRoomAsCreator() {
		// Arrange: create a room and capture sessionCode and creatorHash
		var createResponse = given()
			.contentType(ContentType.JSON)
			.body("""
				{
					"name": "Creator View Room"
				}
				""")
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200)
			.extract()
			.response();

		String sessionCode = createResponse.path("sessionCode");
		String creatorHash = createResponse.path("creatorHash");

		// Act & Assert: retrieve with matching creator hash — creatorHash must be present
		given()
			.header("X-Creator-Hash", creatorHash)
		.when()
			.get("/api/v1/room/{sessionCode}", sessionCode)
		.then()
			.statusCode(200)
			.body("name", equalTo("Creator View Room"))
			.body("sessionCode", equalTo(sessionCode))
			.body("creatorHash", equalTo(creatorHash));
	}

	/**
	 * Fetching a room with a session code that does not exist returns HTTP 404 NOT FOUND.
	 */
	@Test
	void shouldReturn404WhenRoomNotFound() {
		given()
		.when()
			.get("/api/v1/room/{sessionCode}", "XXXXXX")
		.then()
			.statusCode(404)
			.body("status", equalTo(404))
			.body("message", notNullValue());
	}

	// ─── DELETE /api/v1/room/{session-code} ─────────────────────────────────────

	/**
	 * Deleting an existing room with the correct X-Creator-Hash header returns HTTP 200.
	 */
	@Test
	void shouldDeleteRoomSuccessfully() {
		// Arrange: create a room and capture sessionCode and creatorHash
		var createResponse = given()
			.contentType(ContentType.JSON)
			.body("""
				{
					"name": "Room To Delete"
				}
				""")
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200)
			.extract()
			.response();

		String sessionCode = createResponse.path("sessionCode");
		String creatorHash = createResponse.path("creatorHash");

		// Act & Assert: delete with valid creator hash succeeds
		given()
			.header("X-Creator-Hash", creatorHash)
		.when()
			.delete("/api/v1/room/{sessionCode}", sessionCode)
		.then()
			.statusCode(200);
	}

	/**
	 * Attempting to delete a room with a session code that does not exist returns HTTP 404 NOT FOUND.
	 */
	@Test
	void shouldReturn404WhenDeletingNonExistentRoom() {
		given()
			.header("X-Creator-Hash", "some-creator-hash")
		.when()
			.delete("/api/v1/room/{sessionCode}", "XXXXXX")
		.then()
			.statusCode(404)
			.body("status", equalTo(404))
			.body("message", notNullValue());
	}

	// ─── GET /api/v1/room/{session-code}/qrcode ──────────────────────────────

	/**
	 * Requesting the QR code for an existing room returns HTTP 200
	 * with content-type {@code image/png}.
	 */
	@Test
	void shouldReturnQrCodePngForExistingRoom() {
		// Arrange: create a room and capture its sessionCode
		String sessionCode = given()
			.contentType(ContentType.JSON)
			.body("""
				{
					"name": "QR Code Room"
				}
				""")
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200)
			.extract()
			.path("sessionCode");

		// Act & Assert: QR code endpoint returns a PNG image
		given()
		.when()
			.get("/api/v1/room/{sessionCode}/qrcode", sessionCode)
		.then()
			.statusCode(200)
			.contentType("image/png");
	}

	/**
	 * Requesting the QR code for a session code that does not exist returns HTTP 404 NOT FOUND.
	 */
	@Test
	void shouldReturn404WhenQrCodeRequestedForNonExistentRoom() {
		given()
		.when()
			.get("/api/v1/room/{sessionCode}/qrcode", "INVALID")
		.then()
			.statusCode(404)
			.body("status", equalTo(404))
			.body("message", notNullValue());
	}

	// ─── Draw Mode ──────────────────────────────────────────────────────────────

	/**
	 * Integration tests verifying automatic draw mode behaviour at the API level.
	 * <p>
	 * Covers room creation with {@code AUTOMATIC} mode, the default {@code MANUAL} fallback,
	 * and that {@code drawMode} is returned in both creator and player views of GET room.
	 * </p>
	 */
	@Nested
	@DisplayName("Automatic Draw Mode")
	class AutomaticDrawMode {

		/**
		 * A creation request that explicitly sets {@code drawMode} to {@code AUTOMATIC}
		 * returns HTTP 200 and the response contains {@code drawMode: "AUTOMATIC"}.
		 */
		@Test
		@DisplayName("should create room with AUTOMATIC draw mode")
		void shouldCreateRoomWithAutomaticDrawMode() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Auto Draw Room",
						"description": "Server picks the numbers",
						"drawMode": "AUTOMATIC"
					}
					""")
			.when()
				.post("/api/v1/room")
			.then()
				.statusCode(200)
				.body("name", equalTo("Auto Draw Room"))
				.body("drawMode", equalTo("AUTOMATIC"));
		}

		/**
		 * A creation request that omits {@code drawMode} returns HTTP 200 and
		 * the response contains {@code drawMode: "MANUAL"} as the default.
		 */
		@Test
		@DisplayName("should default to MANUAL draw mode when not specified")
		void shouldDefaultToManualDrawMode() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Default Mode Room"
					}
					""")
			.when()
				.post("/api/v1/room")
			.then()
				.statusCode(200)
				.body("name", equalTo("Default Mode Room"))
				.body("drawMode", equalTo("MANUAL"));
		}

		/**
		 * After creating a room with {@code AUTOMATIC} draw mode, a GET request
		 * authenticated with {@code X-Creator-Hash} returns {@code drawMode: "AUTOMATIC"}
		 * in the creator view.
		 */
		@Test
		@DisplayName("should return drawMode in GET room response for creator")
		void shouldReturnDrawModeInCreatorView() {
			// Arrange: create room with AUTOMATIC mode and capture credentials
			var createResponse = given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Creator View Auto Room",
						"drawMode": "AUTOMATIC"
					}
					""")
			.when()
				.post("/api/v1/room")
			.then()
				.statusCode(200)
				.extract()
				.response();

			String sessionCode = createResponse.path("sessionCode");
			String creatorHash = createResponse.path("creatorHash");

			// Act & Assert: GET with creator hash returns AUTOMATIC drawMode
			given()
				.header("X-Creator-Hash", creatorHash)
			.when()
				.get("/api/v1/room/{sessionCode}", sessionCode)
			.then()
				.statusCode(200)
				.body("drawMode", equalTo("AUTOMATIC"))
				.body("creatorHash", equalTo(creatorHash));
		}

		/**
		 * After creating a room with {@code AUTOMATIC} draw mode, a GET request
		 * without {@code X-Creator-Hash} returns {@code drawMode: "AUTOMATIC"}
		 * in the player view.
		 */
		@Test
		@DisplayName("should return drawMode in GET room response for player")
		void shouldReturnDrawModeInPlayerView() {
			// Arrange: create room with AUTOMATIC mode and capture session code
			String sessionCode = given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Player View Auto Room",
						"drawMode": "AUTOMATIC"
					}
					""")
			.when()
				.post("/api/v1/room")
			.then()
				.statusCode(200)
				.extract()
				.path("sessionCode");

			// Act & Assert: GET without creator hash returns AUTOMATIC drawMode and no creatorHash
			given()
			.when()
				.get("/api/v1/room/{sessionCode}", sessionCode)
			.then()
				.statusCode(200)
				.body("drawMode", equalTo("AUTOMATIC"))
				.body("$", not(hasKey("creatorHash")));
		}
	}
}
