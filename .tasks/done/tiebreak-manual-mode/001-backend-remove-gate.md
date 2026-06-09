# 001 — Backend: Remove AUTOMATIC-only gate from tiebreaker

## What to build
Allow MANUAL-mode rooms to start a tiebreaker. The tiebreaker mechanic is unchanged
(random draws from the undrawn pool); only the draw-mode restriction is removed.

Repo: `/home/yanaj/projects/bingoapp`

## Acceptance Criteria
- [ ] `TiebreakService.startTiebreak` no longer rejects MANUAL rooms.
- [ ] A MANUAL room can start a tiebreaker and draw for slots exactly like an AUTOMATIC room.
- [ ] `TiebreakServiceTest` proves a MANUAL room starts a tiebreaker successfully (test flipped).
- [ ] All other tiebreak validations (player count, active-tiebreak, pool size) still enforced.
- [ ] All tests pass (`mvn test`).

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `src/main/java/com/yanajiki/application/bingoapp/service/TiebreakService.java` | Delete the `DrawMode.AUTOMATIC` guard; update Javadoc |
| `src/test/java/com/yanajiki/application/bingoapp/service/TiebreakServiceTest.java` | Replace the "MANUAL throws" test with a "MANUAL succeeds" test |

### Implementation Details

**TiebreakService.java** — remove this block from `startTiebreak` (currently ~lines 66-68):
```java
if (entity.getDrawMode() != DrawMode.AUTOMATIC) {
    throw new BadRequestException(ErrorCode.DRAW_MODE_MISMATCH, "Tiebreaker is only available for automatic draw mode rooms");
}
```
- The `DrawMode` import becomes unused after this removal — delete the import to avoid a warning.
- Update the method Javadoc: remove "is in AUTOMATIC mode" from the validation list and the
  `@throws ... if the room is not AUTOMATIC` clause. Adjust the class-level Javadoc only if it
  claims AUTOMATIC-only (it does not currently — leave it unless inaccurate).
- Do NOT remove the `DRAW_MODE_MISMATCH` enum entry from `ErrorCode` — it is still used by the
  manual/automatic draw endpoints elsewhere.

**TiebreakServiceTest.java** — the existing test (~lines 109-118):
```java
@DisplayName("wrong mode (MANUAL) — throws BadRequestException")
void manualRoom_throwsIllegalArgumentException() {
    RoomEntity entity = RoomEntity.createEntityObject("Manual Room", null);
    ...
    .hasMessageContaining("automatic");
}
```
Replace it with a test that stubs a MANUAL room and asserts `startTiebreak` returns a
`TiebreakDTO` with status `STARTED` (mirror the success assertions used by the existing
AUTOMATIC happy-path test in the same file — find the test that uses `stubAutomaticRoom`
and starts a tiebreak, and follow its structure). Use `RoomEntity.createEntityObject(name, null)`
for a MANUAL room (the 2-arg overload defaults to MANUAL), and stub
`roomRepository.findBySessionCodeAndCreatorHash(...)` to return it, matching how
`stubAutomaticRoom` wires the mock.

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `TiebreakServiceTest.java` (the `stubAutomaticRoom` helper + the start-tiebreak happy-path test) | Mock wiring, @Nested/@DisplayName style, success assertions on TiebreakDTO |

### Conventions
- JUnit 5 + Mockito, `@Nested` + `@DisplayName` organization.
- Never throw raw `IllegalArgumentException`/`IllegalStateException` in services — domain
  exceptions only (not relevant here since we're removing a throw, but keep it in mind).
- Tabs for indentation, camelCase.

## TDD Sequence
1. Flip the test in `TiebreakServiceTest` — MANUAL room now expects a successful `STARTED` result. Run it; it fails against current code (gate still rejects).
2. Remove the AUTOMATIC guard + unused import in `TiebreakService`.
3. Run `mvn test` — all green.

## Done Definition
All acceptance criteria checked. Tests green. No unused-import or compilation warnings.
