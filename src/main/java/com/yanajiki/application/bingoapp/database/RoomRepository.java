package com.yanajiki.application.bingoapp.database;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link RoomEntity}.
 * <p>
 * Provides CRUD operations plus derived-query finders for session-code lookup,
 * creator authentication, and expiration-based cleanup.
 * </p>
 */
@Repository
public interface RoomRepository extends CrudRepository<RoomEntity, Long> {

	/**
	 * Looks up a room by its public session code.
	 *
	 * @param value the session code
	 * @return the matching room, or empty if none found
	 */
	Optional<RoomEntity> findBySessionCode(String value);

	/**
	 * Looks up a room by its unique name.
	 *
	 * @param value the room name
	 * @return the matching room, or empty if none found
	 */
	Optional<RoomEntity> findByName(String value);

	/**
	 * Looks up a room by session code and creator hash, used to authenticate privileged actions.
	 *
	 * @param sessionCode the public session code
	 * @param creatorHash the secret creator hash
	 * @return the matching room, or empty if no match
	 */
	Optional<RoomEntity> findBySessionCodeAndCreatorHash(String sessionCode, String creatorHash);

	/**
	 * Finds all rooms whose {@code updateDateTime} is strictly before the given cutoff.
	 * Used by the cleanup scheduler to identify stale rooms eligible for deletion.
	 *
	 * @param cutoff the instant before which rooms are considered expired
	 * @return list of expired rooms (may be empty, never null)
	 */
	List<RoomEntity> findByUpdateDateTimeBefore(Instant cutoff);
}
