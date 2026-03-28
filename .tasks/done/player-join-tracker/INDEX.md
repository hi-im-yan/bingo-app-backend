# Feature: Player Join Tracker

**Status**: done
**Blocked by feature**: —
**Branch**: feature/player-join-tracker

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | PlayerEntity + PlayerRepository | done | — | Sonnet |
| 002 | PlayerDTO + JoinRoomForm | done | — | Sonnet |
| 003 | Service methods + unit tests | done | 001, 002 | Sonnet |
| 004 | WebSocket + REST endpoints | done | 003 | Sonnet |
| 005 | Integration tests | done | 004 | Sonnet |

## Decisions
- PlayerEntity has @ManyToOne to RoomEntity (a room has many players)
- Duplicate player names in the same room are rejected (ConflictException)
- No authentication required to join — anyone with the session code can join
- Player list via REST is creator-only (requires X-Creator-Hash)
- WebSocket join broadcasts to `/room/{sessionCode}/players` sub-topic (separate from number draws)
- Only track joins, no leave/disconnect tracking
