# 001 — PlayerEntity + PlayerRepository

## What to build
Create the JPA entity for tracking players in a bingo room and its Spring Data repository. A player has a name and belongs to a room via a ManyToOne relationship.

## Acceptance Criteria
- [ ] PlayerEntity persists to `player` table with correct columns
- [ ] ManyToOne relationship to RoomEntity works (FK to room.id)
- [ ] Unique constraint on (name, room) prevents duplicate names per room
- [ ] PlayerRepository provides queries by room
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `PlayerEntity.java` | `com.yanajiki.application.bingoapp.database` | JPA entity for players |
| `PlayerRepository.java` | `com.yanajiki.application.bingoapp.database` | Spring Data repository |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `RoomEntity.java` | Lombok annotations, entity structure, @CreationTimestamp pattern |
| `RoomRepository.java` | CrudRepository pattern, custom query methods |

### Implementation Details

**PlayerEntity.java**:
```java
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

	/**
	 * Factory method to create a PlayerEntity for a given room.
	 */
	public static PlayerEntity create(String name, RoomEntity roomEntity) {
		PlayerEntity player = new PlayerEntity();
		player.setName(name);
		player.setRoomEntity(roomEntity);
		return player;
	}
}
```

**PlayerRepository.java**:
```java
public interface PlayerRepository extends CrudRepository<PlayerEntity, Long> {

	/**
	 * Find all players in a room.
	 */
	List<PlayerEntity> findByRoomEntity(RoomEntity roomEntity);

	/**
	 * Check if a player name already exists in a room.
	 */
	boolean existsByNameAndRoomEntity(String name, RoomEntity roomEntity);
}
```

### Conventions (from project CLAUDE.md)
- Tabs for indentation, camelCase naming
- Lombok for boilerplate (@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor)
- Javadoc on public methods and class
- Entity follows same pattern as RoomEntity (factory method, @CreationTimestamp)

## TDD Sequence
1. Write PlayerEntity — define fields, annotations, factory method
2. Write PlayerRepository — define query methods
3. Run test suite — ensure compilation and no regressions

## Done Definition
All acceptance criteria checked. Entity compiles. Repository compiles. No existing tests broken.
