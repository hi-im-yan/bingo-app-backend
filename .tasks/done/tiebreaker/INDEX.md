# Feature: Tiebreaker (Backend)

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/tiebreaker

## Description

When multiple players get BINGO simultaneously in automatic rooms, the admin starts a tiebreaker. Each contestant draws a random number from the undrawn pool — highest wins. Numbers are ephemeral (not added to drawnNumbers). In-memory state, one active tiebreaker per room.

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| B01 | TiebreakDTO + forms + in-memory state | ready | — | Logic Writer |
| B02 | TiebreakService (start, draw, resolve winner) | blocked | B01 | Logic Writer |
| B03 | WebSocketController tiebreaker endpoints | blocked | B02 | Logic Writer |
| B04 | Update FRONTEND_API.md docs | blocked | B03 | Logic Writer |

## Decisions
- In-memory `ConcurrentHashMap<String, TiebreakState>` — not persisted to DB
- Multiple tiebreakers per game (sequential), but only one active at a time per room
- Each tiebreaker has exactly one winner
- Numbers drawn from undrawn pool, also excluding other tiebreaker draws
- State auto-cleared after FINISHED status is broadcast, allowing a new tiebreaker to start
- Player count range: 2–6
- AUTOMATIC rooms only
