package com.yanajiki.application.bingoapp.websocket;

import com.yanajiki.application.bingoapp.api.response.PlayerDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.service.CorrectionResult;
import com.yanajiki.application.bingoapp.service.RoomService;
import com.yanajiki.application.bingoapp.websocket.form.AddNumberForm;
import com.yanajiki.application.bingoapp.websocket.form.CorrectNumberForm;
import com.yanajiki.application.bingoapp.websocket.form.DrawNumberForm;
import com.yanajiki.application.bingoapp.websocket.form.JoinRoomForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time bingo room events.
 * <p>
 * Handles STOMP message mapping only — delegates all business logic to {@link RoomService}
 * and uses {@link SimpMessagingTemplate} to broadcast updates to subscribed clients.
 * </p>
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

	private final RoomService roomService;
	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * Handles a draw-number event from the creator.
	 * <p>
	 * Delegates to {@link RoomService#drawNumber} to validate the creator, add the number,
	 * and persist. Broadcasts the updated player-view room state to all subscribers of
	 * the room's dynamic topic ({@code /room/{sessionCode}}).
	 * </p>
	 *
	 * @param message the incoming message containing session code, creator hash, and the number to draw
	 * @throws Exception if the room is not found or the number is invalid
	 */
	@MessageMapping("/add-number")
	public void drawNumber(AddNumberForm message) throws Exception {
		String sessionCode = message.getSessionCode();
		String dynamicTopic = "/room/" + sessionCode;

		RoomDTO updatedRoom = roomService.drawNumber(sessionCode, message.getCreatorHash(), message.getNumber());

		log.debug("Broadcasting draw update for room '{}' to topic '{}'", sessionCode, dynamicTopic);
		messagingTemplate.convertAndSend(dynamicTopic, updatedRoom);
	}

	/**
	 * Draws a random number for automatic draw mode rooms.
	 * <p>
	 * Delegates to {@link RoomService#drawRandomNumber} to validate the creator, pick a random
	 * undrawn number, and persist. Broadcasts the updated player-view room state to all subscribers
	 * of the room's dynamic topic ({@code /room/{sessionCode}}).
	 * Only works for rooms in AUTOMATIC draw mode — service enforces this constraint.
	 * </p>
	 *
	 * @param message the draw request containing session code and creator hash
	 */
	/**
	 * Handles a number correction request from the game master.
	 * <p>
	 * Delegates to {@link RoomService#correctLastNumber} to validate the creator, replace the
	 * last drawn number, and persist. Broadcasts the updated player-view room state to
	 * {@code /room/{sessionCode}} and a correction notification to
	 * {@code /room/{sessionCode}/corrections} so connected clients can display the change.
	 * </p>
	 *
	 * @param message the incoming message containing session code, creator hash, and corrected number
	 */
	@MessageMapping("/correct-number")
	public void correctNumber(CorrectNumberForm message) {
		String sessionCode = message.getSessionCode();
		log.info("Number correction requested for room '{}'", sessionCode);

		CorrectionResult result = roomService.correctLastNumber(
				sessionCode, message.getCreatorHash(), message.getNewNumber());

		String roomTopic = "/room/" + sessionCode;
		String correctionTopic = "/room/" + sessionCode + "/corrections";

		log.debug("Broadcasting correction for room '{}': {}", sessionCode, result.correctionDTO().message());
		messagingTemplate.convertAndSend(roomTopic, result.roomDTO());
		messagingTemplate.convertAndSend(correctionTopic, result.correctionDTO());
	}

	@MessageMapping("/draw-number")
	public void drawRandomNumber(DrawNumberForm message) {
		log.info("Automatic draw requested for room: {}", message.getSessionCode());
		RoomDTO roomDTO = roomService.drawRandomNumber(
				message.getSessionCode(), message.getCreatorHash());
		messagingTemplate.convertAndSend(
				"/room/" + message.getSessionCode(), roomDTO);
	}

	/**
	 * Handles a player joining a bingo room.
	 * <p>
	 * Delegates to {@link RoomService#joinRoom} to validate the room, check for duplicate
	 * names, and persist the player. Broadcasts the new player's data to all subscribers
	 * of the room's player topic ({@code /room/{sessionCode}/players}).
	 * </p>
	 *
	 * @param form the join room form containing the player name and session code
	 */
	@MessageMapping("/join-room")
	public void joinRoom(@Valid JoinRoomForm form) {
		log.info("Player '{}' joining room '{}'", form.getPlayerName(), form.getSessionCode());

		PlayerDTO player = roomService.joinRoom(form.getSessionCode(), form.getPlayerName());

		messagingTemplate.convertAndSend(
			"/room/" + form.getSessionCode() + "/players",
			player
		);
	}
}
