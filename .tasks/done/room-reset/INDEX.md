# Feature: Reset Room + Update Info

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/room-reset
**Plan**: `~/.claude/plans/purrfect-snacking-barto.md` (supersedes `~/.claude/plans/velvet-launching-squirrel.md`)

## Context
Creators currently have no way to restart a game without deleting the room and forcing
every player to rejoin. This feature adds two independent creator-only operations:

1. **Reset** — clears drawn numbers on an existing room so the same players can play another game.
2. **Update info** — PATCH endpoint to edit room description (e.g. change paper color between games).

Both broadcast the refreshed `RoomDTO` on the existing `/room/{sessionCode}` STOMP topic.
Frontend composes them between games: reset, then patch (or either alone).

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | `RoomService.resetRoom` + unit tests | pending | — | Implementer |
| 002 | `POST /api/v1/room/{session-code}/reset` controller + STOMP broadcast + integration test | pending | 001 | Implementer |
| 004 | `UpdateRoomForm` + `RoomService.updateRoom` + unit tests | pending | — | Implementer |
| 005 | `PATCH /api/v1/room/{session-code}` controller + STOMP broadcast + integration test | pending | 004 | Implementer |
| 003 | Docs: CLAUDE.md endpoint table + docs/FRONTEND_API.md + regenerate openapi.json (both endpoints) | pending | 002, 005 | Implementer |

## Decisions

### Reset (unchanged from prior plan)
- **POST `/{session-code}/reset` (not DELETE/PUT)** — action with state transition and
  broadcast side effect. POST-on-sub-resource is the conventional fit for "perform this action".
- **Single-event broadcast** — no pre-warning / countdown / cancellation. The updated
  `RoomDTO` on the existing `/room/{sessionCode}` topic is enough; the frontend shows a
  "game reset" toast by detecting the `drawnNumbers` transition (N → 0).
- **Block reset during active tiebreak** — if `tiebreakService.hasActiveTiebreak` is true,
  reject with `400 TIEBREAK_ALREADY_ACTIVE`. Simpler than cascading state cleanup and
  avoids racing with live tiebreak draws.
- **404 ambiguity for auth failures** — unknown session code **or** wrong creator hash
  both return `404 ROOM_NOT_FOUND`, mirroring existing delete/draw semantics.
- **Irreversible, no confirmation token** — confirmation belongs in the frontend UX.
- **TTL reset is a bonus** — `@UpdateTimestamp` on save extends the 24h expiration.

### Update info (new)
- **Separate PATCH endpoint (not combined with reset)** — single responsibility. Reset
  stays body-less; PATCH handles metadata updates. Frontend composes both between games.
  Reset alone is still useful (restart with same settings); PATCH alone is still useful
  (fix a typo or change paper color without resetting).
- **PATCH `/{session-code}` (not PUT)** — partial update. PUT would imply full replacement.
- **Description only (not name, drawMode, etc.)** — minimal scope per stated use case.
  Name changes rarely useful; drawMode changes mid-session would desync live draws/tiebreaks.
  Easy to extend later if needed.
- **Null/missing field = no change; empty string = clear** — standard PATCH semantic.
  Avoids needing JsonNullable.
- **Not blocked during active tiebreak** — description edits are harmless vs. in-flight tiebreak state.
- **Same broadcast topic** — reuses `/room/{sessionCode}` with refreshed `RoomDTO`.
  Frontend detects `description` field change.

### Shared
- **No new error codes, DTOs (beyond `UpdateRoomForm`), entities, or WS topics** —
  everything reuses existing primitives.
