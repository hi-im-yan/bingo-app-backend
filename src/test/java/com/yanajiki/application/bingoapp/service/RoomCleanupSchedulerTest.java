package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.game.DrawMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RoomCleanupScheduler}.
 * <p>
 * Uses Mockito to isolate the scheduler from the repository.
 * The {@code ttlHours} {@code @Value} field is injected via
 * {@link ReflectionTestUtils} to avoid loading a Spring context.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class RoomCleanupSchedulerTest {

	@Mock
	private RoomRepository roomRepository;

	@InjectMocks
	private RoomCleanupScheduler scheduler;

	@BeforeEach
	void injectTtl() {
		ReflectionTestUtils.setField(scheduler, "ttlHours", 24L);
	}

	// ─── cleanupExpiredRooms ──────────────────────────────────────────────────────

	@Nested
	@DisplayName("cleanupExpiredRooms")
	class CleanupExpiredRooms {

		/**
		 * When expired rooms are found, {@code deleteAll} must be called with those rooms.
		 */
		@Test
		@DisplayName("deletes expired rooms when they exist")
		void deletesExpiredRooms() {
			// given
			RoomEntity expiredRoom = RoomEntity.createEntityObject("Expired", "desc", DrawMode.MANUAL);
			when(roomRepository.findByUpdateDateTimeBefore(any(Instant.class)))
				.thenReturn(List.of(expiredRoom));

			// when
			scheduler.cleanupExpiredRooms();

			// then
			verify(roomRepository).deleteAll(List.of(expiredRoom));
		}

		/**
		 * When no expired rooms are found, {@code deleteAll} must never be called.
		 */
		@Test
		@DisplayName("does not call deleteAll when no expired rooms exist")
		void doesNotDeleteWhenNoneExpired() {
			// given
			when(roomRepository.findByUpdateDateTimeBefore(any(Instant.class)))
				.thenReturn(Collections.emptyList());

			// when
			scheduler.cleanupExpiredRooms();

			// then
			verify(roomRepository, never()).deleteAll(anyList());
		}

		/**
		 * The cutoff passed to the repository must be {@code now minus ttlHours},
		 * within a 5-second tolerance to account for test execution time.
		 */
		@Test
		@DisplayName("passes a cutoff of now minus ttlHours to the repository")
		void passesCorrectCutoffToRepository() {
			// given
			when(roomRepository.findByUpdateDateTimeBefore(any(Instant.class)))
				.thenReturn(Collections.emptyList());

			Instant before = Instant.now();

			// when
			scheduler.cleanupExpiredRooms();

			Instant after = Instant.now();

			// then
			ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
			verify(roomRepository).findByUpdateDateTimeBefore(captor.capture());

			Instant capturedCutoff = captor.getValue();
			Instant expectedMin = before.minus(24, ChronoUnit.HOURS);
			Instant expectedMax = after.minus(24, ChronoUnit.HOURS);

			assertThat(capturedCutoff)
				.isAfterOrEqualTo(expectedMin)
				.isBeforeOrEqualTo(expectedMax.plus(5, ChronoUnit.SECONDS));
		}
	}
}
