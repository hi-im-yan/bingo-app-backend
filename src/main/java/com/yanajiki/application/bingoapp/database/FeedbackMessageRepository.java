package com.yanajiki.application.bingoapp.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link FeedbackMessageEntity}.
 * <p>
 * Provides standard CRUD operations for persisting and retrieving feedback messages.
 * </p>
 */
@Repository
public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessageEntity, Long> {}
