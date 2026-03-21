package com.yanajiki.application.bingoapp.database;

import com.yanajiki.application.bingoapp.game.DrawMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing a bingo room.
 * <p>
 * Each room has a unique name, a randomly generated session code used by players to join,
 * and a creator hash used to authenticate administrative actions (e.g., drawing numbers).
 * Drawn numbers are stored as a collection of integers. Range and duplicate validation
 * is performed in the service layer, not here, to keep the entity decoupled from game-specific rules.
 * </p>
 */
@Entity
@Table(name = "room")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RoomEntity {

	private static final int SESSION_CODE_LENGTH = 6;
	private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", unique = true)
	private String name;

	@Column(name = "description")
	private String description;

	@Column(name = "session_code", unique = true)
	private String sessionCode;

	@Column(name = "creator_hash", unique = true)
	private String creatorHash;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "room_drawn_numbers", joinColumns = @JoinColumn(name = "room_id"))
	@Column(name = "number")
	private List<Integer> drawnNumbers = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DrawMode drawMode;

	@CreationTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime createDateTime;

	@UpdateTimestamp
	@Temporal(TemporalType.TIMESTAMP)
	private LocalDateTime updateDateTime;

	/**
	 * Factory method to create a new {@link RoomEntity} with a generated session code and creator hash,
	 * defaulting to {@link DrawMode#MANUAL}.
	 *
	 * @param name        the unique name of the room
	 * @param description a short description of the room
	 * @return a new {@link RoomEntity} ready to be persisted with {@link DrawMode#MANUAL}
	 */
	public static RoomEntity createEntityObject(String name, String description) {
		return createEntityObject(name, description, DrawMode.MANUAL);
	}

	/**
	 * Factory method to create a new {@link RoomEntity} with a generated session code, creator hash,
	 * and the specified draw mode.
	 *
	 * @param name        the unique name of the room
	 * @param description a short description of the room
	 * @param drawMode    the draw mode for the room ({@link DrawMode#MANUAL} or {@link DrawMode#AUTOMATIC})
	 * @return a new {@link RoomEntity} ready to be persisted
	 */
	public static RoomEntity createEntityObject(String name, String description, DrawMode drawMode) {
		String creatorHash = UUID.randomUUID().toString();

		RoomEntity roomEntity = new RoomEntity();
		roomEntity.setName(name);
		roomEntity.setDescription(description);
		roomEntity.setCreatorHash(creatorHash);
		roomEntity.setCreateDateTime(LocalDateTime.now());
		roomEntity.setUpdateDateTime(LocalDateTime.now());
		roomEntity.setDrawnNumbers(new ArrayList<>());
		roomEntity.setSessionCode(newSessionCode());
		roomEntity.setDrawMode(drawMode);
		return roomEntity;
	}

	/**
	 * Appends a drawn number to this room's list of drawn numbers.
	 * <p>
	 * No validation is performed here — range and duplicate checks are the responsibility
	 * of the service layer, which has access to the active {@code NumberLabelMapper} and
	 * can apply game-specific rules without coupling the entity to any particular game type.
	 * </p>
	 *
	 * @param number the bingo number to add
	 */
	public void addDrawnNumber(int number) {
		drawnNumbers.add(number);
	}

	/**
	 * Generates a cryptographically secure random session code of {@value #SESSION_CODE_LENGTH} characters,
	 * using uppercase letters and digits.
	 *
	 * @return a new random session code string
	 */
	private static String newSessionCode() {
		StringBuilder randomChars = new StringBuilder(SESSION_CODE_LENGTH);
		for (int i = 0; i < SESSION_CODE_LENGTH; i++) {
			int randomIndex = SECURE_RANDOM.nextInt(ALLOWED_CHARS.length());
			randomChars.append(ALLOWED_CHARS.charAt(randomIndex));
		}
		return randomChars.toString();
	}
}
