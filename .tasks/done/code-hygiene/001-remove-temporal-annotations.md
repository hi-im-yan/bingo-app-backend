# 001 — Remove @Temporal Annotations from RoomEntity

## What to build
Remove the unnecessary `@Temporal(TemporalType.TIMESTAMP)` annotations from `RoomEntity`'s timestamp fields. Hibernate 6+ (Spring Boot 3.x) handles `LocalDateTime` mapping automatically.

## Acceptance Criteria
- [ ] `@Temporal` removed from `createDateTime` field
- [ ] `@Temporal` removed from `updateDateTime` field
- [ ] `import javax.persistence.Temporal` / `import javax.persistence.TemporalType` removed if no longer used
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `RoomEntity.java` | Remove `@Temporal(TemporalType.TIMESTAMP)` from lines ~62 and ~66; remove unused imports |

## Done Definition
No `@Temporal` annotations in entity. Tests green. No behavioral change.
