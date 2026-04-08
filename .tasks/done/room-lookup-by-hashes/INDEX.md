# Feature: Room Lookup by Creator Hashes

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/room-lookup-by-hashes

## Context
GMs lose access to their rooms when the session code is forgotten. The frontend already
persists each created room's `creatorHash` in localStorage, but the backend has no way
to resolve those hashes back to rooms. `creator_hash` is `unique = true` on `RoomEntity`
(strict 1:1), so a GM with multiple rooms accumulates multiple hashes — the lookup must
accept a list.

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Repository `findAllByCreatorHashIn` + service method + unit test | done | — | Implementer |
| 002 | `POST /api/v1/room/lookup` controller + integration test | done | 001 | Implementer |
| 003 | Docs: CLAUDE.md endpoint table + docs/FRONTEND_API.md | done | 002 | Implementer |

## Decisions
- **POST over GET** — creator hashes are privileged credentials; keep them out of query
  strings, access logs, browser history, and proxy caches. Also avoids URL length limits
  when a GM has many rooms.
- **Path `/api/v1/room/lookup`** — sibling of existing room endpoints, verb-ish suffix
  signals it's a query-by-body, not a resource fetch.
- **Silently skip unknown/expired hashes** — returns only the rooms that still exist.
  No 404 on partial misses; the response itself tells the frontend what survived. The
  frontend can then prune stale hashes from localStorage.
- **Creator view** — response uses the existing creator-view DTO (includes `creatorHash`),
  since the caller proved ownership by knowing the hash.
- **No auth header** — the hash list in the body *is* the credential, same trust model
  as `X-Creator-Hash` on the existing GET.
- **No pagination / no cap (yet)** — realistic GM hash counts are small (tens at most).
  Revisit if abuse appears.
