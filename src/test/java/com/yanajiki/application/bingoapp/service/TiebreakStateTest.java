package com.yanajiki.application.bingoapp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TiebreakState}.
 * <p>
 * Verifies state transitions, draw recording, slot validation,
 * winner resolution, and completion detection.
 * </p>
 */
class TiebreakStateTest {

	// ─── Construction ────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("construction")
	class Construction {

		@Test
		@DisplayName("new state has correct player count and no draws")
		void newState_hasCorrectPlayerCountAndNoDraws() {
			TiebreakState state = new TiebreakState(3);

			assertThat(state.getPlayerCount()).isEqualTo(3);
			assertThat(state.getDraws()).isEmpty();
		}
	}

	// ─── getStatus ───────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("getStatus")
	class GetStatus {

		@Test
		@DisplayName("STARTED when no draws yet")
		void started_whenNoDraws() {
			TiebreakState state = new TiebreakState(2);

			assertThat(state.getStatus()).isEqualTo("STARTED");
		}

		@Test
		@DisplayName("IN_PROGRESS after partial draws")
		void inProgress_afterPartialDraws() {
			TiebreakState state = new TiebreakState(3);
			state.addDraw(1, 42);

			assertThat(state.getStatus()).isEqualTo("IN_PROGRESS");
		}

		@Test
		@DisplayName("FINISHED when all slots drawn")
		void finished_whenAllSlotsDrawn() {
			TiebreakState state = new TiebreakState(2);
			state.addDraw(1, 42);
			state.addDraw(2, 55);

			assertThat(state.getStatus()).isEqualTo("FINISHED");
		}
	}

	// ─── addDraw ─────────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("addDraw")
	class AddDraw {

		@Test
		@DisplayName("records slot-number mapping")
		void recordsSlotNumberMapping() {
			TiebreakState state = new TiebreakState(3);
			state.addDraw(1, 42);

			assertThat(state.getDraws()).containsEntry(1, 42);
		}

		@Test
		@DisplayName("multiple draws are preserved in insertion order")
		void multipleDrawsPreservedInOrder() {
			TiebreakState state = new TiebreakState(3);
			state.addDraw(1, 42);
			state.addDraw(2, 17);
			state.addDraw(3, 65);

			assertThat(state.getDraws()).containsKeys(1, 2, 3);
			assertThat(state.getDraws().values()).containsExactly(42, 17, 65);
		}
	}

	// ─── isSlotDrawn ─────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("isSlotDrawn")
	class IsSlotDrawn {

		@Test
		@DisplayName("false for undrawn slot")
		void falseForUndrawnSlot() {
			TiebreakState state = new TiebreakState(2);

			assertThat(state.isSlotDrawn(1)).isFalse();
		}

		@Test
		@DisplayName("true for drawn slot")
		void trueForDrawnSlot() {
			TiebreakState state = new TiebreakState(2);
			state.addDraw(1, 42);

			assertThat(state.isSlotDrawn(1)).isTrue();
		}
	}

	// ─── isComplete ──────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("isComplete")
	class IsComplete {

		@Test
		@DisplayName("false when draws < playerCount")
		void falseWhenIncomplete() {
			TiebreakState state = new TiebreakState(3);
			state.addDraw(1, 42);

			assertThat(state.isComplete()).isFalse();
		}

		@Test
		@DisplayName("true when draws == playerCount")
		void trueWhenComplete() {
			TiebreakState state = new TiebreakState(2);
			state.addDraw(1, 42);
			state.addDraw(2, 55);

			assertThat(state.isComplete()).isTrue();
		}
	}

	// ─── getWinnerSlot ───────────────────────────────────────────────────────────

	@Nested
	@DisplayName("getWinnerSlot")
	class GetWinnerSlot {

		@Test
		@DisplayName("returns slot with highest number")
		void returnsSlotWithHighestNumber() {
			TiebreakState state = new TiebreakState(3);
			state.addDraw(1, 10);
			state.addDraw(2, 65);
			state.addDraw(3, 33);

			assertThat(state.getWinnerSlot()).isEqualTo(2);
		}

		@Test
		@DisplayName("returns first slot when tie on highest number")
		void returnsFirstSlotOnTie() {
			TiebreakState state = new TiebreakState(2);
			state.addDraw(1, 50);
			state.addDraw(2, 50);

			// LinkedHashMap iteration order — first inserted wins on tie
			assertThat(state.getWinnerSlot()).isEqualTo(1);
		}

		@Test
		@DisplayName("throws when no draws exist")
		void throwsWhenNoDraws() {
			TiebreakState state = new TiebreakState(2);

			assertThatThrownBy(state::getWinnerSlot)
				.isInstanceOf(java.util.NoSuchElementException.class);
		}
	}
}
