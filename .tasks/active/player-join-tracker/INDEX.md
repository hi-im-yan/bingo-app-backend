# Feature: Player Join Tracker

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/player-join-tracker

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | PlayerEntity + PlayerRepository | ready | — | Sonnet |
| 002 | PlayerDTO + JoinRoomForm | ready | — | Sonnet |
| 003 | Service methods + unit tests | blocked | 001, 002 | Sonnet |
| 004 | WebSocket + REST endpoints | blocked | 003 | Sonnet |
| 005 | Integration tests | blocked | 004 | Sonnet |

## Decisions
- PlayerEntity has @ManyToOne to RoomEntity (a room has many players)
- Duplicate player names in the same room are rejected (ConflictException)
- No authentication required to join — anyone with the session code can join
- Player list via REST is creator-only (requires X-Creator-Hash)
- WebSocket join broadcasts to `/room/{sessionCode}/players` sub-topic (separate from number draws)
- Only track joins, no leave/disconnect tracking
