# 001 — Add STOMP WebSocket Test Infrastructure

## What to build
Create a reusable test helper/base class for WebSocket integration tests that sets up a STOMP client, connects to the server, and provides utility methods for subscribing to topics and sending messages.

## Acceptance Criteria
- [ ] WebSocket test helper/base class exists with connect/subscribe/send utilities
- [ ] A smoke test proves the STOMP connection works (connect + subscribe to a topic)
- [ ] Helper is reusable by future WS test classes (draw tests, correction tests, player join tests)
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `WebSocketTestHelper.java` | `src/test/java/.../integration/` | Reusable STOMP client setup: connect, subscribe, send, await messages |
| `WebSocketConnectionTest.java` | `src/test/java/.../integration/` | Smoke test: connect to /bingo-connect, subscribe to a room topic |

### Files to READ (for patterns — do NOT modify)
| File | What to check |
|------|---------------|
| `WebSocketConfig.java` | STOMP endpoint path, message broker config, app destination prefixes |
| `WebSocketController.java` | @MessageMapping paths, payload types, response destinations |

### Implementation Details

**WebSocketTestHelper** should provide:
- `connectToServer()` — creates `WebSocketStompClient` with `SockJsClient`, connects to `ws://localhost:{port}/bingo-connect`
- `subscribeTo(StompSession, String destination)` — subscribes and returns a `BlockingQueue<String>` for received messages
- `sendMessage(StompSession, String destination, Object payload)` — sends a STOMP message
- `awaitMessage(BlockingQueue, Duration timeout)` — blocks until a message arrives or times out

Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@LocalServerPort`.

### Dependencies
May need to add `spring-boot-starter-websocket` test utilities. Check if `WebSocketStompClient` and `SockJsClient` are already on the test classpath via existing dependencies.

## TDD Sequence
1. Create the helper class
2. Write the smoke test (connect + subscribe)
3. Run `mvn test`

## Done Definition
STOMP test infrastructure works. Smoke test connects and subscribes successfully. Helper is ready for use by tasks 002-005.
