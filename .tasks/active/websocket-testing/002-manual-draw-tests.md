# 002 — Integration Tests for Manual Draw (add-number) Endpoint

## What to build
Integration tests that verify the full STOMP flow for manual number drawing: send a draw request via `/app/add-number`, receive the updated RoomDTO broadcast on `/room/{sessionCode}`.

## Acceptance Criteria
- [ ] Test: valid draw request broadcasts updated room with the drawn number
- [ ] Test: draw with invalid creatorHash is rejected
- [ ] Test: draw on AUTOMATIC mode room is rejected
- [ ] Test: draw duplicate number is rejected
- [ ] Tests use WebSocketTestHelper from task 001
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `ManualDrawWebSocketTest.java` | `src/test/java/.../integration/` | STOMP integration tests for /app/add-number |

### Files to READ (for context — do NOT modify)
| File | What to check |
|------|---------------|
| `WebSocketController.java` | @MessageMapping("/add-number") method signature, payload expectations |
| `AddNumberForm.java` | Required fields, validation annotations |
| `RoomService.drawNumber()` | Business rules enforced (mode check, range, duplicates) |

### Implementation Details

Each test should:
1. Create a room via REST (POST /api/v1/room with drawMode=MANUAL)
2. Connect STOMP client, subscribe to `/room/{sessionCode}`
3. Send message to `/app/add-number` with AddNumberForm payload
4. Assert received broadcast contains expected drawn number (or error for negative cases)

## TDD Sequence
1. Write tests first — they define the expected WS contract
2. Tests should pass against current code (happy path) or reveal issues (error handling)
3. Run `mvn test`

## Done Definition
Manual draw WebSocket flow is tested end-to-end. Happy path and error cases covered.
