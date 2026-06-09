# Feature: Tiebreaker in Manual Mode

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/tiebreak-manual-mode

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Backend — remove AUTOMATIC-only gate in `startTiebreak` + flip test | ready | — | Implementer |
| 002 | Frontend — render `AdminTiebreakPanel` in MANUAL branch + test | ready | — | Implementer |
| 003 | Docs — backend CLAUDE.md + frontend FRONTEND_API.md | blocked | 001, 002 | Implementer |

## Decisions
- Tiebreaker restriction is product-level, not technical. The tiebreak is its own
  random-draw competition (draws from the undrawn pool, highest number wins) and works
  identically regardless of the room's main draw mode.
- Manual tiebreak behaves **identically** to automatic — random draw from the undrawn pool.
  The GM does NOT manually pick tiebreak numbers.
- `DRAW_MODE_MISMATCH` ErrorCode stays — still used by the manual/automatic draw endpoints
  (`add-number` requires MANUAL, `draw-number` requires AUTOMATIC). Only the tiebreak usage
  is removed.
- 001 (backend) and 002 (frontend) are independent — separate repos, can run in parallel.
- Backend repo: `/home/yanaj/projects/bingoapp`. Frontend repo: `/home/yanaj/projects/bingo-app-frontend`.
