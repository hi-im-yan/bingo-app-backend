# 003 — Documentation updates

## What to build
Update project docs to reflect the new reset endpoint, per the Feature Closeout Checklist
in CLAUDE.md.

## Acceptance Criteria
- [ ] `CLAUDE.md` API Endpoints table includes `POST /api/v1/room/{session-code}/reset`
- [ ] `docs/FRONTEND_API.md` documents the endpoint: request signature, headers, response
      schema, error table (404, 400), and frontend integration note
- [ ] `docs/openapi.json` regenerated from the running app (only if the static spec is
      checked in)

## Files to MODIFY
| File | Change |
|------|--------|
| `CLAUDE.md` | Add row to API Endpoints table (near DELETE row) |
| `docs/FRONTEND_API.md` | Add "Reset Room" section near the delete endpoint; include TS interface guidance |
| `docs/openapi.json` | Regenerate from running app (only if static spec is checked in) |

## Notes
- Frontend integration note for `FRONTEND_API.md`:
  - The existing `/room/{sessionCode}` STOMP subscription automatically receives the
    reset as a `RoomDTO` with empty `drawnNumbers` — **no new topic, no new DTO type**.
  - Frontend should detect the transition (non-empty → empty `drawnNumbers`) and show
    a "Game was reset" toast to all connected players.
  - Confirmation dialog belongs on the creator's side before calling the endpoint —
    the API is intentionally irreversible.
- CLAUDE.md row suggestion:
  `| POST | /api/v1/room/{session-code}/reset | Reset drawn numbers (blocks during active tiebreak) | X-Creator-Hash (required) |`

## Done Definition
All acceptance criteria checked. Docs accurately reflect the implemented endpoint.
