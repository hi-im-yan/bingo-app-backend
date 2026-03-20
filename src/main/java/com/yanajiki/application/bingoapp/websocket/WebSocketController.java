package com.yanajiki.application.bingoapp.websocket;

import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.service.RoomService;
import com.yanajiki.application.bingoapp.websocket.form.AddNumberForm;
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
}
