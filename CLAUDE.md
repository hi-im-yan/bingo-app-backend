# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Bingo room management API with real-time number drawing via WebSocket.
Rooms are created via REST, numbers are drawn and broadcast in real-time over STOMP WebSocket.

## Stack

- **Java 21**, **Spring Boot 3.4.4**, **Maven**
- **Lombok**, **PostgreSQL** (prod), **H2** (dev/test)
- **WebSocket (STOMP)** for real-time number broadcasting
- **SpringDoc OpenAPI** for API docs (Swagger UI at `/swagger-ui.html`)
- **JUnit 5 + Mockito** (unit), **RestAssured** (integration), **JaCoCo** (80% min coverage)

## Build & Test Commands

```bash
mvn clean compile              # Compile
mvn clean test                 # Run all tests (unit + integration)
mvn clean package              # Build JAR (includes tests)
mvn spring-boot:run            # Run locally (dev profile, H2 in-memory)
mvn test -Dtest=RoomServiceTest                    # Single test class
mvn test -Dtest=RoomServiceTest#shouldCreateRoom   # Single test method
```

If `mvn` is not in PATH, install via SDKMAN (`sdk install maven`). Same for JDK 21.

## Docker

```bash
docker-compose up                                    # Postgres only (dev)
docker-compose -f docker-compose-app-bd.yml up       # App + Postgres
```

## Architecture

Layered: `controller -> service -> repository`

```
com.yanajiki.application.bingoapp/
  api/            # REST controllers, forms (CreateRoomForm), responses (RoomDTO, ApiResponse)
  service/        # Business logic (RoomService) — all logic lives here
  database/       # JPA entity (RoomEntity) + Spring Data repository
  exception/      # Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
  websocket/      # STOMP WebSocket controller + config
  config/         # CORS config, OpenAPI config
  game/           # NumberLabelMapper interface + StandardBingoMapper (75-ball bingo rules) + DrawMode enum
```

### Key Design Decisions

- **Controllers are thin** — they delegate everything to `RoomService`. No business logic in controllers.
- **Game abstraction**: `NumberLabelMapper` interface defines valid number ranges and label format (B/I/N/G/O columns). `StandardBingoMapper` implements 75-ball bingo. This allows future variants (e.g., 90-ball).
- **Drawn numbers** stored via `@ElementCollection` (separate `room_drawn_numbers` table), not CSV.
- **Creator identity**: `creatorHash` (UUID) passed in `X-Creator-Hash` header for privileged operations (draw, delete). `sessionCode` (6-char alphanumeric via `SecureRandom`) is the public room identifier.
- **Two DTO views**: `RoomDTO.fromEntityToCreator()` includes creatorHash; `RoomDTO.fromEntityToPlayer()` hides it via `@JsonInclude(NON_NULL)`.
- **Draw modes**: `DrawMode` enum (`MANUAL`, `AUTOMATIC`) stored per room. Manual mode: creator picks exact number. Automatic mode: server picks random number from remaining pool. Mode is enforced — each room only accepts its designated draw endpoint.

### API Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/room` | Create room (optional `drawMode`: `MANUAL`/`AUTOMATIC`, defaults `MANUAL`) | None |
| GET | `/api/v1/room/{session-code}` | Get room | `X-Creator-Hash` header (optional, determines view) |
| DELETE | `/api/v1/room/{session-code}` | Delete room | `X-Creator-Hash` header (required) |
| WS | `/bingo-connect` → `/app/add-number` | Manual draw: creator picks number (MANUAL rooms only) | creatorHash in payload |
| WS | `/bingo-connect` → `/app/draw-number` | Automatic draw: server picks random number (AUTOMATIC rooms only) | creatorHash in payload |

Both WS draw endpoints broadcast the updated `RoomDTO` (player view) to `/room/{sessionCode}`.

### Exception Handling

Centralized in `GlobalExceptionHandler`:
- `ConflictException` -> 409 (duplicate room name)
- `RoomNotFoundException` -> 404
- `MethodArgumentNotValidException` -> 400 (validation errors)
- `IllegalArgumentException` -> 400 (business rule violation, including draw mode mismatch)
- `IllegalStateException` -> 400 (e.g., all numbers already drawn)
- Generic `Exception` -> 500 (logged at ERROR)

## Spring Profiles

- **dev** (default): H2 in-memory, permissive CORS (`*`), H2 console at `/h2-console`
- **prod**: PostgreSQL (env vars: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`), strict CORS, `ddl-auto=validate`

## Testing Patterns

- **Unit tests** (`RoomServiceTest`): Mockito mocks for repository/mapper, `@Nested` + `@DisplayName` organization
- **Integration tests** (`RoomControllerIntegrationTest`): RestAssured on random port with H2, BDD given/when/then style
- **Entity tests** (`RoomEntityTest`): Factory method and append behavior
- Tests use dev profile (H2) by default

## Git Workflow

- Branch `v2` for current development; merge to `main` when validated
- Conventional commits format
- Feature sub-branches off `v2` if needed

## CI/CD

GitHub Actions workflow triggered on PR to `develop` or `main`:
1. Test (`mvn clean test` on JDK 21)
2. Build artifact (`mvn clean package`)
3. Docker build + push

## Team Structure (Standard)

| Role | Model | Tools & Skills | Responsibility |
|------|-------|----------------|---------------|
| Orchestrator | Sonnet | /new-feature, /status, gh CLI, TaskCreate/TaskUpdate | Manages issues, delegates, reviews output, git workflow |
| Implementer | Sonnet | Read, Edit, Write, Grep, Glob, Bash, /review (self-review) | Writes tests and implementation (TDD in single agent). Follow conventions above |
| Explorer | Haiku | Read, Glob, Grep | Quick codebase searches and lookups. No editing |
