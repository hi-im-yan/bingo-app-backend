# 005 — Reuse Static SecureRandom in selectRandomNumber

## What to build
The `selectRandomNumber()` method in `RoomEntity` creates `new SecureRandom()` on every call, but the class already has a `private static final SecureRandom` instance used for session code generation. Reuse it.

## Acceptance Criteria
- [ ] `selectRandomNumber()` uses the existing static `SecureRandom` instance
- [ ] No `new SecureRandom()` instantiation in that method
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `RoomEntity.java` | In `selectRandomNumber()`, replace `new SecureRandom()` with the existing static field (likely named `RANDOM` or `SECURE_RANDOM`) |

### Note
Check the field name first — it's used for session code generation. `SecureRandom` is thread-safe, so sharing it is fine.

## Done Definition
Single static SecureRandom instance used for both session codes and random number selection. Tests green.
