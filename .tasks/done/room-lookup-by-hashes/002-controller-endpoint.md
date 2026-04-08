# 002 — POST /api/v1/room/lookup endpoint + integration test

## What to build
Thin controller endpoint that accepts a body of creator hashes and returns the matching
rooms in creator view. Delegates to `RoomService.findRoomsByCreatorHashes` from task 001.

## Acceptance Criteria
- [ ] `POST /api/v1/room/lookup` accepts `{ "creatorHashes": ["uuid", ...] }`
- [ ] Returns `200 OK` with `List<RoomDTO>` (creator view, includes `creatorHash`)
- [ ] Empty list in body → `200 OK` with `[]`
- [ ] Missing/null `creatorHashes` field → `200 OK` with `[]` (treat as empty)
- [ ] Unknown hashes silently skipped (no 404, no error)
- [ ] OpenAPI annotations match the style of existing endpoints in `RoomController`
- [ ] RestAssured integration test (BDD given/when/then) covers: all-found, partial, none, empty body

## Technical Spec

### Files to CREATE
| File | Package | Purpose |
|------|---------|---------|
| `api/RoomLookupForm.java` | `com.yanajiki.application.bingoapp.api` | Request DTO record: `record RoomLookupForm(List<String> creatorHashes) {}` |
| `api/RoomControllerLookupIT.java` (or extend existing IT) | `com.yanajiki.application.bingoapp.api` (test) | RestAssured integration test |

### Files to MODIFY
| File | Change |
|------|--------|
| `api/RoomController.java` | Add `lookup(@RequestBody RoomLookupForm form)` handler |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `api/RoomController.java` | Existing `@Operation`/`@ApiResponses` annotation style, thin-controller delegation |
| `api/CreateRoomForm.java` | Request DTO record style |
| Existing RestAssured ITs in `api/` package | BDD given/when/then test style, fixture setup |

### Implementation Sketch
```java
@PostMapping("/lookup")
public List<RoomDTO> lookup(@RequestBody RoomLookupForm form) {
    return roomService.findRoomsByCreatorHashes(
        form.creatorHashes() == null ? List.of() : form.creatorHashes()
    );
}
```

### Conventions
- Controller stays thin — no logic beyond null-coalescing the list
- No `@Valid` needed — there's nothing to validate; an empty/null list is legal
- Tabs, Lombok where applicable, OpenAPI annotations on the endpoint

## TDD Sequence
1. Write integration test scenarios (create N rooms, capture hashes, POST to /lookup)
2. Add the form record + controller method
3. Tests pass via PostToolUse hook

## Done Definition
All acceptance criteria checked. Integration tests green. Endpoint visible in Swagger UI.
