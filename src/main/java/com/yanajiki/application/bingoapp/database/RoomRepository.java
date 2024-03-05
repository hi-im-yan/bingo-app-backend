package com.yanajiki.application.bingoapp.database;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends CrudRepository<RoomEntity, String> {
    Optional<RoomEntity> findBySessionCode(String value);

    Optional<RoomEntity> findByName(String value);

    Optional<RoomEntity> findBySessionCodeAndCreatorHash(String sessionCode, String creatorHash);
}
