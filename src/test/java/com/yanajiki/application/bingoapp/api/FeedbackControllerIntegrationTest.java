package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.database.FeedbackMessageRepository;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for {@link FeedbackController}.
 * <p>
 * Boots the full Spring context on a random port, backed by an in-memory H2 database
 * (test profile). Each test is isolated: all feedback messages are purged after each test.
 * </p>
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
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
		void shouldReturn200Anonymous() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Anonymous User",
						"content": "Great bingo app!"
					}
					""")
			.when()
				.post("/api/v1/feedback")
			.then()
				.statusCode(200)
				.body("id", notNullValue())
				.body("name", equalTo("Anonymous User"))
				.body("content", equalTo("Great bingo app!"))
				.body("createdAt", notNullValue())
				.body("$", not(hasKey("email")))
				.body("$", not(hasKey("phone")));
		}

		@Test
		@DisplayName("returns 200 with email only")
		void shouldReturn200WithEmail() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Alice",
						"email": "alice@example.com",
						"content": "Love the real-time drawing!"
					}
					""")
			.when()
				.post("/api/v1/feedback")
			.then()
				.statusCode(200)
				.body("name", equalTo("Alice"))
				.body("email", equalTo("alice@example.com"))
				.body("$", not(hasKey("phone")));
		}

		@Test
		@DisplayName("returns 200 with phone only")
		void shouldReturn200WithPhone() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Bob",
						"phone": "555-1234",
						"content": "Works well on mobile"
					}
					""")
			.when()
				.post("/api/v1/feedback")
			.then()
				.statusCode(200)
				.body("name", equalTo("Bob"))
				.body("phone", equalTo("555-1234"))
				.body("$", not(hasKey("email")));
		}

		@Test
		@DisplayName("returns 200 with both email and phone")
		void shouldReturn200WithBoth() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Charlie",
						"email": "charlie@example.com",
						"phone": "555-5678",
						"content": "Feature request: 90-ball bingo"
					}
					""")
			.when()
				.post("/api/v1/feedback")
			.then()
				.statusCode(200)
				.body("name", equalTo("Charlie"))
				.body("email", equalTo("charlie@example.com"))
				.body("phone", equalTo("555-5678"))
				.body("content", equalTo("Feature request: 90-ball bingo"));
		}

		@Test
		@DisplayName("returns 400 when name is blank")
		void shouldReturn400WhenNameBlank() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "   ",
						"content": "Some feedback"
					}
					""")
			.when()
				.post("/api/v1/feedback")
			.then()
				.statusCode(400)
				.body("status", equalTo(400))
				.body("code", equalTo("VALIDATION_ERROR"));
		}

		@Test
		@DisplayName("returns 400 when content is blank")
		void shouldReturn400WhenContentBlank() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Dave",
						"content": ""
					}
					""")
			.when()
				.post("/api/v1/feedback")
			.then()
				.statusCode(400)
				.body("status", equalTo(400))
				.body("code", equalTo("VALIDATION_ERROR"));
		}

		@Test
		@DisplayName("returns 400 when email format is invalid")
		void shouldReturn400WhenEmailInvalid() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Eve",
						"email": "not-an-email",
						"content": "Testing validation"
					}
					""")
			.when()
				.post("/api/v1/feedback")
			.then()
				.statusCode(400)
				.body("status", equalTo(400))
				.body("code", equalTo("VALIDATION_ERROR"));
		}

		@Test
		@DisplayName("persists to database")
		void shouldPersistToDatabase() {
			given()
				.contentType(ContentType.JSON)
				.body("""
					{
						"name": "Frank",
						"email": "frank@example.com",
						"content": "Persistence check"
					}
					""")
			.when()
				.post("/api/v1/feedback")
			.then()
				.statusCode(200);

			var messages = feedbackMessageRepository.findAll();
			assertThat(messages).hasSize(1);
			assertThat(messages.get(0).getName()).isEqualTo("Frank");
			assertThat(messages.get(0).getEmail()).isEqualTo("frank@example.com");
			assertThat(messages.get(0).getContent()).isEqualTo("Persistence check");
		}
	}
}
