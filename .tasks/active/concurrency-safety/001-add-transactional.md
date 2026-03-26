# 001 — Add @Transactional to Compound Read-Write Service Methods

## What to build
Add `@Transactional` annotations to all RoomService methods that perform read-modify-save cycles, ensuring atomicity and preventing race conditions (e.g., two concurrent users drawing the same number).

## Acceptance Criteria
- [ ] `drawNumber()` annotated with `@Transactional`
- [ ] `drawRandomNumber()` annotated with `@Transactional`
- [ ] Any other compound read-write methods also annotated (audit full service)
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `RoomService.java` | Add `@Transactional` to `drawNumber()`, `drawRandomNumber()`, and any other read-modify-save methods |

### Files to READ (for context — do NOT modify)
| File | What to check |
|------|---------------|
| `RoomEntity.java` | Understand entity state transitions during draws |
| `RoomRepository.java` | Verify repository extends Spring Data (transaction proxy support) |

### Implementation Details

Add `import org.springframework.transaction.annotation.Transactional;` and annotate:

```java
@Transactional
public RoomDTO drawNumber(String sessionCode, String creatorHash, int number) { ... }

@Transactional
public RoomDTO drawRandomNumber(String sessionCode, String creatorHash) { ... }
```

Audit the entire service for other methods that read an entity, modify it, and save it back. Read-only methods (findBySessionCode, etc.) do NOT need `@Transactional` — Spring Data handles those.

## TDD Sequence
1. Read existing unit tests to verify they still pass with @Transactional (Mockito tests are unaffected)
2. Add @Transactional annotations
3. Run `mvn test` — all existing tests must pass

## Done Definition
All compound read-write service methods are transactional. No regressions.
