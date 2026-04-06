# 002 — ContactForm DTO

## What to build
Request DTO for the contact form endpoint. Name and content are required. Email and phone are fully optional (users can submit anonymously or provide contact info if they want a response).

## Acceptance Criteria
- [ ] `ContactForm` has fields: name (required), email (optional, @Email if provided), phone (optional), content (required)
- [ ] No custom validator — email and phone are independently optional
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `ContactForm.java` | `com.yanajiki.application.bingoapp.api.form` | Request DTO |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `api/form/CreateRoomForm.java` | Lombok annotations, Jakarta validation pattern |

### Implementation Details

```java
@NoArgsConstructor
@Getter
@Setter
public class ContactForm {

	@NotBlank
	@Size(max = 100)
	private String name;

	@Email
	@Size(max = 254)
	private String email;

	@Size(max = 20)
	private String phone;

	@NotBlank
	@Size(max = 2000)
	private String content;
}
```

No `@AtLeastOneContact` validator. Email and phone are both optional. If the user wants a response, they provide one. If they just want to tell you something, they don't have to.

### Conventions (from project CLAUDE.md)
- Request DTOs: mutable Lombok classes (`@NoArgsConstructor`, `@Getter`, `@Setter`)
- Jakarta validation annotations for constraints
- Tabs for indentation

## TDD Sequence
1. Write `ContactForm` — simple DTO, no separate test needed (covered by integration test in task 006)
2. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
