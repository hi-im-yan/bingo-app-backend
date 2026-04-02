# Feature: Error Codes for i18n

**Status**: ready
**Branch**: feature/error-codes

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Foundation — ErrorCode enum, ErrorResponse, BingoException hierarchy | ready | — | Sonnet |
| 002 | Update services to throw BingoException subtypes with ErrorCode | ready | 001 | Sonnet |
| 003 | Update GlobalExceptionHandler + create WebSocketErrorHandler | ready | 001 | Sonnet |
| 004 | Update all tests for error code assertions | ready | 002, 003 | Sonnet |
| 005 | Update docs/FRONTEND_API.md with new error shapes and codes | ready | 001 | Sonnet |

## Decisions
- New `BingoException` base class carries `ErrorCode` — all custom exceptions extend it
- `BadRequestException` replaces `IllegalArgumentException`/`IllegalStateException` throws in services
- `ErrorResponse` record replaces `ApiResponse` for errors only — success responses unchanged
- WebSocket errors return structured JSON body via `@MessageExceptionHandler`
- `VALIDATION_ERROR` includes a `fields` array with per-field codes
