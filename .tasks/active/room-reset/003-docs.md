# 003 — Documentation updates

## What to build
Update project docs to reflect both new endpoints (reset + update info), per the
Feature Closeout Checklist in CLAUDE.md.

## Acceptance Criteria
- [ ] `CLAUDE.md` API Endpoints table includes `POST /api/v1/room/{session-code}/reset`
- [ ] `CLAUDE.md` API Endpoints table includes `PATCH /api/v1/room/{session-code}`
- [ ] `docs/FRONTEND_API.md` documents both endpoints: request signature, headers,
      response schema, error table (400, 404), and frontend integration notes
- [ ] `docs/openapi.json` regenerated from the running app (only if the static spec is
      checked in)

## Files to MODIFY
| File | Change |
|------|--------|
| `CLAUDE.md` | Add two rows to API Endpoints table (near DELETE row) |
| `docs/FRONTEND_API.md` | Add "Reset Room" and "Update Room" sections near the delete endpoint; include TS interface for `UpdateRoomForm` |
| `docs/openapi.json` | Regenerate from running app (only if static spec is checked in) |

## Notes

### Frontend integration for reset
- The existing `/room/{sessionCode}` STOMP subscription automatically receives the
  reset as a `RoomDTO` with empty `drawnNumbers` — **no new topic, no new DTO type**.
- Frontend should detect the transition (non-empty → empty `drawnNumbers`) and show
  a "Game was reset" toast to all connected players.
- Confirmation dialog belongs on the creator's side before calling the endpoint —
  the API is intentionally irreversible.

### Frontend integration for update
- The same `/room/{sessionCode}` STOMP subscription receives the updated `RoomDTO` —
  frontend re-renders the description panel.
- PATCH body semantic:
  - absent / `null` → no change
  - `""` → clears description
  - non-empty string → updates description
- TypeScript interface:
  ```ts
  interface UpdateRoomForm {
    description?: string; // null/missing = no change; "" = clear
  }
  ```

### CLAUDE.md row suggestions
```
| POST | /api/v1/room/{session-code}/reset | Reset drawn numbers (blocks during active tiebreak) | X-Creator-Hash (required) |
| PATCH | /api/v1/room/{session-code} | Update room info (description only) | X-Creator-Hash (required) |
```

## Done Definition
All acceptance criteria checked. Docs accurately reflect both endpoints.
