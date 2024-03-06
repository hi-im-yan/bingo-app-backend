package com.yanajiki.application.bingoapp.websocket;

import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.websocket.form.AddNumberForm;
import lombok.AllArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
public class WebSocketController {

    private final RoomRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/add-number")
    public void greeting(AddNumberForm message) throws Exception {
        String sessionCode = message.getSessionCode();
        String dynamicTopic = "/room/" + sessionCode;

        RoomEntity roomEntity = repository.findBySessionCodeAndCreatorHash(message.getSessionCode(), message.getCreatorHash())
                .orElseThrow(() -> new RoomNotFoundException("Room not found."));

        roomEntity.setDrawnNumbers(message.getNumber().toString());

        repository.save(roomEntity);

        // Your logic here, process the message if needed

        // Send the message to the dynamic topic
        messagingTemplate.convertAndSend(dynamicTopic, RoomDTO.fromEntityToPlayer(roomEntity));
    }
}
