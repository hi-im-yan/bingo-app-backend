package com.yanajiki.application.bingoapp.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity representing a player in a bingo room.
 * <p>
 * Each player has a display name and belongs to exactly one {@link RoomEntity}.
 * A unique constraint on {@code (name, room_id)} prevents duplicate player names
 * within the same room. Player names are allowed to be reused across different rooms.
 * </p>
 * <p>
 * Use the {@link #create(String, RoomEntity)} factory method to build a new player
 * ready for persistence.
 * </p>
 */
@Entity
@Table(name = "player", uniqueConstraints = {
	@UniqueConstraint(columnNames = {"name", "room_id"})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlayerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private RoomEntity roomEntity;

	@CreationTimestamp
	private LocalDateTime joinDateTime;

	public static PlayerEntity create(String name, RoomEntity roomEntity) {
		PlayerEntity player = new PlayerEntity();
		player.setName(name);
		player.setRoomEntity(roomEntity);
		return player;
	}
}
