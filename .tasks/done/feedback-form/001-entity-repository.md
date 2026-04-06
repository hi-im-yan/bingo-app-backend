# 001 — FeedbackMessageEntity + FeedbackMessageRepository

## What to build
JPA entity for the `feedback_messages` table and its Spring Data JPA repository. This is the persistence layer for feedback form submissions.

## Acceptance Criteria
- [ ] `FeedbackMessageEntity` maps to `feedback_messages` table
- [ ] Fields: id (auto-generated), name, email (nullable), phone (nullable), content, createdAt
- [ ] Static factory method `create(name, email, phone, content)`
- [ ] `FeedbackMessageRepository` extends `JpaRepository<FeedbackMessageEntity, Long>`
- [ ] Entity test verifies factory method sets fields correctly
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `FeedbackMessageEntity.java` | `com.yanajiki.application.bingoapp.database` | JPA entity |
| `FeedbackMessageRepository.java` | `com.yanajiki.application.bingoapp.database` | Spring Data JPA interface |
| `FeedbackMessageEntityTest.java` | `com.yanajiki.application.bingoapp.database` (test) | Entity unit test |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `database/RoomEntity.java` | Lombok annotations, @CreationTimestamp, factory method pattern |
| `database/PlayerEntity.java` | @ManyToOne pattern (not needed here, but reference for entity style) |
| `database/RoomRepository.java` | Repository interface pattern |

### Implementation Details

**Entity:**
```java
@Entity
@Table(name = "feedback_messages")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FeedbackMessageEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 254)
	private String email;

	@Column(length = 20)
	private String phone;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@CreationTimestamp
	private Instant createdAt;

	public static FeedbackMessageEntity create(String name, String email, String phone, String content) {
		FeedbackMessageEntity entity = new FeedbackMessageEntity();
		entity.setName(name);
		entity.setEmail(email);
		entity.setPhone(phone);
		entity.setContent(content);
		return entity;
	}
}
```

**Repository:**
```java
@Repository
public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessageEntity, Long> {}
```

### Conventions (from project CLAUDE.md)
- Lombok: `@NoArgsConstructor`, `@AllArgsConstructor`, `@Getter`, `@Setter`
- Tabs for indentation
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` for IDs
- `@CreationTimestamp` for auto-managed timestamps
- Static factory methods for entity construction

## TDD Sequence
1. Write `FeedbackMessageEntityTest` — test factory method sets all fields
2. Write `FeedbackMessageEntity` — make test pass
3. Write `FeedbackMessageRepository` — interface only, no test needed
4. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
