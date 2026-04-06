package com.yanajiki.application.bingoapp.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FeedbackMessageEntity}.
 * <p>
 * Covers the static factory method {@link FeedbackMessageEntity#create} to ensure
 * all fields are correctly set on the created entity.
 * No Spring context is loaded — these are pure JUnit 5 tests.
 * </p>
 */
class FeedbackMessageEntityTest {

	@Nested
	@DisplayName("create")
	class Create {

		/**
		 * The factory method must set the name field from the argument.
		 */
		@Test
		@DisplayName("sets name from argument")
		void setsName() {
			// when
			FeedbackMessageEntity entity = FeedbackMessageEntity.create("John Doe", null, null, "Hello");

			// then
			assertThat(entity.getName()).isEqualTo("John Doe");
		}

		/**
		 * The factory method must set the email field from the argument.
		 */
		@Test
		@DisplayName("sets email from argument")
		void setsEmail() {
			// when
			FeedbackMessageEntity entity = FeedbackMessageEntity.create("Jane", "jane@example.com", null, "Content");

			// then
			assertThat(entity.getEmail()).isEqualTo("jane@example.com");
		}

		/**
		 * The factory method must set the phone field from the argument.
		 */
		@Test
		@DisplayName("sets phone from argument")
		void setsPhone() {
			// when
			FeedbackMessageEntity entity = FeedbackMessageEntity.create("Bob", null, "+5511999999999", "Content");

			// then
			assertThat(entity.getPhone()).isEqualTo("+5511999999999");
		}

		/**
		 * The factory method must set the content field from the argument.
		 */
		@Test
		@DisplayName("sets content from argument")
		void setsContent() {
			// when
			FeedbackMessageEntity entity = FeedbackMessageEntity.create("Alice", null, null, "Great app!");

			// then
			assertThat(entity.getContent()).isEqualTo("Great app!");
		}

		/**
		 * The factory method must accept null values for optional email and phone fields.
		 */
		@Test
		@DisplayName("accepts null email and phone")
		void acceptsNullEmailAndPhone() {
			// when
			FeedbackMessageEntity entity = FeedbackMessageEntity.create("Alice", null, null, "Great app!");

			// then
			assertThat(entity.getEmail()).isNull();
			assertThat(entity.getPhone()).isNull();
		}

		/**
		 * The factory method must set all fields together correctly.
		 */
		@Test
		@DisplayName("sets all fields correctly")
		void setsAllFields() {
			// given
			String name = "Carlos";
			String email = "carlos@example.com";
			String phone = "+5511988887777";
			String content = "This is feedback content.";

			// when
			FeedbackMessageEntity entity = FeedbackMessageEntity.create(name, email, phone, content);

			// then
			assertThat(entity.getName()).isEqualTo(name);
			assertThat(entity.getEmail()).isEqualTo(email);
			assertThat(entity.getPhone()).isEqualTo(phone);
			assertThat(entity.getContent()).isEqualTo(content);
		}

		/**
		 * The factory method must leave id and createdAt as null since they are
		 * managed by the database and Hibernate respectively.
		 */
		@Test
		@DisplayName("leaves id and createdAt as null before persistence")
		void leavesIdAndCreatedAtNull() {
			// when
			FeedbackMessageEntity entity = FeedbackMessageEntity.create("Test", null, null, "Content");

			// then
			assertThat(entity.getId()).isNull();
			assertThat(entity.getCreatedAt()).isNull();
		}
	}
}
