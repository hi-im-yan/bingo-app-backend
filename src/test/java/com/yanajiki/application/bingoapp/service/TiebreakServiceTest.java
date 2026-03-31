package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.response.TiebreakDTO;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.game.DrawMode;
import com.yanajiki.application.bingoapp.game.NumberLabelMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TiebreakService}.
 * <p>
 * Covers tiebreaker lifecycle: start, draw per slot, completion,
 * and all validation paths (room not found, wrong mode, bad player count,
 * duplicate tiebreaker, slot out of range, slot already drawn).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class TiebreakServiceTest {

	@Mock
	private RoomRepository roomRepository;

	@Mock
	private NumberLabelMapper numberLabelMapper;

	@InjectMocks
	private TiebreakService tiebreakService;

	// ─── Helpers ─────────────────────────────────────────────────────────────────

	/**
	 * Stubs the mapper to behave like standard bingo (range 1–75, label "X-{n}").
	 */
	private void stubStandardMapper() {
		when(numberLabelMapper.getMinNumber()).thenReturn(1);
		when(numberLabelMapper.getMaxNumber()).thenReturn(75);
		when(numberLabelMapper.toLabel(anyInt())).thenAnswer(inv -> "X-" + inv.getArgument(0));
	}

	/**
	 * Creates an AUTOMATIC room entity and stubs repository lookup for both session code + hash.
	 */
	private RoomEntity stubAutomaticRoom(String name) {
		RoomEntity entity = RoomEntity.createEntityObject(name, null, DrawMode.AUTOMATIC);
		when(roomRepository.findBySessionCodeAndCreatorHash(entity.getSessionCode(), entity.getCreatorHash()))
			.thenReturn(Optional.of(entity));
		return entity;
	}

	// ─── startTiebreak ───────────────────────────────────────────────────────────

	@Nested
	@DisplayName("startTiebreak")
	class StartTiebreak {

		@Test
		@DisplayName("success — creates tiebreaker with STARTED status")
		void success_createsTiebreaker() {
			// given
			RoomEntity entity = stubAutomaticRoom("Tiebreak Room");
			// No mapper stubs needed — DTO.from with empty draws doesn't call toLabel

			// when
			TiebreakDTO dto = tiebreakService.startTiebreak(
				entity.getSessionCode(), entity.getCreatorHash(), 3);

			// then
			assertThat(dto.status()).isEqualTo("STARTED");
			assertThat(dto.playerCount()).isEqualTo(3);
			assertThat(dto.draws()).isEmpty();
			assertThat(dto.winnerSlot()).isNull();
			assertThat(tiebreakService.hasActiveTiebreak(entity.getSessionCode())).isTrue();
		}

		@Test
		@DisplayName("room not found — throws RoomNotFoundException")
		void roomNotFound_throwsRoomNotFoundException() {
			when(roomRepository.findBySessionCodeAndCreatorHash("GHOST", "bad-hash"))
				.thenReturn(Optional.empty());

			assertThatThrownBy(() -> tiebreakService.startTiebreak("GHOST", "bad-hash", 3))
				.isInstanceOf(RoomNotFoundException.class);
		}

		@Test
		@DisplayName("wrong mode (MANUAL) — throws IllegalArgumentException")
		void manualRoom_throwsIllegalArgumentException() {
			RoomEntity entity = RoomEntity.createEntityObject("Manual Room", null);
			when(roomRepository.findBySessionCodeAndCreatorHash(entity.getSessionCode(), entity.getCreatorHash()))
				.thenReturn(Optional.of(entity));

			assertThatThrownBy(() -> tiebreakService.startTiebreak(
				entity.getSessionCode(), entity.getCreatorHash(), 3))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("automatic");
		}

		@Test
		@DisplayName("player count below 2 — throws IllegalArgumentException")
		void playerCountBelowMin_throwsIllegalArgumentException() {
			RoomEntity entity = stubAutomaticRoom("Low Count Room");

			assertThatThrownBy(() -> tiebreakService.startTiebreak(
				entity.getSessionCode(), entity.getCreatorHash(), 1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 2 and 6");
		}

		@Test
		@DisplayName("player count above 6 — throws IllegalArgumentException")
		void playerCountAboveMax_throwsIllegalArgumentException() {
			RoomEntity entity = stubAutomaticRoom("High Count Room");

			assertThatThrownBy(() -> tiebreakService.startTiebreak(
				entity.getSessionCode(), entity.getCreatorHash(), 7))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 2 and 6");
		}

		@Test
		@DisplayName("already active tiebreaker — throws IllegalStateException")
		void alreadyActive_throwsIllegalStateException() {
			RoomEntity entity = stubAutomaticRoom("Active Tiebreak Room");

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 2);

			assertThatThrownBy(() -> tiebreakService.startTiebreak(
				entity.getSessionCode(), entity.getCreatorHash(), 3))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already has an active tiebreaker");
		}

		@Test
		@DisplayName("can start new tiebreaker after clearing previous one")
		void canStartNewAfterClear() {
			RoomEntity entity = stubAutomaticRoom("Sequential Tiebreak Room");

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 2);
			tiebreakService.clearTiebreak(entity.getSessionCode());

			TiebreakDTO dto = tiebreakService.startTiebreak(
				entity.getSessionCode(), entity.getCreatorHash(), 4);

			assertThat(dto.status()).isEqualTo("STARTED");
			assertThat(dto.playerCount()).isEqualTo(4);
		}
	}

	// ─── drawForSlot ─────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("drawForSlot")
	class DrawForSlot {

		@Test
		@DisplayName("success — draws number and returns IN_PROGRESS")
		void success_drawsNumberAndReturnsInProgress() {
			RoomEntity entity = stubAutomaticRoom("Draw Slot Room");
			stubStandardMapper();

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 3);

			TiebreakDTO dto = tiebreakService.drawForSlot(
				entity.getSessionCode(), entity.getCreatorHash(), 1);

			assertThat(dto.status()).isEqualTo("IN_PROGRESS");
			assertThat(dto.draws()).hasSize(1);
			assertThat(dto.draws().get(0).slot()).isEqualTo(1);
			assertThat(dto.draws().get(0).number()).isBetween(1, 75);
			assertThat(dto.winnerSlot()).isNull();
		}

		@Test
		@DisplayName("all slots drawn — status is FINISHED with winnerSlot")
		void allSlotsDrawn_finishedWithWinner() {
			RoomEntity entity = stubAutomaticRoom("Finish Room");
			stubStandardMapper();

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 2);
			tiebreakService.drawForSlot(entity.getSessionCode(), entity.getCreatorHash(), 1);
			TiebreakDTO dto = tiebreakService.drawForSlot(
				entity.getSessionCode(), entity.getCreatorHash(), 2);

			assertThat(dto.status()).isEqualTo("FINISHED");
			assertThat(dto.draws()).hasSize(2);
			assertThat(dto.winnerSlot()).isNotNull();
			assertThat(dto.winnerSlot()).isBetween(1, 2);
		}

		@Test
		@DisplayName("room not found — throws RoomNotFoundException")
		void roomNotFound_throwsRoomNotFoundException() {
			when(roomRepository.findBySessionCodeAndCreatorHash("GHOST", "bad-hash"))
				.thenReturn(Optional.empty());

			assertThatThrownBy(() -> tiebreakService.drawForSlot("GHOST", "bad-hash", 1))
				.isInstanceOf(RoomNotFoundException.class);
		}

		@Test
		@DisplayName("no active tiebreaker — throws IllegalStateException")
		void noActiveTiebreak_throwsIllegalStateException() {
			RoomEntity entity = stubAutomaticRoom("No Tiebreak Room");

			assertThatThrownBy(() -> tiebreakService.drawForSlot(
				entity.getSessionCode(), entity.getCreatorHash(), 1))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No active tiebreaker");
		}

		@Test
		@DisplayName("slot below 1 — throws IllegalArgumentException")
		void slotBelowMin_throwsIllegalArgumentException() {
			RoomEntity entity = stubAutomaticRoom("Bad Slot Room");

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 3);

			assertThatThrownBy(() -> tiebreakService.drawForSlot(
				entity.getSessionCode(), entity.getCreatorHash(), 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 1 and");
		}

		@Test
		@DisplayName("slot above playerCount — throws IllegalArgumentException")
		void slotAboveMax_throwsIllegalArgumentException() {
			RoomEntity entity = stubAutomaticRoom("Over Slot Room");

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 3);

			assertThatThrownBy(() -> tiebreakService.drawForSlot(
				entity.getSessionCode(), entity.getCreatorHash(), 4))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 1 and 3");
		}

		@Test
		@DisplayName("slot already drawn — throws IllegalArgumentException")
		void slotAlreadyDrawn_throwsIllegalArgumentException() {
			RoomEntity entity = stubAutomaticRoom("Dup Slot Room");
			stubStandardMapper();

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 3);
			tiebreakService.drawForSlot(entity.getSessionCode(), entity.getCreatorHash(), 1);

			assertThatThrownBy(() -> tiebreakService.drawForSlot(
				entity.getSessionCode(), entity.getCreatorHash(), 1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already drawn");
		}

		@Test
		@DisplayName("tiebreaker numbers do not collide with room's drawn numbers")
		void tiebreakNumbersExcludeRoomDrawnNumbers() {
			RoomEntity entity = stubAutomaticRoom("Collision Room");
			// Draw 73 of 75 numbers into the room, leaving only 2 available
			for (int i = 1; i <= 73; i++) {
				entity.addDrawnNumber(i);
			}
			stubStandardMapper();

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 2);
			TiebreakDTO dto1 = tiebreakService.drawForSlot(entity.getSessionCode(), entity.getCreatorHash(), 1);
			TiebreakDTO dto2 = tiebreakService.drawForSlot(entity.getSessionCode(), entity.getCreatorHash(), 2);

			int num1 = dto1.draws().get(0).number();
			int num2 = dto2.draws().stream().filter(d -> d.slot() == 2).findFirst().orElseThrow().number();

			// Both must be from {74, 75} and not equal
			assertThat(num1).isBetween(74, 75);
			assertThat(num2).isBetween(74, 75);
			assertThat(num1).isNotEqualTo(num2);
		}
	}

	// ─── clearTiebreak ───────────────────────────────────────────────────────────

	@Nested
	@DisplayName("clearTiebreak")
	class ClearTiebreak {

		@Test
		@DisplayName("removes active tiebreaker state")
		void removesActiveState() {
			RoomEntity entity = stubAutomaticRoom("Clear Room");

			tiebreakService.startTiebreak(entity.getSessionCode(), entity.getCreatorHash(), 2);
			assertThat(tiebreakService.hasActiveTiebreak(entity.getSessionCode())).isTrue();

			tiebreakService.clearTiebreak(entity.getSessionCode());
			assertThat(tiebreakService.hasActiveTiebreak(entity.getSessionCode())).isFalse();
		}

		@Test
		@DisplayName("no-op when no active tiebreaker")
		void noOp_whenNoActiveTiebreaker() {
			tiebreakService.clearTiebreak("NONEXISTENT");
			assertThat(tiebreakService.hasActiveTiebreak("NONEXISTENT")).isFalse();
		}
	}
}
