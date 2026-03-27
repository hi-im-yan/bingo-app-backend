# Feature: GM Number Correction

**Status**: done
**Blocked by feature**: —
**Branch**: feature/gm-number-correction

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | CorrectNumberForm + NumberCorrectionDTO | done | — | — |
| 002 | RoomService.correctLastNumber() + unit tests | done | 001 | — |
| 003 | WebSocket endpoint + dual broadcast | done | 002 | — |
| 004 | Integration test | done | 003 | — |

## Decisions
- Only the last drawn number can be corrected (not arbitrary index)
- Correction **replaces** the old number — old number is removed from drawn list
- MANUAL mode only — AUTOMATIC rooms cannot use this (server picks numbers, no human error)
- Dual broadcast: updated RoomDTO to `/room/{sessionCode}` + NumberCorrectionDTO to `/room/{sessionCode}/corrections`
- WebSocket endpoint (not REST) — consistent with existing draw flow, GM is already on WS connection
- New number must pass same validation as draw: in range [1-75], not already in drawn list
- No entity/DB schema changes — drawnNumbers list is modified in place
