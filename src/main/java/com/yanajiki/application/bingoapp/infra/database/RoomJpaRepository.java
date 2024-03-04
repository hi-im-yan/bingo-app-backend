package com.yanajiki.application.bingoapp.infra.database;

import com.yanajiki.application.bingoapp.domain.exception.RoomNotFoundException;
import com.yanajiki.application.bingoapp.domain.model.Room;
import com.yanajiki.application.bingoapp.domain.service.RoomRepositoryPort;
import com.yanajiki.application.bingoapp.domain.vo.SessionCode;
import com.yanajiki.application.bingoapp.utils.RoomMapper;
import lombok.AllArgsConstructor;

import java.util.Optional;

@AllArgsConstructor
public class RoomJpaRepository implements RoomRepositoryPort {

    private final RoomRepository repository;

    @Override
    public Room save(Room room) {
        RoomEntity roomEntity = RoomMapper.domainToEntity(room);

        RoomEntity saved = repository.save(roomEntity);

        return RoomMapper.entityToDomain(saved);
    }

    @Override
    public void deleteRoom(SessionCode sessionCode) {

        RoomEntity roomEntity = repository.findBySessionCode(sessionCode.getValue())
                .orElseThrow(() -> new RoomNotFoundException("Room not Found."));

        repository.delete(roomEntity);
    }

    @Override
    public Optional<Room> findRoomBySessionCode(SessionCode sessionCode) {
        RoomEntity roomEntity = repository.findBySessionCode(sessionCode.getValue())
                .orElseThrow(() -> new RoomNotFoundException("Room not Found."));

        return Optional.of(RoomMapper.entityToDomain(roomEntity));
    }
}
