package com.yanajiki.application.bingoapp.infra.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<RoomEntity, String> {
    Optional<RoomEntity> findBySessionCode(String value);
}
