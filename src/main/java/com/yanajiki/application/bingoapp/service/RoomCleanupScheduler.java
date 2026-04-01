package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled component that periodically removes stale bingo rooms from the database.
 * <p>
 * A room is considered expired when its {@code updateDateTime} has not been refreshed
 * within the configured TTL window ({@code room.cleanup.ttl-hours}, default 24 h).
 * The cleanup runs at a fixed rate controlled by {@code room.cleanup.interval-ms}
 * (default every hour). Deletion is performed in a single batch via
 * {@link RoomRepository#deleteAll}.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoomCleanupScheduler {

	private final RoomRepository roomRepository;

	/** How many hours a room may be idle before it is eligible for deletion. */
	@Value("${room.cleanup.ttl-hours:24}")
	private long ttlHours;

	/**
	 * Finds and deletes all rooms whose {@code updateDateTime} is older than
	 * {@code now minus ttlHours}. Runs at the rate specified by
	 * {@code room.cleanup.interval-ms} (default 3 600 000 ms = 1 hour).
	 */
	@Scheduled(fixedRateString = "${room.cleanup.interval-ms:3600000}")
	public void cleanupExpiredRooms() {
		Instant cutoff = Instant.now().minus(ttlHours, ChronoUnit.HOURS);
		List<RoomEntity> expired = roomRepository.findByUpdateDateTimeBefore(cutoff);

		if (expired.isEmpty()) {
			log.debug("No expired rooms found.");
			return;
		}

		log.info("Deleting {} expired room(s) with updateDateTime before {}.", expired.size(), cutoff);
		roomRepository.deleteAll(expired);
		log.info("Expired room cleanup complete.");
	}
}
