package com.yanajiki.application.bingoapp.database;

import com.yanajiki.application.bingoapp.game.DrawMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RoomEntity}.
 * <p>
 * Covers the factory method {@link RoomEntity#createEntityObject} and the
 * append behaviour of {@link RoomEntity#addDrawnNumber}.
 * Range and duplicate validation has moved to the service layer (see RoomServiceTest);
 * the entity method is now a plain list append with no business-rule enforcement.
 * No Spring context is loaded — these are pure JUnit 5 tests.
 * </p>
 */
class RoomEntityTest {

	// ─── createEntityObject ───────────────────────────────────────────────────────

	@Nested
	@DisplayName("createEntityObject")
	class CreateEntityObject {

		/**
		 * The factory method must generate a 6-character session code composed
		 * exclusively of uppercase letters and digits (A–Z, 0–9).
		 */
		@Test
		@DisplayName("generates a 6-character alphanumeric session code")
		void generatesValidSessionCode() {
			// when
			RoomEntity entity = RoomEntity.createEntityObject("Test Room", "desc");

			// then
			String sessionCode = entity.getSessionCode();
			assertThat(sessionCode)
				.isNotBlank()
				.hasSize(6)
				.matches("[A-Z0-9]{6}");
		}

		/**
		 * The factory method must generate a UUID-format creator hash (36 characters,
		 * matching the standard UUID pattern with hyphens).
		 */
		@Test
		@DisplayName("generates a UUID creator hash")
		void generatesUuidCreatorHash() {
			// when
			RoomEntity entity = RoomEntity.createEntityObject("UUID Room", null);

			// then
			String creatorHash = entity.getCreatorHash();
			assertThat(creatorHash)
				.isNotBlank()
				.hasSize(36)
				.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
		}

		/**
		 * The factory method must set the name and description on the entity as-is.
		 */
		@Test
		@DisplayName("sets name and description from arguments")
		void setsNameAndDescription() {
			// when
			RoomEntity entity = RoomEntity.createEntityObject("My Bingo Room", "Fun room");

			// then
			assertThat(entity.getName()).isEqualTo("My Bingo Room");
			assertThat(entity.getDescription()).isEqualTo("Fun room");
		}

		/**
		 * The factory method must initialise the drawn-numbers list as empty, not null.
		 */
		@Test
		@DisplayName("initialises drawnNumbers as an empty list")
		void initialisesDrawnNumbersAsEmptyList() {
			// when
			RoomEntity entity = RoomEntity.createEntityObject("Empty Room", null);

			// then
			assertThat(entity.getDrawnNumbers()).isNotNull().isEmpty();
		}

		/**
		 * Two entities created by the same factory call must have different session codes
		 * (probabilistic check — collision chance is negligible for 6-char codes).
		 */
		@Test
		@DisplayName("generates unique session codes across calls")
		void generatesDifferentSessionCodesOnEachCall() {
			// when
			RoomEntity first = RoomEntity.createEntityObject("Room A", null);
			RoomEntity second = RoomEntity.createEntityObject("Room B", null);

			// then — collision probability is ~1/36^6 ≈ 1 in 2 billion
			assertThat(first.getSessionCode()).isNotEqualTo(second.getSessionCode());
		}

		/**
		 * When no draw mode is supplied, the factory method must default to {@link DrawMode#MANUAL}.
		 */
		@Test
		@DisplayName("defaults drawMode to MANUAL when not specified")
		void defaultsDrawModeToManual() {
			// when
			RoomEntity entity = RoomEntity.createEntityObject("Manual Room", "desc");

			// then
			assertThat(entity.getDrawMode()).isEqualTo(DrawMode.MANUAL);
		}

		/**
		 * When {@link DrawMode#AUTOMATIC} is explicitly provided, the factory method must
		 * store it on the entity.
		 */
		@Test
		@DisplayName("sets drawMode to AUTOMATIC when explicitly provided")
		void setsDrawModeToAutomatic() {
			// when
			RoomEntity entity = RoomEntity.createEntityObject("Auto Room", "desc", DrawMode.AUTOMATIC);

			// then
			assertThat(entity.getDrawMode()).isEqualTo(DrawMode.AUTOMATIC);
		}

		/**
		 * Passing {@link DrawMode#MANUAL} explicitly to the three-argument factory method
		 * must yield the same result as the two-argument default.
		 */
		@Test
		@DisplayName("sets drawMode to MANUAL when explicitly provided")
		void setsDrawModeToManualExplicitly() {
			// when
			RoomEntity entity = RoomEntity.createEntityObject("Explicit Manual Room", "desc", DrawMode.MANUAL);

			// then
			assertThat(entity.getDrawMode()).isEqualTo(DrawMode.MANUAL);
		}
	}

	// ─── addDrawnNumber ───────────────────────────────────────────────────────────

	@Nested
	@DisplayName("addDrawnNumber")
	class AddDrawnNumber {

		/**
		 * A number must be appended to the drawn-numbers list.
		 */
		@Test
		@DisplayName("appends number to drawnNumbers list")
		void validNumber_addedToList() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Room", null);

			// when
			entity.addDrawnNumber(37);

			// then
			assertThat(entity.getDrawnNumbers()).containsExactly(37);
		}

		/**
		 * Multiple numbers can be added in sequence; all are preserved in insertion order.
		 */
		@Test
		@DisplayName("multiple numbers — all present in insertion order")
		void multipleNumbers_allPresentInOrder() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Room", null);

			// when
			entity.addDrawnNumber(5);
			entity.addDrawnNumber(20);
			entity.addDrawnNumber(75);

			// then
			assertThat(entity.getDrawnNumbers()).containsExactly(5, 20, 75);
		}
	}
}
