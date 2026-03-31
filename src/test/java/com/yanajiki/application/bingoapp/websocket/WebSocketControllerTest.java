package com.yanajiki.application.bingoapp.websocket;

import com.yanajiki.application.bingoapp.api.response.NumberCorrectionDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.api.response.TiebreakDTO;
import com.yanajiki.application.bingoapp.game.DrawMode;
import com.yanajiki.application.bingoapp.service.CorrectionResult;
import com.yanajiki.application.bingoapp.service.RoomService;
import com.yanajiki.application.bingoapp.service.TiebreakService;
import com.yanajiki.application.bingoapp.websocket.form.CorrectNumberForm;
import com.yanajiki.application.bingoapp.websocket.form.StartTiebreakForm;
import com.yanajiki.application.bingoapp.websocket.form.TiebreakDrawForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebSocketController}.
 * <p>
 * Uses Mockito to verify that the controller delegates correctly to {@link RoomService}
 * and broadcasts the expected payloads to the correct STOMP topics.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

	@Mock
	private RoomService roomService;

	@Mock
	private TiebreakService tiebreakService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private WebSocketController webSocketController;

	// ─── correctNumber ──────────────────────────────────────────────────────────

	@Nested
	@DisplayName("correctNumber")
	class CorrectNumber {

		/**
		 * Verifies that the controller calls the service with the correct arguments,
		 * broadcasts the updated RoomDTO to the room topic, and broadcasts the
		 * NumberCorrectionDTO to the corrections topic.
		 */
		@Test
		@DisplayName("success — calls service and broadcasts to both topics")
		void success_callsServiceAndBroadcastsToBothTopics() {
			// given
			CorrectNumberForm form = new CorrectNumberForm();
			form.setSessionCode("ABC123");
			form.setCreatorHash("hash");
			form.setNewNumber(12);

			RoomDTO roomDTO = new RoomDTO("Test Room", "desc", "ABC123", null,
					List.of(5, 12), List.of("X-5", "X-12"), DrawMode.MANUAL);
			NumberCorrectionDTO correctionDTO = NumberCorrectionDTO.of(42, "X-42", 12, "X-12");
			CorrectionResult result = new CorrectionResult(roomDTO, correctionDTO);

			when(roomService.correctLastNumber("ABC123", "hash", 12)).thenReturn(result);

			// when
			webSocketController.correctNumber(form);

			// then
			verify(roomService).correctLastNumber("ABC123", "hash", 12);
			verify(messagingTemplate).convertAndSend("/room/ABC123", roomDTO);
			verify(messagingTemplate).convertAndSend("/room/ABC123/corrections", correctionDTO);
		}
	}

	// ─── startTiebreak ──────────────────────────────────────────────────────────

	@Nested
	@DisplayName("startTiebreak")
	class StartTiebreakTest {

		@Test
		@DisplayName("success — calls service and broadcasts to tiebreak topic")
		void success_callsServiceAndBroadcasts() {
			// given
			StartTiebreakForm form = new StartTiebreakForm("ABC123", "hash", 3);

			TiebreakDTO dto = new TiebreakDTO("STARTED", 3, List.of(), null);
			when(tiebreakService.startTiebreak("ABC123", "hash", 3)).thenReturn(dto);

			// when
			webSocketController.startTiebreak(form);

			// then
			verify(tiebreakService).startTiebreak("ABC123", "hash", 3);
			verify(messagingTemplate).convertAndSend("/room/ABC123/tiebreak", dto);
		}
	}

	// ─── tiebreakDraw ───────────────────────────────────────────────────────────

	@Nested
	@DisplayName("tiebreakDraw")
	class TiebreakDrawTest {

		@Test
		@DisplayName("in progress — calls service and broadcasts, does not clear")
		void inProgress_broadcastsWithoutClear() {
			// given
			TiebreakDrawForm form = new TiebreakDrawForm("ABC123", "hash", 1);

			TiebreakDTO dto = new TiebreakDTO("IN_PROGRESS", 3,
				List.of(new TiebreakDTO.TiebreakDrawEntry(1, 42, "N-42")), null);
			when(tiebreakService.drawForSlot("ABC123", "hash", 1)).thenReturn(dto);

			// when
			webSocketController.tiebreakDraw(form);

			// then
			verify(tiebreakService).drawForSlot("ABC123", "hash", 1);
			verify(messagingTemplate).convertAndSend("/room/ABC123/tiebreak", dto);
			verify(tiebreakService, never()).clearTiebreak("ABC123");
		}

		@Test
		@DisplayName("finished — broadcasts and clears tiebreaker state")
		void finished_broadcastsAndClears() {
			// given
			TiebreakDrawForm form = new TiebreakDrawForm("ABC123", "hash", 2);

			TiebreakDTO dto = new TiebreakDTO("FINISHED", 2,
				List.of(
					new TiebreakDTO.TiebreakDrawEntry(1, 42, "N-42"),
					new TiebreakDTO.TiebreakDrawEntry(2, 65, "O-65")),
				2);
			when(tiebreakService.drawForSlot("ABC123", "hash", 2)).thenReturn(dto);

			// when
			webSocketController.tiebreakDraw(form);

			// then
			verify(messagingTemplate).convertAndSend("/room/ABC123/tiebreak", dto);
			verify(tiebreakService).clearTiebreak("ABC123");
		}
	}
}
