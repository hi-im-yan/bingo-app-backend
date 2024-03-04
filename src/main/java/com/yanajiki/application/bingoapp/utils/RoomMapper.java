package com.yanajiki.application.bingoapp.utils;

import com.yanajiki.application.bingoapp.domain.model.Room;
import com.yanajiki.application.bingoapp.domain.vo.*;
import com.yanajiki.application.bingoapp.infra.database.RoomEntity;

public class RoomMapper {

    public static RoomEntity domainToEntity(Room room) {
        RoomEntity roomEntity = new RoomEntity();
        roomEntity.setId(room.getId().getValue());
        roomEntity.setName(room.getName().getValue());
        roomEntity.setPassword(room.getPassword().getValue());
        roomEntity.setDescription(room.getDescription());
        roomEntity.setSessionCode(room.getSessionCode().getValue());
        roomEntity.setCreatorHash(room.getCreatorHash().getValue());
        roomEntity.setDrawnNumbers(room.getDrawnNumbers());

        return roomEntity;
    }

    public static Room entityToDomain(RoomEntity roomEntity) {
        Room room = new Room();
        room.setId(new Id(roomEntity.getId()));
        room.setName(new Name(roomEntity.getName()));
        room.setPassword(new Password(roomEntity.getPassword()));
        room.setDescription(roomEntity.getDescription());
        room.setSessionCode(new SessionCode(roomEntity.getSessionCode()));
        room.setCreatorHash(new CreatorHash(roomEntity.getCreatorHash()));
        room.setDrawnNumbers(roomEntity.getDrawnNumbers());

        return room;
    }
}
