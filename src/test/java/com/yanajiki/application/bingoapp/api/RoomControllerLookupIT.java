package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.database.RoomRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for the {@code POST /api/v1/room/lookup} endpoint.
 * <p>
 * Covers the full range of inputs: all-found, partial match, no match, and empty/missing list.
 * The endpoint must always return HTTP 200 with a (possibly empty) list — it never throws 404.
 * </p>
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class RoomControllerLookupIT {

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

	/**
	 * Creates a room via the public API and returns the full create response
	 * so callers can extract {@code sessionCode} and {@code creatorHash}.
	 */
	private Response createRoom(String name) {
		return given()
			.contentType(ContentType.JSON)
			.body("""
				{
					"name": "%s"
				}
				""".formatted(name))
		.when()
			.post("/api/v1/room")
		.then()
			.statusCode(200)
			.extract()
			.response();
	}

	@Test
	@DisplayName("should return all rooms when every hash matches")
	void shouldReturnAllRoomsWhenAllHashesMatch() {
		// Given: three rooms exist, and we capture all three creator hashes
		String hashA = createRoom("Lookup Room A").path("creatorHash");
		String hashB = createRoom("Lookup Room B").path("creatorHash");
		String hashC = createRoom("Lookup Room C").path("creatorHash");

		String requestBody = """
			{
				"creatorHashes": ["%s", "%s", "%s"]
			}
			""".formatted(hashA, hashB, hashC);

		// When/Then: the lookup returns all three rooms in creator view
		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room/lookup")
		.then()
			.statusCode(200)
			.body("size()", equalTo(3))
			.body("creatorHash", hasItems(hashA, hashB, hashC))
			.body("name", hasItems("Lookup Room A", "Lookup Room B", "Lookup Room C"));
	}

	@Test
	@DisplayName("should return only matching rooms and silently skip unknown hashes")
	void shouldReturnPartialMatchAndSkipUnknownHashes() {
		// Given: two existing rooms plus one bogus hash in the request
		String hashA = createRoom("Partial Room A").path("creatorHash");
		String hashB = createRoom("Partial Room B").path("creatorHash");

		String requestBody = """
			{
				"creatorHashes": ["%s", "not-a-real-hash", "%s"]
			}
			""".formatted(hashA, hashB);

		// When/Then: only the two known rooms come back, no 404, no error
		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room/lookup")
		.then()
			.statusCode(200)
			.body("size()", equalTo(2))
			.body("creatorHash", hasItems(hashA, hashB));
	}

	@Test
	@DisplayName("should return empty list when no hashes match")
	void shouldReturnEmptyListWhenNoHashesMatch() {
		// Given: a room exists but none of the requested hashes point to it
		createRoom("Unrelated Room");

		String requestBody = """
			{
				"creatorHashes": ["ghost-1", "ghost-2"]
			}
			""";

		// When/Then: 200 OK with an empty list
		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room/lookup")
		.then()
			.statusCode(200)
			.body("$", hasSize(0));
	}

	@Test
	@DisplayName("should return empty list when creatorHashes is an empty array")
	void shouldReturnEmptyListWhenCreatorHashesIsEmpty() {
		// Given: a valid request with an empty list
		String requestBody = """
			{
				"creatorHashes": []
			}
			""";

		// When/Then: 200 OK with an empty list
		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room/lookup")
		.then()
			.statusCode(200)
			.body("$", hasSize(0));
	}

	@Test
	@DisplayName("should return empty list when creatorHashes field is missing")
	void shouldReturnEmptyListWhenBodyOmitsCreatorHashes() {
		// Given: an empty JSON object — the field is missing entirely
		String requestBody = "{}";

		// When/Then: 200 OK with an empty list, field treated as null/empty
		given()
			.contentType(ContentType.JSON)
			.body(requestBody)
		.when()
			.post("/api/v1/room/lookup")
		.then()
			.statusCode(200)
			.body("$", hasSize(0));
	}
}
