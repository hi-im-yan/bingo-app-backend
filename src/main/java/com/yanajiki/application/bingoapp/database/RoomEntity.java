package com.yanajiki.application.bingoapp.database;

import com.yanajiki.application.bingoapp.game.DrawMode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.security.SecureRandom;
import java.time.Instant;
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

	@OneToMany(mappedBy = "roomEntity", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PlayerEntity> players = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DrawMode drawMode;

	@CreationTimestamp
	private Instant createDateTime;

	@UpdateTimestamp
	private Instant updateDateTime;

	public static RoomEntity createEntityObject(String name, String description) {
		return createEntityObject(name, description, DrawMode.MANUAL);
	}

	public static RoomEntity createEntityObject(String name, String description, DrawMode drawMode) {
		String creatorHash = UUID.randomUUID().toString();

		RoomEntity roomEntity = new RoomEntity();
		roomEntity.setName(name);
		roomEntity.setDescription(description);
		roomEntity.setCreatorHash(creatorHash);
		roomEntity.setCreateDateTime(Instant.now());
		roomEntity.setUpdateDateTime(Instant.now());
		roomEntity.setDrawnNumbers(new ArrayList<>());
		roomEntity.setSessionCode(newSessionCode());
		roomEntity.setDrawMode(drawMode);
		return roomEntity;
	}

	public void addDrawnNumber(int number) {
		drawnNumbers.add(number);
	}

	private static String newSessionCode() {
		StringBuilder randomChars = new StringBuilder(SESSION_CODE_LENGTH);
		for (int i = 0; i < SESSION_CODE_LENGTH; i++) {
			int randomIndex = SECURE_RANDOM.nextInt(ALLOWED_CHARS.length());
			randomChars.append(ALLOWED_CHARS.charAt(randomIndex));
		}
		return randomChars.toString();
	}
}
