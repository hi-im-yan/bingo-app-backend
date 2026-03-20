package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
	 * A valid creation request returns HTTP 200 with a populated sessionCode and creatorHash.
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
			.body("creatorHash", notNullValue());
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
}
