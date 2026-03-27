package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.game.DrawMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Integration tests for {@link RoomService#correctLastNumber}.
 * <p>
 * Boots the full Spring context with H2, testing the correction flow end-to-end:
 * service logic, JPA persistence, and validation.
 * </p>
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class NumberCorrectionIntegrationTest {

	@Autowired
	private RoomService roomService;

	@Autowired
	private RoomRepository roomRepository;

	@AfterEach
	void tearDown() {
		roomRepository.deleteAll();
	}

	// ─── correctLastNumber ──────────────────────────────────────────────────────

	@Nested
	@DisplayName("correctLastNumber")
	class CorrectLastNumber {

		/**
		 * Full happy path: create room, draw two numbers, correct the last one.
		 * Verifies both the returned result and the persisted state in the database.
		 */
		@Test
		@DisplayName("success — replaces last number in drawn list")
		void success_replacesLastNumberInDrawnList() {
			// given
			RoomEntity saved = roomRepository.save(RoomEntity.createEntityObject("Correction Room", "desc"));
			String sessionCode = saved.getSessionCode();
			String creatorHash = saved.getCreatorHash();

			roomService.drawNumber(sessionCode, creatorHash, 5);
			roomService.drawNumber(sessionCode, creatorHash, 42);

			// when
			CorrectionResult result = roomService.correctLastNumber(sessionCode, creatorHash, 12);

			// then — result
			assertThat(result.roomDTO().drawnNumbers()).containsExactly(5, 12);
			assertThat(result.roomDTO().drawnLabels()).containsExactly("B-5", "B-12");
			assertThat(result.correctionDTO().oldNumber()).isEqualTo(42);
			assertThat(result.correctionDTO().oldLabel()).isEqualTo("N-42");
			assertThat(result.correctionDTO().newNumber()).isEqualTo(12);
			assertThat(result.correctionDTO().newLabel()).isEqualTo("B-12");
			assertThat(result.correctionDTO().message()).isEqualTo("GM changed N-42 to B-12");

			// then — database
			RoomEntity persisted = roomRepository.findBySessionCode(sessionCode).orElseThrow();
			assertThat(persisted.getDrawnNumbers()).containsExactly(5, 12);
		}

		/**
		 * Correcting when only one number has been drawn — the single element is replaced.
		 */
		@Test
		@DisplayName("success — correcting single drawn number")
		void success_correctingSingleDrawnNumber() {
			// given
			RoomEntity saved = roomRepository.save(RoomEntity.createEntityObject("Single Draw Room", null));
			String sessionCode = saved.getSessionCode();
			String creatorHash = saved.getCreatorHash();

			roomService.drawNumber(sessionCode, creatorHash, 75);

			// when
			CorrectionResult result = roomService.correctLastNumber(sessionCode, creatorHash, 1);

			// then
			assertThat(result.roomDTO().drawnNumbers()).containsExactly(1);
			assertThat(result.correctionDTO().message()).isEqualTo("GM changed O-75 to B-1");
		}

		/**
		 * Correcting when no numbers have been drawn must fail.
		 */
		@Test
		@DisplayName("error — no numbers drawn yet")
		void error_noNumbersDrawnYet() {
			// given
			RoomEntity saved = roomRepository.save(RoomEntity.createEntityObject("Empty Room", null));

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(
					saved.getSessionCode(), saved.getCreatorHash(), 10))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No numbers have been drawn");
		}

		/**
		 * AUTOMATIC rooms must reject correction requests.
		 */
		@Test
		@DisplayName("error — AUTOMATIC room rejects correction")
		void error_automaticRoomRejectsCorrection() {
			// given
			RoomEntity saved = roomRepository.save(
					RoomEntity.createEntityObject("Auto Room", null, DrawMode.AUTOMATIC));

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(
					saved.getSessionCode(), saved.getCreatorHash(), 10))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("manual draw mode");
		}

		/**
		 * Correcting to a number that is already in the drawn list (not the last) must fail.
		 */
		@Test
		@DisplayName("error — new number already in drawn list (duplicate)")
		void error_newNumberAlreadyDrawn() {
			// given
			RoomEntity saved = roomRepository.save(RoomEntity.createEntityObject("Dup Room", null));
			String sessionCode = saved.getSessionCode();
			String creatorHash = saved.getCreatorHash();

			roomService.drawNumber(sessionCode, creatorHash, 5);
			roomService.drawNumber(sessionCode, creatorHash, 42);

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(sessionCode, creatorHash, 5))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already been drawn");
		}

		/**
		 * Correcting to a number out of range must fail.
		 */
		@Test
		@DisplayName("error — new number out of range")
		void error_newNumberOutOfRange() {
			// given
			RoomEntity saved = roomRepository.save(RoomEntity.createEntityObject("Range Room", null));
			String sessionCode = saved.getSessionCode();
			String creatorHash = saved.getCreatorHash();

			roomService.drawNumber(sessionCode, creatorHash, 5);

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(sessionCode, creatorHash, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 1 and 75");
		}

		/**
		 * Correcting in a non-existent room must fail.
		 */
		@Test
		@DisplayName("error — room not found")
		void error_roomNotFound() {
			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber("INVALID", "bad-hash", 1))
				.isInstanceOf(RoomNotFoundException.class);
		}
	}
}
