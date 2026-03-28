package com.yanajiki.application.bingoapp.database;

import org.springframework.data.repository.CrudRepository;
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
public interface PlayerRepository extends CrudRepository<PlayerEntity, Long> {

	/**
	 * Finds all players that have joined the given room.
	 *
	 * @param roomEntity the room whose players should be returned
	 * @return a list of all {@link PlayerEntity} instances associated with the room;
	 *         empty list if no players have joined
	 */
	List<PlayerEntity> findByRoomEntity(RoomEntity roomEntity);

	/**
	 * Checks whether a player with the given name already exists in the given room.
	 *
	 * @param name       the player name to check
	 * @param roomEntity the room to search within
	 * @return {@code true} if a player with that name is already registered in the room;
	 *         {@code false} otherwise
	 */
	boolean existsByNameAndRoomEntity(String name, RoomEntity roomEntity);
}
