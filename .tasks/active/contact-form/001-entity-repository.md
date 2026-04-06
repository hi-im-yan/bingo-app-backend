# 001 — ContactMessageEntity + ContactMessageRepository

## What to build
JPA entity for the `contact_messages` table and its Spring Data JPA repository. This is the persistence layer for contact form submissions.

## Acceptance Criteria
- [ ] `ContactMessageEntity` maps to `contact_messages` table
- [ ] Fields: id (auto-generated), name, email (nullable), phone (nullable), content, createdAt
- [ ] Static factory method `create(name, email, phone, content)`
- [ ] `ContactMessageRepository` extends `JpaRepository<ContactMessageEntity, Long>`
- [ ] Entity test verifies factory method sets fields correctly
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `ContactMessageEntity.java` | `com.yanajiki.application.bingoapp.database` | JPA entity |
| `ContactMessageRepository.java` | `com.yanajiki.application.bingoapp.database` | Spring Data JPA interface |
| `ContactMessageEntityTest.java` | `com.yanajiki.application.bingoapp.database` (test) | Entity unit test |

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
@Table(name = "contact_messages")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ContactMessageEntity {

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

	public static ContactMessageEntity create(String name, String email, String phone, String content) {
		ContactMessageEntity entity = new ContactMessageEntity();
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
public interface ContactMessageRepository extends JpaRepository<ContactMessageEntity, Long> {}
```

### Conventions (from project CLAUDE.md)
- Lombok: `@NoArgsConstructor`, `@AllArgsConstructor`, `@Getter`, `@Setter`
- Tabs for indentation
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` for IDs
- `@CreationTimestamp` for auto-managed timestamps
- Static factory methods for entity construction

## TDD Sequence
1. Write `ContactMessageEntityTest` — test factory method sets all fields
2. Write `ContactMessageEntity` — make test pass
3. Write `ContactMessageRepository` — interface only, no test needed
4. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
