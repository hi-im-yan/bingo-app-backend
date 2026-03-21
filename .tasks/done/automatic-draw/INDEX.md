# Feature: Automatic Draw Game Mode

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/automatic-draw

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | DrawMode enum + RoomEntity update + entity tests | done | — | Implementer |
| 002 | CreateRoomForm + RoomDTO updates + update existing tests | done | 001 | Implementer |
| 003 | RoomService.drawRandomNumber() + unit tests | done | 001 | Implementer |
| 004 | DrawNumberForm + WebSocketController + mode enforcement | done | 003 | Implementer |
| 005 | Integration tests for automatic draw flow | done | 004 | Implementer |

## Decisions
- Draw mode is enforced per room: MANUAL rooms only use `/add-number`, AUTOMATIC rooms only use `/draw-number`
- DrawMode defaults to MANUAL for backward compatibility
- Random selection uses SecureRandom, picks from remaining pool [1-75] minus drawn numbers
- Drawing stays WebSocket-only (no REST endpoint for draw)
