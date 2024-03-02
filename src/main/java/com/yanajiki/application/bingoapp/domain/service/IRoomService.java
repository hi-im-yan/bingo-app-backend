package com.yanajiki.application.bingoapp.domain.service;

import com.yanajiki.application.bingoapp.domain.model.Room;
import com.yanajiki.application.bingoapp.domain.vo.SessionCode;

import java.util.Optional;

public interface IRoomService {

    public Room createRoom(Room room);
    public Room updateRoom(Room room);
    public void deleteRoom(SessionCode sessionCode);
    public Room findRoomBySessionCode(SessionCode sessionCode);
}
