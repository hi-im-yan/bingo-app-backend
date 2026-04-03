package com.yanajiki.application.bingoapp.database;

import org.springframework.data.jpa.repository.JpaRepository;
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
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

	Optional<RoomEntity> findBySessionCode(String value);

	Optional<RoomEntity> findByName(String value);

	Optional<RoomEntity> findBySessionCodeAndCreatorHash(String sessionCode, String creatorHash);

	/** Used by the cleanup scheduler to find rooms older than the TTL. */
	List<RoomEntity> findByUpdateDateTimeBefore(Instant cutoff);
}
