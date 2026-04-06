package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.form.FeedbackForm;
import com.yanajiki.application.bingoapp.api.response.FeedbackMessageDTO;
import com.yanajiki.application.bingoapp.database.FeedbackMessageEntity;
import com.yanajiki.application.bingoapp.database.FeedbackMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FeedbackService}.
 * <p>
 * Uses Mockito to isolate the service from {@link FeedbackMessageRepository} and
 * {@link DiscordNotifier}, verifying persistence behaviour, DTO mapping, and
 * delegation to the notifier across all field combinations.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

	@Mock
	FeedbackMessageRepository repository;

	@Mock
	DiscordNotifier discordNotifier;

	@InjectMocks
	FeedbackService feedbackService;

	// ─── Helpers ─────────────────────────────────────────────────────────────

	/**
	 * Builds a {@link FeedbackForm} with all fields populated.
	 */
	private FeedbackForm buildForm(String name, String email, String phone, String content) {
		FeedbackForm form = new FeedbackForm();
		form.setName(name);
		form.setEmail(email);
		form.setPhone(phone);
		form.setContent(content);
		return form;
	}

	/**
	 * Builds a {@link FeedbackMessageEntity} with an assigned ID and timestamp,
	 * simulating a persisted entity returned by the repository.
	 */
	private FeedbackMessageEntity persistedEntity(Long id, String name, String email, String phone, String content) {
		FeedbackMessageEntity entity = FeedbackMessageEntity.create(name, email, phone, content);
		entity.setId(id);
		entity.setCreatedAt(Instant.parse("2026-04-06T10:00:00Z"));
		return entity;
	}

	// ─── Tests ───────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("submit")
	class Submit {

		@Test
		@DisplayName("saves entity and returns DTO with all fields")
		void shouldSaveAndReturnDTO() {
			FeedbackForm form = buildForm("Alice", "alice@example.com", "+1234567890", "Great app!");
			FeedbackMessageEntity saved = persistedEntity(1L, "Alice", "alice@example.com", "+1234567890", "Great app!");

			when(repository.save(any())).thenReturn(saved);

			FeedbackMessageDTO result = feedbackService.submit(form);

			assertThat(result.id()).isEqualTo(1L);
			assertThat(result.name()).isEqualTo("Alice");
			assertThat(result.email()).isEqualTo("alice@example.com");
			assertThat(result.phone()).isEqualTo("+1234567890");
			assertThat(result.content()).isEqualTo("Great app!");
			assertThat(result.createdAt()).isNotNull();
		}

		@Test
		@DisplayName("delegates notification to DiscordNotifier")
		void shouldCallDiscordNotifier() {
			FeedbackForm form = buildForm("Bob", "bob@example.com", null, "Loving the bingo rooms!");
			FeedbackMessageEntity saved = persistedEntity(2L, "Bob", "bob@example.com", null, "Loving the bingo rooms!");

			when(repository.save(any())).thenReturn(saved);

			feedbackService.submit(form);

			ArgumentCaptor<FeedbackMessageEntity> captor = ArgumentCaptor.forClass(FeedbackMessageEntity.class);
			verify(discordNotifier).notify(captor.capture());

			FeedbackMessageEntity notified = captor.getValue();
			assertThat(notified.getId()).isEqualTo(2L);
			assertThat(notified.getName()).isEqualTo("Bob");
		}

		@Test
		@DisplayName("handles null email gracefully")
		void shouldHandleNullEmail() {
			FeedbackForm form = buildForm("Carol", null, "+9876543210", "Anonymous feedback");
			FeedbackMessageEntity saved = persistedEntity(3L, "Carol", null, "+9876543210", "Anonymous feedback");

			when(repository.save(any())).thenReturn(saved);

			FeedbackMessageDTO result = feedbackService.submit(form);

			assertThat(result.email()).isNull();
			assertThat(result.phone()).isEqualTo("+9876543210");
			assertThat(result.name()).isEqualTo("Carol");
		}

		@Test
		@DisplayName("handles null phone gracefully")
		void shouldHandleNullPhone() {
			FeedbackForm form = buildForm("Dave", "dave@example.com", null, "Phone-less feedback");
			FeedbackMessageEntity saved = persistedEntity(4L, "Dave", "dave@example.com", null, "Phone-less feedback");

			when(repository.save(any())).thenReturn(saved);

			FeedbackMessageDTO result = feedbackService.submit(form);

			assertThat(result.phone()).isNull();
			assertThat(result.email()).isEqualTo("dave@example.com");
			assertThat(result.name()).isEqualTo("Dave");
		}
	}
}
