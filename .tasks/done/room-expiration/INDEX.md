# Feature: Room Expiration

**Status**: done
**Blocked by feature**: —
**Branch**: feature/room-expiration

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Migrate timestamps from LocalDateTime to Instant | done | — | sonnet |
| 002 | Add repository query for expired rooms | done | 001 | sonnet |
| 003 | RoomCleanupScheduler + @EnableScheduling | done | 002 | sonnet |
| 004 | Integration test for room expiration | done | 003 | sonnet |

## Decisions
- Expiration based on `updateDateTime` (updated by Hibernate `@UpdateTimestamp` on every save, i.e. every draw)
- Rooms with no draws expire 24h after creation (updateDateTime == createDateTime initially)
- TTL (24h) and scheduler interval (1h) configurable via `application.properties`
- Use `Instant` for all timestamps, stored as UTC
- Find-then-delete approach so scheduler can log what it's cleaning up
- No new API endpoints — cleanup is fully internal
- `@UpdateTimestamp` fires on any entity save, not just draws — acceptable since any mutation counts as activity
