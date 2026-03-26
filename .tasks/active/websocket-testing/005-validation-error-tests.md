# 005 — Integration Tests for WS Validation Error Handling

## What to build
Integration tests that verify invalid WebSocket payloads (missing fields, out-of-range numbers) are caught by the manual Bean Validation added in task 004 and return clean error responses.

## Acceptance Criteria
- [ ] Test: blank sessionCode in AddNumberForm returns validation error
- [ ] Test: number outside [1-75] in AddNumberForm returns validation error
- [ ] Test: blank creatorHash in DrawNumberForm returns validation error
- [ ] Tests verify error is sent to error channel, NOT broadcast to room topic
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `WebSocketValidationTest.java` | `src/test/java/.../integration/` | Tests for WS validation error handling |

### Implementation Details

Each test should:
1. Create a room via REST
2. Connect STOMP client, subscribe to both `/room/{sessionCode}` and the error channel
3. Send invalid payload to `/app/add-number` or `/app/draw-number`
4. Assert: error channel receives validation error, room topic does NOT receive a broadcast

## TDD Sequence
1. Write tests — they should fail without task 004's validation
2. After task 004, tests pass
3. Run `mvn test`

## Done Definition
Validation errors on WS endpoints are tested. Error delivery works correctly.
