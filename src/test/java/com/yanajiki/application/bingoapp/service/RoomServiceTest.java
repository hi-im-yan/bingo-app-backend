package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.form.CreateRoomForm;
import com.yanajiki.application.bingoapp.api.response.PlayerDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.database.PlayerEntity;
import com.yanajiki.application.bingoapp.database.PlayerRepository;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.ConflictException;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.game.DrawMode;
import com.yanajiki.application.bingoapp.game.NumberLabelMapper;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RoomService}.
 * <p>
 * Uses Mockito to isolate the service from the repository and the number-label mapper,
 * verifying all business-logic branches: room creation, lookup (creator vs player view),
 * deletion, and number drawing (valid, not found, out-of-range, duplicate).
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

	@Mock
	private RoomRepository repository;

	/**
	 * Mocked mapper — returns a predictable label format and the standard 1–75 range,
	 * so service tests are independent of the real {@code StandardBingoMapper} implementation.
	 */
	@Mock
	private NumberLabelMapper numberLabelMapper;

	/** Mocked player repository for player join and list operations. */
	@Mock
	private PlayerRepository playerRepository;

	@InjectMocks
	private RoomService roomService;

	// ─── Helpers ─────────────────────────────────────────────────────────────────

	/**
	 * Builds a {@link CreateRoomForm} with the given name and description,
	 * leaving {@code drawMode} null (service defaults to MANUAL).
	 */
	private CreateRoomForm buildForm(String name, String description) {
		CreateRoomForm form = new CreateRoomForm();
		form.setName(name);
		form.setDescription(description);
		return form;
	}

	/**
	 * Builds a {@link CreateRoomForm} with the given name, description, and explicit draw mode.
	 */
	private CreateRoomForm buildForm(String name, String description, DrawMode drawMode) {
		CreateRoomForm form = buildForm(name, description);
		form.setDrawMode(drawMode);
		return form;
	}

	/**
	 * Stubs the mapper to behave like standard bingo (range 1–75, label "X-{n}").
	 */
	private void stubStandardMapper() {
		when(numberLabelMapper.getMinNumber()).thenReturn(1);
		when(numberLabelMapper.getMaxNumber()).thenReturn(75);
		when(numberLabelMapper.toLabel(anyInt())).thenAnswer(inv -> "X-" + inv.getArgument(0));
	}

	// ─── createRoom ──────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("createRoom")
	class CreateRoom {

		/**
		 * A valid creation form with a unique name should persist the entity and return a creator-view DTO
		 * that includes the {@code creatorHash}, both drawn fields (empty), and {@code drawMode} defaulting to MANUAL.
		 */
		@Test
		@DisplayName("success — saves entity and returns creator DTO with creatorHash and MANUAL drawMode by default")
		void success_savesEntityAndReturnsCreatorDto() {
			// given
			CreateRoomForm form = buildForm("Friday Night Bingo", "Weekly game");
			RoomEntity saved = RoomEntity.createEntityObject("Friday Night Bingo", "Weekly game");

			when(repository.findByName("Friday Night Bingo")).thenReturn(Optional.empty());
			when(repository.save(any(RoomEntity.class))).thenReturn(saved);
			// no toLabel stub needed — saved entity has no drawn numbers, labels list will be empty

			// when
			RoomDTO result = roomService.createRoom(form);

			// then
			assertThat(result.name()).isEqualTo("Friday Night Bingo");
			assertThat(result.description()).isEqualTo("Weekly game");
			assertThat(result.sessionCode()).isNotBlank();
			assertThat(result.creatorHash()).isNotBlank();
			assertThat(result.drawnNumbers()).isEmpty();
			assertThat(result.drawnLabels()).isEmpty();
			assertThat(result.drawMode()).isEqualTo(DrawMode.MANUAL);

			verify(repository).findByName("Friday Night Bingo");
			verify(repository).save(any(RoomEntity.class));
		}

		/**
		 * When {@code drawMode} is explicitly set to AUTOMATIC in the form, the returned DTO
		 * must reflect AUTOMATIC — not the default MANUAL.
		 */
		@Test
		@DisplayName("success — explicit AUTOMATIC drawMode is preserved in the response")
		void success_automaticDrawMode_isPreservedInResponse() {
			// given
			CreateRoomForm form = buildForm("Auto Bingo Night", "Automatic draws", DrawMode.AUTOMATIC);
			RoomEntity saved = RoomEntity.createEntityObject("Auto Bingo Night", "Automatic draws", DrawMode.AUTOMATIC);

			when(repository.findByName("Auto Bingo Night")).thenReturn(Optional.empty());
			when(repository.save(any(RoomEntity.class))).thenReturn(saved);

			// when
			RoomDTO result = roomService.createRoom(form);

			// then
			assertThat(result.drawMode()).isEqualTo(DrawMode.AUTOMATIC);
			verify(repository).save(any(RoomEntity.class));
		}

		/**
		 * Creating a room whose name is already taken must throw {@link ConflictException}
		 * and never attempt a save.
		 */
		@Test
		@DisplayName("conflict — name already exists, throws ConflictException")
		void conflict_nameAlreadyExists_throwsConflictException() {
			// given
			CreateRoomForm form = buildForm("Existing Room", "desc");
			RoomEntity existing = RoomEntity.createEntityObject("Existing Room", null);

			when(repository.findByName("Existing Room")).thenReturn(Optional.of(existing));

			// when / then
			assertThatThrownBy(() -> roomService.createRoom(form))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("Room already exists");

			verify(repository, never()).save(any());
		}
	}

	// ─── findRoomBySessionCode ───────────────────────────────────────────────────

	@Nested
	@DisplayName("findRoomBySessionCode")
	class FindRoomBySessionCode {

		/**
		 * A valid creator hash triggers a lookup by both session code and hash,
		 * returning the creator view with {@code creatorHash} populated.
		 */
		@Test
		@DisplayName("with valid creatorHash — returns creator view including hash")
		void withCreatorHash_returnsCreatorView() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Creator Room", null);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			// no toLabel stub — entity has no drawn numbers, so toLabel is never invoked

			// when
			RoomDTO result = roomService.findRoomBySessionCode(sessionCode, creatorHash);

			// then
			assertThat(result.creatorHash()).isEqualTo(creatorHash);
			verify(repository).findBySessionCodeAndCreatorHash(sessionCode, creatorHash);
			verify(repository, never()).findBySessionCode(any());
		}

		/**
		 * A null creator hash triggers the player-view lookup (session code only),
		 * returning a DTO with {@code creatorHash} set to {@code null}.
		 */
		@Test
		@DisplayName("without creatorHash (null) — returns player view with null hash")
		void withoutCreatorHash_null_returnsPlayerView() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Player Room", null);
			String sessionCode = entity.getSessionCode();

			when(repository.findBySessionCode(sessionCode)).thenReturn(Optional.of(entity));
			// no toLabel stub — entity has no drawn numbers, so toLabel is never invoked

			// when
			RoomDTO result = roomService.findRoomBySessionCode(sessionCode, null);

			// then
			assertThat(result.creatorHash()).isNull();
			verify(repository).findBySessionCode(sessionCode);
			verify(repository, never()).findBySessionCodeAndCreatorHash(anyString(), anyString());
		}

		/**
		 * A blank (whitespace-only) creator hash is treated as absent — falls back to player view.
		 */
		@Test
		@DisplayName("without creatorHash (blank string) — returns player view with null hash")
		void withBlankCreatorHash_returnsPlayerView() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Blank Hash Room", null);
			String sessionCode = entity.getSessionCode();

			when(repository.findBySessionCode(sessionCode)).thenReturn(Optional.of(entity));
			// no toLabel stub — entity has no drawn numbers, so toLabel is never invoked

			// when
			RoomDTO result = roomService.findRoomBySessionCode(sessionCode, "   ");

			// then
			assertThat(result.creatorHash()).isNull();
			verify(repository).findBySessionCode(sessionCode);
			verify(repository, never()).findBySessionCodeAndCreatorHash(anyString(), anyString());
		}

		/**
		 * A session code that does not exist in the repository must throw {@link RoomNotFoundException}.
		 */
		@Test
		@DisplayName("room not found — throws RoomNotFoundException")
		void roomNotFound_throwsRoomNotFoundException() {
			// given
			when(repository.findBySessionCode("GHOST1")).thenReturn(Optional.empty());

			// when / then
			assertThatThrownBy(() -> roomService.findRoomBySessionCode("GHOST1", null))
				.isInstanceOf(RoomNotFoundException.class);
		}
	}

	// ─── deleteRoom ──────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("deleteRoom")
	class DeleteRoom {

		/**
		 * Deleting with matching session code and creator hash must invoke {@code repository.delete}.
		 */
		@Test
		@DisplayName("success — finds entity and deletes it")
		void success_findsAndDeletes() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Room To Delete", null);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));

			// when
			roomService.deleteRoom(sessionCode, creatorHash);

			// then
			verify(repository).delete(entity);
		}

		/**
		 * Deleting a room that cannot be found (wrong code or wrong hash)
		 * must throw {@link RoomNotFoundException} and never call delete.
		 */
		@Test
		@DisplayName("room not found — throws RoomNotFoundException")
		void roomNotFound_throwsRoomNotFoundException() {
			// given
			when(repository.findBySessionCodeAndCreatorHash("GHOST2", "wrong-hash"))
				.thenReturn(Optional.empty());

			// when / then
			assertThatThrownBy(() -> roomService.deleteRoom("GHOST2", "wrong-hash"))
				.isInstanceOf(RoomNotFoundException.class);

			verify(repository, never()).delete(any());
		}
	}

	// ─── drawNumber ──────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("drawNumber")
	class DrawNumber {

		/**
		 * Drawing a valid number must persist the entity and return the player view (no creatorHash)
		 * with both {@code drawnNumbers} and {@code drawnLabels} populated.
		 */
		@Test
		@DisplayName("success — number added, entity saved, player DTO returned with labels")
		void success_addsNumberAndReturnsPlayerDto() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Draw Room", null);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(repository.save(entity)).thenReturn(entity);
			stubStandardMapper();

			// when
			RoomDTO result = roomService.drawNumber(sessionCode, creatorHash, 42);

			// then
			assertThat(result.drawnNumbers()).containsExactly(42);
			assertThat(result.drawnLabels()).containsExactly("X-42");
			assertThat(result.creatorHash()).isNull();
			verify(repository).save(entity);
		}

		/**
		 * Drawing a number in a non-existent room must throw {@link RoomNotFoundException}
		 * and never attempt a save.
		 */
		@Test
		@DisplayName("room not found — throws RoomNotFoundException")
		void roomNotFound_throwsRoomNotFoundException() {
			// given
			when(repository.findBySessionCodeAndCreatorHash("GHOST3", "no-hash"))
				.thenReturn(Optional.empty());

			// when / then
			assertThatThrownBy(() -> roomService.drawNumber("GHOST3", "no-hash", 10))
				.isInstanceOf(RoomNotFoundException.class);

			verify(repository, never()).save(any());
		}

		/**
		 * Drawing 0 (below the 1–75 bingo range) must throw {@link IllegalArgumentException}.
		 */
		@Test
		@DisplayName("invalid number (0) — throws IllegalArgumentException")
		void invalidNumber_zero_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Range Room", null);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(numberLabelMapper.getMinNumber()).thenReturn(1);
			when(numberLabelMapper.getMaxNumber()).thenReturn(75);

			// when / then
			assertThatThrownBy(() -> roomService.drawNumber(sessionCode, creatorHash, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 1 and 75");

			verify(repository, never()).save(any());
		}

		/**
		 * Drawing 76 (above the 1–75 bingo range) must throw {@link IllegalArgumentException}.
		 */
		@Test
		@DisplayName("invalid number (76) — throws IllegalArgumentException")
		void invalidNumber_aboveMax_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Range Room 2", null);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(numberLabelMapper.getMinNumber()).thenReturn(1);
			when(numberLabelMapper.getMaxNumber()).thenReturn(75);

			// when / then
			assertThatThrownBy(() -> roomService.drawNumber(sessionCode, creatorHash, 76))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 1 and 75");

			verify(repository, never()).save(any());
		}

		/**
		 * Drawing a number that has already been drawn must throw {@link IllegalArgumentException}.
		 */
		@Test
		@DisplayName("duplicate number — throws IllegalArgumentException")
		void duplicateNumber_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Duplicate Draw Room", null);
			entity.addDrawnNumber(7);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(numberLabelMapper.getMinNumber()).thenReturn(1);
			when(numberLabelMapper.getMaxNumber()).thenReturn(75);

			// when / then
			assertThatThrownBy(() -> roomService.drawNumber(sessionCode, creatorHash, 7))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already been drawn");

			verify(repository, never()).save(any());
		}

		/**
		 * Calling {@code drawNumber()} on an AUTOMATIC room must throw {@link IllegalArgumentException}
		 * with a message indicating automatic draw mode is in use.
		 */
		@Test
		@DisplayName("wrong mode (AUTOMATIC room) — throws IllegalArgumentException")
		void automaticRoom_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Auto Room", null, DrawMode.AUTOMATIC);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));

			// when / then
			assertThatThrownBy(() -> roomService.drawNumber(sessionCode, creatorHash, 10))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("This room uses automatic draw mode");

			verify(repository, never()).save(any());
		}
	}

	// ─── correctLastNumber ──────────────────────────────────────────────────────

	@Nested
	@DisplayName("correctLastNumber")
	class CorrectLastNumber {

		/**
		 * Correcting the last drawn number should replace it in the list, persist,
		 * and return a {@link CorrectionResult} with both the updated room and correction notification.
		 */
		@Test
		@DisplayName("success — replaces last number and returns correction result")
		void success_replacesLastNumberAndReturnsCorrectionResult() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Correction Room", null);
			entity.addDrawnNumber(5);
			entity.addDrawnNumber(42);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(repository.save(entity)).thenReturn(entity);
			stubStandardMapper();

			// when
			CorrectionResult result = roomService.correctLastNumber(sessionCode, creatorHash, 12);

			// then
			assertThat(result.roomDTO().drawnNumbers()).containsExactly(5, 12);
			assertThat(result.roomDTO().drawnLabels()).containsExactly("X-5", "X-12");
			assertThat(result.roomDTO().creatorHash()).isNull();
			assertThat(result.correctionDTO().oldNumber()).isEqualTo(42);
			assertThat(result.correctionDTO().oldLabel()).isEqualTo("X-42");
			assertThat(result.correctionDTO().newNumber()).isEqualTo(12);
			assertThat(result.correctionDTO().newLabel()).isEqualTo("X-12");
			assertThat(result.correctionDTO().message()).isEqualTo("GM changed X-42 to X-12");
			verify(repository).save(entity);
		}

		/**
		 * Correcting in a room that does not exist must throw {@link RoomNotFoundException}.
		 */
		@Test
		@DisplayName("room not found — throws RoomNotFoundException")
		void roomNotFound_throwsRoomNotFoundException() {
			// given
			when(repository.findBySessionCodeAndCreatorHash("GHOST5", "bad-hash"))
				.thenReturn(Optional.empty());

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber("GHOST5", "bad-hash", 10))
				.isInstanceOf(RoomNotFoundException.class);

			verify(repository, never()).save(any());
		}

		/**
		 * Correcting in an AUTOMATIC room must throw {@link IllegalArgumentException}.
		 */
		@Test
		@DisplayName("wrong mode (AUTOMATIC) — throws IllegalArgumentException")
		void automaticRoom_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Auto Correct Room", null, DrawMode.AUTOMATIC);
			entity.addDrawnNumber(5);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(sessionCode, creatorHash, 10))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("manual draw mode");

			verify(repository, never()).save(any());
		}

		/**
		 * Correcting when no numbers have been drawn must throw {@link IllegalStateException}.
		 */
		@Test
		@DisplayName("no numbers drawn — throws IllegalStateException")
		void noNumbersDrawn_throwsIllegalStateException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Empty Draw Room", null);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(sessionCode, creatorHash, 10))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No numbers have been drawn");

			verify(repository, never()).save(any());
		}

		/**
		 * Correcting to a number below the valid range (0) must throw {@link IllegalArgumentException}.
		 */
		@Test
		@DisplayName("new number out of range (0) — throws IllegalArgumentException")
		void newNumberBelowRange_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Range Correct Room", null);
			entity.addDrawnNumber(5);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(numberLabelMapper.getMinNumber()).thenReturn(1);
			when(numberLabelMapper.getMaxNumber()).thenReturn(75);

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(sessionCode, creatorHash, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 1 and 75");

			verify(repository, never()).save(any());
		}

		/**
		 * Correcting to a number above the valid range (76) must throw {@link IllegalArgumentException}.
		 */
		@Test
		@DisplayName("new number out of range (76) — throws IllegalArgumentException")
		void newNumberAboveRange_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Range Correct Room 2", null);
			entity.addDrawnNumber(5);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(numberLabelMapper.getMinNumber()).thenReturn(1);
			when(numberLabelMapper.getMaxNumber()).thenReturn(75);

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(sessionCode, creatorHash, 76))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 1 and 75");

			verify(repository, never()).save(any());
		}

		/**
		 * Correcting to a number already in the drawn list (not the last one) must throw {@link IllegalArgumentException}.
		 */
		@Test
		@DisplayName("new number already drawn (duplicate) — throws IllegalArgumentException")
		void duplicateNewNumber_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Duplicate Correct Room", null);
			entity.addDrawnNumber(5);
			entity.addDrawnNumber(42);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(numberLabelMapper.getMinNumber()).thenReturn(1);
			when(numberLabelMapper.getMaxNumber()).thenReturn(75);

			// when / then
			assertThatThrownBy(() -> roomService.correctLastNumber(sessionCode, creatorHash, 5))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("already been drawn");

			verify(repository, never()).save(any());
		}
	}

	// ─── drawRandomNumber ────────────────────────────────────────────────────────

	@Nested
	@DisplayName("drawRandomNumber")
	class DrawRandomNumber {

		/**
		 * Drawing a random number on an AUTOMATIC room must pick one of the remaining numbers,
		 * persist the entity, and return the player view with the new number included.
		 */
		@Test
		@DisplayName("success — picks a number from remaining pool, saves entity, returns player DTO")
		void success_picksRemainingNumberAndReturnsPlayerDto() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Auto Draw Room", null, DrawMode.AUTOMATIC);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(repository.save(entity)).thenReturn(entity);
			stubStandardMapper();

			// when
			RoomDTO result = roomService.drawRandomNumber(sessionCode, creatorHash);

			// then
			assertThat(result.drawnNumbers()).hasSize(1);
			int drawnNumber = result.drawnNumbers().get(0);
			assertThat(drawnNumber).isBetween(1, 75);
			assertThat(result.drawnLabels()).containsExactly("X-" + drawnNumber);
			assertThat(result.creatorHash()).isNull();
			verify(repository).save(entity);
		}

		/**
		 * When all 75 numbers have already been drawn, {@code drawRandomNumber()} must throw
		 * {@link IllegalStateException}.
		 */
		@Test
		@DisplayName("all numbers drawn — throws IllegalStateException")
		void allNumbersDrawn_throwsIllegalStateException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Full Room", null, DrawMode.AUTOMATIC);
			for (int i = 1; i <= 75; i++) {
				entity.addDrawnNumber(i);
			}
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));
			when(numberLabelMapper.getMinNumber()).thenReturn(1);
			when(numberLabelMapper.getMaxNumber()).thenReturn(75);

			// when / then
			assertThatThrownBy(() -> roomService.drawRandomNumber(sessionCode, creatorHash))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("All numbers have been drawn");

			verify(repository, never()).save(any());
		}

		/**
		 * Calling {@code drawRandomNumber()} on a MANUAL room must throw {@link IllegalArgumentException}
		 * with a message indicating manual draw mode is in use.
		 */
		@Test
		@DisplayName("wrong mode (MANUAL room) — throws IllegalArgumentException")
		void manualRoom_throwsIllegalArgumentException() {
			// given
			RoomEntity entity = RoomEntity.createEntityObject("Manual Room", null);
			String sessionCode = entity.getSessionCode();
			String creatorHash = entity.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(entity));

			// when / then
			assertThatThrownBy(() -> roomService.drawRandomNumber(sessionCode, creatorHash))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("This room uses manual draw mode");

			verify(repository, never()).save(any());
		}

		/**
		 * Calling {@code drawRandomNumber()} with an unknown session code or wrong creator hash
		 * must throw {@link RoomNotFoundException}.
		 */
		@Test
		@DisplayName("room not found — throws RoomNotFoundException")
		void roomNotFound_throwsRoomNotFoundException() {
			// given
			when(repository.findBySessionCodeAndCreatorHash("GHOST4", "bad-hash"))
				.thenReturn(Optional.empty());

			// when / then
			assertThatThrownBy(() -> roomService.drawRandomNumber("GHOST4", "bad-hash"))
				.isInstanceOf(RoomNotFoundException.class);

			verify(repository, never()).save(any());
		}
	}

	// ─── joinRoom ────────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Join Room")
	class JoinRoom {

		/**
		 * A valid session code and unique player name should persist the player
		 * and return a {@link PlayerDTO} with the player's name.
		 */
		@Test
		@DisplayName("should register player and return PlayerDTO")
		void success_registersPlayerAndReturnsPlayerDto() {
			// given
			RoomEntity room = RoomEntity.createEntityObject("Test Room", null);
			String sessionCode = room.getSessionCode();

			when(repository.findBySessionCode(sessionCode)).thenReturn(Optional.of(room));
			when(playerRepository.existsByNameAndRoomEntity("Alice", room)).thenReturn(false);
			when(playerRepository.save(any(PlayerEntity.class))).thenAnswer(inv -> inv.getArgument(0));

			// when
			PlayerDTO result = roomService.joinRoom(sessionCode, "Alice");

			// then
			assertThat(result.name()).isEqualTo("Alice");
			verify(playerRepository).save(any(PlayerEntity.class));
		}

		/**
		 * Using a session code that does not match any room must throw {@link RoomNotFoundException}
		 * and never attempt to save a player.
		 */
		@Test
		@DisplayName("should throw RoomNotFoundException when session code is invalid")
		void invalidSessionCode_throwsRoomNotFoundException() {
			// given
			when(repository.findBySessionCode("GHOST")).thenReturn(Optional.empty());

			// when / then
			assertThatThrownBy(() -> roomService.joinRoom("GHOST", "Alice"))
				.isInstanceOf(RoomNotFoundException.class);

			verify(playerRepository, never()).save(any());
		}

		/**
		 * When a player with the same name is already registered in the room,
		 * a {@link ConflictException} must be thrown and no save should occur.
		 */
		@Test
		@DisplayName("should throw ConflictException when player name is duplicate in room")
		void duplicatePlayerName_throwsConflictException() {
			// given
			RoomEntity room = RoomEntity.createEntityObject("Conflict Room", null);
			String sessionCode = room.getSessionCode();

			when(repository.findBySessionCode(sessionCode)).thenReturn(Optional.of(room));
			when(playerRepository.existsByNameAndRoomEntity("Alice", room)).thenReturn(true);

			// when / then
			assertThatThrownBy(() -> roomService.joinRoom(sessionCode, "Alice"))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("already taken");

			verify(playerRepository, never()).save(any());
		}
	}

	// ─── getPlayersByRoom ────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Get Players By Room")
	class GetPlayersByRoom {

		/**
		 * A valid session code and creator hash should return the full list of players
		 * as {@link PlayerDTO} instances.
		 */
		@Test
		@DisplayName("should return list of PlayerDTOs for valid creator")
		void success_returnsPlayerDtoList() {
			// given
			RoomEntity room = RoomEntity.createEntityObject("Player List Room", null);
			String sessionCode = room.getSessionCode();
			String creatorHash = room.getCreatorHash();

			PlayerEntity alice = PlayerEntity.create("Alice", room);
			PlayerEntity bob = PlayerEntity.create("Bob", room);

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(room));
			when(playerRepository.findByRoomEntity(room)).thenReturn(List.of(alice, bob));

			// when
			List<PlayerDTO> result = roomService.getPlayersByRoom(sessionCode, creatorHash);

			// then
			assertThat(result).hasSize(2);
			assertThat(result).extracting(PlayerDTO::name).containsExactly("Alice", "Bob");
		}

		/**
		 * When no players have joined, the method should return an empty list.
		 */
		@Test
		@DisplayName("should return empty list when no players have joined")
		void noPlayers_returnsEmptyList() {
			// given
			RoomEntity room = RoomEntity.createEntityObject("Empty Room", null);
			String sessionCode = room.getSessionCode();
			String creatorHash = room.getCreatorHash();

			when(repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash))
				.thenReturn(Optional.of(room));
			when(playerRepository.findByRoomEntity(room)).thenReturn(List.of());

			// when
			List<PlayerDTO> result = roomService.getPlayersByRoom(sessionCode, creatorHash);

			// then
			assertThat(result).isEmpty();
		}

		/**
		 * Using an incorrect creator hash must throw {@link RoomNotFoundException}.
		 */
		@Test
		@DisplayName("should throw RoomNotFoundException when creator hash is invalid")
		void invalidCreatorHash_throwsRoomNotFoundException() {
			// given
			when(repository.findBySessionCodeAndCreatorHash("ROOM01", "wrong-hash"))
				.thenReturn(Optional.empty());

			// when / then
			assertThatThrownBy(() -> roomService.getPlayersByRoom("ROOM01", "wrong-hash"))
				.isInstanceOf(RoomNotFoundException.class);

			verify(playerRepository, never()).findByRoomEntity(any());
		}
	}
}
