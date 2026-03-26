# 003 — Integration Tests for Concurrent Draw and Duplicate Room Name

## What to build
Integration tests that prove: (1) two concurrent draw requests cannot produce the same number, and (2) creating a room with a duplicate name returns 409.

## Acceptance Criteria
- [ ] Integration test: concurrent draws on the same room produce distinct numbers
- [ ] Integration test: POST /api/v1/room with duplicate name returns 409 Conflict
- [ ] Tests use RestAssured BDD style
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `ConcurrencyIntegrationTest.java` | `src/test/java/.../integration/` | Concurrent draw test using ExecutorService + CountDownLatch |
| `DuplicateRoomNameIntegrationTest.java` | `src/test/java/.../integration/` | Duplicate room name → 409 test |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| Existing integration tests | RestAssured setup, BDD style, @SpringBootTest config |

### Implementation Details

**Concurrent draws test:**
- Create a room, then fire N concurrent draw-number requests (via service or REST)
- Use `ExecutorService` + `CountDownLatch` to synchronize threads
- Assert: all drawn numbers are unique, no duplicates in the final list

**Duplicate room name test:**
- Create a room with name "TestRoom"
- Create another room with name "TestRoom"
- Assert: second call returns 409 with appropriate error message

## TDD Sequence
1. Write both tests (they should fail or expose the issue without the fixes from 001/002)
2. With fixes from 001 and 002 applied, both tests should pass
3. Run `mvn test`

## Done Definition
Both integration tests pass. Concurrent draws are safe. Duplicate name returns 409.
