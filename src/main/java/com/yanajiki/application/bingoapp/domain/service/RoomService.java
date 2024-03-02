package com.yanajiki.application.bingoapp.domain.service;

import com.yanajiki.application.bingoapp.domain.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.domain.model.Room;
import com.yanajiki.application.bingoapp.domain.vo.SessionCode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class RoomService implements IRoomService {

    private final RoomRepositoryPort repositoryPort;

    @Override
    public Room createRoom(Room room) {
        return repositoryPort.save(room);
    }

    @Override
    public Room updateRoom(Room room) {
        SessionCode sessionCode = room.getSessionCode();
        repositoryPort.findRoomBySessionCode(sessionCode)
                .orElseThrow(() -> new RoomNotFoundException("Room not found for the SessionCode:" + sessionCode));

        return repositoryPort.save(room);
    }

    @Override
    public void deleteRoom(SessionCode sessionCode) {
        try {
            repositoryPort.deleteRoom(sessionCode);
        } catch (Exception e) {
            log.info("Tried to delete a room with a unexisting session code.");
        }
    }

    @Override
    public Room findRoomBySessionCode(SessionCode sessionCode) {
        return repositoryPort.findRoomBySessionCode(sessionCode)
                .orElseThrow(() -> new RoomNotFoundException("Room not found for the SessionCode:" + sessionCode));
    }
}
