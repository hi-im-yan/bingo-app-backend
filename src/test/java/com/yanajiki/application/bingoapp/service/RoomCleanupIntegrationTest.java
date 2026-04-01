package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.game.DrawMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for {@link RoomCleanupScheduler} against a full Spring context.
 * <p>
 * Boots the complete application (H2, test profile) to verify that
 * {@link RoomCleanupScheduler#cleanupExpiredRooms()} correctly removes stale rooms
 * without disturbing active ones. The scheduler is invoked directly rather than
 * waiting for the cron timer to fire.
 * </p>
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class RoomCleanupIntegrationTest {

	@Autowired
	private RoomCleanupScheduler roomCleanupScheduler;

	@Autowired
	private RoomRepository roomRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@AfterEach
	void tearDown() {
		roomRepository.deleteAll();
	}

	// ─── Helpers ─────────────────────────────────────────────────────────────────

	/**
	 * Persists a new room and returns it.
	 *
	 * @param name unique room name
	 * @return persisted entity
	 */
	private RoomEntity createRoom(String name) {
		RoomEntity entity = RoomEntity.createEntityObject(name, "desc", DrawMode.MANUAL);
		return roomRepository.save(entity);
	}

	/**
	 * Forces the {@code update_date_time} of a room to {@code hoursAgo} hours in the past
	 * using a native SQL UPDATE, bypassing {@code @UpdateTimestamp}.
	 * Must be called within a transaction.
	 *
	 * @param roomId   the primary key of the room to age
	 * @param hoursAgo how many hours in the past to set the timestamp
	 */
	private void ageRoom(Long roomId, int hoursAgo) {
		entityManager.createNativeQuery("UPDATE room SET update_date_time = :time WHERE id = :id")
			.setParameter("time", Instant.now().minus(hoursAgo, ChronoUnit.HOURS))
			.setParameter("id", roomId)
			.executeUpdate();
		entityManager.flush();
		entityManager.clear();
	}

	// ─── cleanupExpiredRooms ──────────────────────────────────────────────────────

	@Nested
	@DisplayName("cleanupExpiredRooms")
	class CleanupExpiredRooms {

		/**
		 * A room whose {@code updateDateTime} is 25 hours old must be deleted by the cleanup.
		 */
		@Test
		@Transactional
		@DisplayName("deletes a room that is older than TTL")
		void deletesExpiredRoom() {
			// given
			RoomEntity room = createRoom("Expired Integration Room");
			ageRoom(room.getId(), 25);

			// when
			roomCleanupScheduler.cleanupExpiredRooms();

			// then
			assertThat(roomRepository.findById(room.getId())).isEmpty();
		}

		/**
		 * A room whose {@code updateDateTime} is only 1 hour old must survive the cleanup.
		 */
		@Test
		@Transactional
		@DisplayName("keeps a room that is within TTL")
		void keepsActiveRoom() {
			// given
			RoomEntity room = createRoom("Active Integration Room");
			ageRoom(room.getId(), 1);

			// when
			roomCleanupScheduler.cleanupExpiredRooms();

			// then
			assertThat(roomRepository.findById(room.getId())).isPresent();
		}

		/**
		 * When both expired and active rooms exist, only the expired room is deleted.
		 */
		@Test
		@Transactional
		@DisplayName("mixed rooms — only expired room is deleted, active room survives")
		void mixedRooms_onlyExpiredDeleted() {
			// given
			RoomEntity expired = createRoom("Mixed Expired Integration");
			RoomEntity active = createRoom("Mixed Active Integration");

			ageRoom(expired.getId(), 25);
			ageRoom(active.getId(), 1);

			// when
			roomCleanupScheduler.cleanupExpiredRooms();

			// then
			assertThat(roomRepository.findById(expired.getId())).isEmpty();
			assertThat(roomRepository.findById(active.getId())).isPresent();
		}

		/**
		 * A room that had a draw recently (1 hour ago) must survive even if it was
		 * created long ago — the TTL is based on {@code updateDateTime}, not {@code createDateTime}.
		 */
		@Test
		@Transactional
		@DisplayName("room with recent activity survives cleanup")
		void roomWithRecentDrawSurvives() {
			// given — room was "created" long ago but has recent activity (1 hour)
			RoomEntity room = createRoom("Recent Draw Room");
			ageRoom(room.getId(), 1);

			// when
			roomCleanupScheduler.cleanupExpiredRooms();

			// then
			assertThat(roomRepository.findById(room.getId())).isPresent();
		}
	}
}
