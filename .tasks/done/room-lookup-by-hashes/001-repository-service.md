# 001 — Repository + Service: findRoomsByCreatorHashes

## What to build
Add a batch lookup that resolves a list of creator hashes to their rooms. Repository
method + service method + unit test. Unknown hashes are silently dropped.

## Acceptance Criteria
- [ ] `RoomRepository.findAllByCreatorHashIn(Collection<String>)` returns matching rooms
- [ ] `RoomService.findRoomsByCreatorHashes(List<String>)` returns `List<RoomDTO>` in **creator view**
- [ ] Empty/null input list returns empty list (no exception, no DB call)
- [ ] Unknown hashes are silently skipped — response contains only existing rooms
- [ ] Unit test covers: all-found, partial-found, none-found, empty input
- [ ] All tests pass (`mvn test` runs via project hook — do not invoke manually)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `database/RoomRepository.java` | Add `List<RoomEntity> findAllByCreatorHashIn(Collection<String> creatorHashes);` |
| `service/RoomService.java` | Add `findRoomsByCreatorHashes(List<String>)` returning `List<RoomDTO>` (creator view) |

### Files to CREATE
| File | Purpose |
|------|---------|
| `service/RoomServiceTest.java` (extend if exists) | Unit test for the new service method |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `database/RoomRepository.java` | Spring Data derived query style |
| `service/RoomService.java` | Existing creator-view DTO mapping (mirror `findRoomBySessionCode` when `creatorHash` matches) |
| `api/RoomDTO.java` | Creator view shape |

### Implementation Notes
- Guard: `if (creatorHashes == null || creatorHashes.isEmpty()) return List.of();`
- Reuse the existing entity→creator-view mapper rather than duplicating it.
- No new exceptions — empty result is a valid outcome, not an error.
- Order of returned rooms is not contractual; do not sort unless a test demands it.

## TDD Sequence
1. Add service unit test cases (mock repository): all-found, partial, none, empty input
2. Add repository method signature
3. Implement service method to make tests pass

## Done Definition
All acceptance criteria checked. Tests green via the PostToolUse hook.
