# Feature: WebSocket Testing & Validation

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/websocket-testing

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Add STOMP WebSocket test infrastructure | ready | — | — |
| 002 | Integration tests for manual draw (add-number) endpoint | blocked | 001 | — |
| 003 | Integration tests for automatic draw (draw-number) endpoint | blocked | 001 | — |
| 004 | Add manual Bean Validation to WebSocket controller | blocked | 001 | — |
| 005 | Integration tests for validation error handling on WS endpoints | blocked | 004 | — |

## Decisions
- Use spring-boot-starter-websocket test support with StompSession for integration tests
- STOMP messages do NOT trigger Bean Validation automatically — add explicit Validator.validate() call in WebSocketController before delegating to service
- On validation failure, send error frame back to sender (not broadcast) via @MessageExceptionHandler or MessagingTemplate.convertAndSendToUser
- Test infrastructure as a reusable base class/helper so future WS features (gm-number-correction, player-join-tracker) inherit it
