package com.yanajiki.application.bingoapp.database;

import com.yanajiki.application.bingoapp.game.DrawMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data-slice tests for {@link RoomRepository}.
 * <p>
 * Uses {@code @DataJpaTest} to load only the JPA slice (H2 in-memory, test profile),
 * and {@link TestEntityManager} to persist fixtures and manipulate timestamps
 * directly via native SQL to bypass the {@code @UpdateTimestamp} managed field.
 * </p>
 */
@DataJpaTest
@ActiveProfiles("test")
class RoomRepositoryTest {

	@Autowired
	private RoomRepository roomRepository;

	@Autowired
	private TestEntityManager entityManager;

	// ─── Helpers ─────────────────────────────────────────────────────────────────

	/**
	 * Persists a minimal {@link RoomEntity} and returns it.
	 *
	 * @param name unique name for the room
	 * @return the persisted entity with its generated ID
	 */
	private RoomEntity persistRoom(String name) {
		RoomEntity entity = RoomEntity.createEntityObject(name, "desc", DrawMode.MANUAL);
		return entityManager.persistAndFlush(entity);
	}

	/**
	 * Forces the {@code update_date_time} column of the given room to an arbitrary
	 * {@link Instant} using a native SQL UPDATE, bypassing {@code @UpdateTimestamp}.
	 *
	 * @param roomId the primary key of the room
	 * @param time   the timestamp to inject
	 */
	private void forceUpdateDateTime(Long roomId, Instant time) {
		entityManager.getEntityManager()
			.createNativeQuery("UPDATE room SET update_date_time = :time WHERE id = :id")
			.setParameter("time", time)
			.setParameter("id", roomId)
			.executeUpdate();
		entityManager.flush();
		entityManager.clear();
	}

	// ─── findByUpdateDateTimeBefore ───────────────────────────────────────────────

	@Nested
	@DisplayName("findByUpdateDateTimeBefore")
	class FindByUpdateDateTimeBefore {

		/**
		 * A room whose {@code updateDateTime} is 25 hours in the past must be returned
		 * when the cutoff is {@code now minus 24 hours}.
		 */
		@Test
		@DisplayName("returns expired rooms whose updateDateTime is before the cutoff")
		void returnsExpiredRooms() {
			// given
			RoomEntity expired = persistRoom("Expired Room");
			forceUpdateDateTime(expired.getId(), Instant.now().minus(25, ChronoUnit.HOURS));

			Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

			// when
			List<RoomEntity> result = roomRepository.findByUpdateDateTimeBefore(cutoff);

			// then
			assertThat(result)
				.hasSize(1)
				.extracting(RoomEntity::getName)
				.containsExactly("Expired Room");
		}

		/**
		 * A room whose {@code updateDateTime} is only 1 hour in the past must NOT be returned
		 * when the cutoff is {@code now minus 24 hours}.
		 */
		@Test
		@DisplayName("does not return active rooms whose updateDateTime is after the cutoff")
		void doesNotReturnActiveRooms() {
			// given
			RoomEntity active = persistRoom("Active Room");
			forceUpdateDateTime(active.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

			Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

			// when
			List<RoomEntity> result = roomRepository.findByUpdateDateTimeBefore(cutoff);

			// then
			assertThat(result).isEmpty();
		}

		/**
		 * When both expired and active rooms exist, only the expired ones are returned.
		 */
		@Test
		@DisplayName("mixed rooms — returns only expired ones")
		void mixedRooms_returnsOnlyExpired() {
			// given
			RoomEntity expired = persistRoom("Mixed Expired Room");
			RoomEntity active = persistRoom("Mixed Active Room");

			forceUpdateDateTime(expired.getId(), Instant.now().minus(25, ChronoUnit.HOURS));
			forceUpdateDateTime(active.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

			Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);

			// when
			List<RoomEntity> result = roomRepository.findByUpdateDateTimeBefore(cutoff);

			// then
			assertThat(result)
				.hasSize(1)
				.extracting(RoomEntity::getName)
				.containsExactly("Mixed Expired Room");
		}
	}
}
