# Feature: Reset Room Numbers

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/room-reset
**Plan**: `~/.claude/plans/velvet-launching-squirrel.md`

## Context
Creators currently have no way to restart a game without deleting the room and forcing
every player to rejoin. This adds a creator-only REST endpoint that clears a room's
drawn numbers and broadcasts the fresh state to all connected players so their UIs
re-render automatically.

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | `RoomService.resetRoom` + unit tests | pending | — | Implementer |
| 002 | `POST /api/v1/room/{session-code}/reset` controller + STOMP broadcast + integration test | pending | 001 | Implementer |
| 003 | Docs: CLAUDE.md endpoint table + docs/FRONTEND_API.md + regenerate openapi.json | pending | 002 | Implementer |

## Decisions
- **POST `/{session-code}/reset` (not DELETE/PUT)** — action is a state transition with
  side effects (broadcast) and returns a resource representation. DELETE implies resource
  removal; PUT implies replacing the full resource. POST-on-sub-resource is the
  conventional fit for "perform this action".
- **Single-event broadcast** — no pre-warning / countdown / cancellation. The updated
  `RoomDTO` on the existing `/room/{sessionCode}` topic is enough; the frontend shows a
  "game reset" toast by detecting the `drawnNumbers` transition. Matches the existing
  draw-broadcast pattern, no new topic or DTO.
- **Block reset during active tiebreak** — if `tiebreakService.hasActiveTiebreak` is true,
  reject with `400 TIEBREAK_ALREADY_ACTIVE`. Simpler than cascading state cleanup and
  avoids racing with live tiebreak draws.
- **404 ambiguity for auth failures** — unknown session code **or** wrong creator hash
  both return `404 ROOM_NOT_FOUND`, mirroring the existing delete/draw semantics. We
  don't leak room existence to callers without the hash.
- **Irreversible, no confirmation token** — confirmation belongs in the frontend UX
  (dialog), not the API.
- **TTL reset is a bonus** — saving the entity via `@UpdateTimestamp` extends the 24h
  expiration. Desirable side effect, no extra logic required.
- **No new error codes, DTOs, entities, or WS topics** — everything reuses existing
  primitives (`TIEBREAK_ALREADY_ACTIVE`, `ROOM_NOT_FOUND`, `RoomDTO.fromEntityToPlayer`,
  `/room/{sessionCode}` topic).
