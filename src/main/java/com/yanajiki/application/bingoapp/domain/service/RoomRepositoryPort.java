package com.yanajiki.application.bingoapp.domain.service;

import com.yanajiki.application.bingoapp.domain.model.Room;
import com.yanajiki.application.bingoapp.domain.vo.Id;
import com.yanajiki.application.bingoapp.domain.vo.SessionCode;

import java.util.Optional;

public interface RoomRepositoryPort {

    public Room save(Room room);
    public void deleteRoom(SessionCode sessionCode);
    public Optional<Room> findRoomBySessionCode(SessionCode sessionCode);
}
