# 003 — Integration Tests for Automatic Draw (draw-number) Endpoint

## What to build
Integration tests for the automatic draw STOMP endpoint: send a request via `/app/draw-number`, verify a random number is drawn and broadcast.

## Acceptance Criteria
- [ ] Test: valid auto-draw request broadcasts updated room with a new drawn number
- [ ] Test: auto-draw with invalid creatorHash is rejected
- [ ] Test: auto-draw on MANUAL mode room is rejected
- [ ] Test: auto-draw when all 75 numbers drawn is rejected
- [ ] Tests use WebSocketTestHelper from task 001
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `AutomaticDrawWebSocketTest.java` | `src/test/java/.../integration/` | STOMP integration tests for /app/draw-number |

### Files to READ (for context — do NOT modify)
| File | What to check |
|------|---------------|
| `WebSocketController.java` | @MessageMapping("/draw-number") method signature |
| `DrawNumberForm.java` | Required fields |
| `RoomService.drawRandomNumber()` | Business rules (mode check, all-drawn state) |

### Implementation Details

Same pattern as task 002 but for automatic draws. Key difference: the drawn number is server-selected, so assert that the broadcast contains *some* valid number in range [1-75] that wasn't previously drawn, rather than a specific number.

## TDD Sequence
1. Write tests
2. Run `mvn test`

## Done Definition
Automatic draw WebSocket flow is tested end-to-end. Happy path and error cases covered.
