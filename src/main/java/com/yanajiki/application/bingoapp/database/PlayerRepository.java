package com.yanajiki.application.bingoapp.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link PlayerEntity}.
 * <p>
 * Provides CRUD operations and custom query methods for retrieving and checking
 * players within a specific bingo room.
 * </p>
 */
@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {

	List<PlayerEntity> findByRoomEntity(RoomEntity roomEntity);

	boolean existsByNameAndRoomEntity(String name, RoomEntity roomEntity);
}
