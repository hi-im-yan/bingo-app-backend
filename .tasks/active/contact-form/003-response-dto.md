# 003 — ContactMessageDTO Response Record

## What to build
A Java record for the contact message API response, with a static factory method for entity-to-DTO conversion.

## Acceptance Criteria
- [ ] `ContactMessageDTO` is a Java record
- [ ] Fields: id, name, email, phone, content, createdAt
- [ ] `@JsonInclude(NON_NULL)` to omit null email/phone
- [ ] Static `fromEntity(ContactMessageEntity)` factory method
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `ContactMessageDTO.java` | `com.yanajiki.application.bingoapp.api.response` | Response record |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `api/response/PlayerDTO.java` | Record structure, factory method pattern |
| `api/response/RoomDTO.java` | `@JsonInclude(NON_NULL)` pattern |

### Implementation Details
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContactMessageDTO(
	Long id,
	String name,
	String email,
	String phone,
	String content,
	Instant createdAt
) {
	public static ContactMessageDTO fromEntity(ContactMessageEntity entity) {
		return new ContactMessageDTO(
			entity.getId(),
			entity.getName(),
			entity.getEmail(),
			entity.getPhone(),
			entity.getContent(),
			entity.getCreatedAt()
		);
	}
}
```

### Conventions (from project CLAUDE.md)
- Response DTOs: Java records
- `@JsonInclude(NON_NULL)` for optional fields
- Static factory methods for entity→DTO conversion
- Tabs for indentation

## TDD Sequence
1. No separate test — covered by ContactService unit test (task 005) and integration test (task 006)
2. Write `ContactMessageDTO`
3. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
