# CLAUDE.md — Bingo Room Management API

Real-time bingo room API: REST for room CRUD, STOMP WebSocket for number drawing.

## Stack
Java 21, Spring Boot 3.4.4, Maven, Lombok, H2 (dev/test), PostgreSQL (prod),
WebSocket (STOMP), SpringDoc OpenAPI, JUnit 5 + Mockito, RestAssured, JaCoCo (80% min).

## Commands
mvn clean compile                                    # Compile
mvn clean test                                       # All tests
mvn clean package                                    # Build JAR
mvn spring-boot:run                                  # Run locally (dev profile, H2)
mvn test -Dtest=ClassName                            # Single test class
mvn test -Dtest=ClassName#methodName                 # Single test method
docker-compose up                                    # Postgres only (dev)
docker-compose -f docker-compose-app-bd.yml up       # App + Postgres

If mvn/JDK 21 not in PATH: install via SDKMAN (sdk install maven / sdk install java 21-tem).

## Architecture
Layered: controller -> service -> repository

com.yanajiki.application.bingoapp/
  api/        # Controllers, forms, responses (RoomDTO, ApiResponse)
  service/    # All business logic (RoomService)
  database/   # JPA entities (RoomEntity, PlayerEntity) + repositories
  exception/  # Custom exceptions + GlobalExceptionHandler
  websocket/  # STOMP controller + config
  config/     # CORS, OpenAPI
  game/       # NumberLabelMapper interface + DrawMode enum

## Design Conventions
- **Controllers are thin** — delegate everything to RoomService. No business logic in controllers.
- **NumberLabelMapper** interface defines valid ranges and label format. StandardBingoMapper = 75-ball. Extensible for future variants.
- **Drawn numbers** stored via @ElementCollection (separate table), not CSV.
- **creatorHash** (UUID) = privileged identity; **sessionCode** (6-char) = public room ID. Two DTO views hide creatorHash from players via @JsonInclude(NON_NULL).
- **DrawMode** (MANUAL/AUTOMATIC) is per-room and enforced at the endpoint level.
- **PlayerEntity** has @ManyToOne to RoomEntity. Unique constraint on (name, room_id). Player list is creator-only (REST), join broadcasts to `/room/{sessionCode}/players` (WS).
- **TiebreakService** holds in-memory `ConcurrentHashMap<String, TiebreakState>` for active tiebreakers. Multiple sequential tiebreakers per game, one active at a time per room. Numbers drawn from undrawn pool (ephemeral, not added to room's drawnNumbers). State auto-cleared after FINISHED. Player count minimum 2, capped by available (undrawn) numbers. AUTOMATIC rooms only.
- **FeedbackMessageEntity** stores user feedback. Name + content required, email + phone optional (anonymous submissions allowed). `DiscordNotifier` sends async fire-and-forget webhook notification on submit. Webhook URL via `app.discord.webhook-url` property (blank = disabled).
- **RoomCleanupScheduler** runs on a fixed interval (default 1h, configurable via `room.cleanup.interval-ms`). Deletes rooms whose `updateDateTime` is older than the TTL (default 24h, configurable via `room.cleanup.ttl-hours`). Any entity save (draw, player join, etc.) resets the TTL via `@UpdateTimestamp`. Timestamps use `Instant` (UTC). No API endpoints — fully internal.

## API Endpoints
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /api/v1/room | Create room (optional drawMode, defaults MANUAL) | None |
| GET | /api/v1/room/{session-code} | Get room | X-Creator-Hash (optional, determines view) |
| DELETE | /api/v1/room/{session-code} | Delete room | X-Creator-Hash (required) |
| GET | /api/v1/room/{session-code}/players | List players in room | X-Creator-Hash (required) |
| POST | /api/v1/feedback | Submit feedback message (async Discord notification) | None |
| WS | /bingo-connect → /app/add-number | Manual draw (MANUAL rooms only) | creatorHash in payload |
| WS | /bingo-connect → /app/draw-number | Automatic draw (AUTOMATIC rooms only) | creatorHash in payload |
| WS | /bingo-connect → /app/join-room | Player joins a room | sessionCode + playerName in payload |
| WS | /bingo-connect → /app/start-tiebreak | Start tiebreaker (AUTOMATIC only) | creatorHash + playerCount in payload |
| WS | /bingo-connect → /app/tiebreak-draw | Draw for tiebreaker slot (AUTOMATIC only) | creatorHash + slot in payload |

Draw WS endpoints broadcast updated RoomDTO (player view) to /room/{sessionCode}.
Join WS endpoint broadcasts PlayerDTO to /room/{sessionCode}/players.
Tiebreaker WS endpoints broadcast TiebreakDTO to /room/{sessionCode}/tiebreak.
WS errors are sent to /user/queue/errors as ErrorResponse JSON (personal queue per client).

## Exception Pattern
- **BingoException** (abstract base) carries an `ErrorCode` enum. All domain exceptions extend it.
- **BadRequestException** (400), **ConflictException** (409), **RoomNotFoundException** (404) — use these with the matching `ErrorCode`.
- **GlobalExceptionHandler** returns `ErrorResponse(status, code, message, fields?)` for REST. Handles `BingoException`, `MethodArgumentNotValidException` (with `VALIDATION_ERROR` + per-field `fields` array), `NoResourceFoundException`, and catch-all `Exception`→500.
- **WebSocketErrorHandler** (`@ControllerAdvice` + `@MessageExceptionHandler`) catches WS exceptions and sends `ErrorResponse` JSON to `/user/queue/errors` via `@SendToUser`.
- When adding new errors: create an `ErrorCode` entry, throw a `BingoException` subtype with it. Never use raw `IllegalArgumentException`/`IllegalStateException` in services.

## Testing Conventions
- Unit tests: Mockito mocks, @Nested + @DisplayName organization
- Integration tests: RestAssured, BDD given/when/then style
- Tests use dev profile (H2) by default

## Profiles
- dev (default): H2 in-memory, permissive CORS, /h2-console
- prod: PostgreSQL public schema (env: DB_HOST/PORT/NAME/USER/PASSWORD), strict CORS, ddl-auto=update

## Feature Closeout Checklist
When closing out a feature, always update ALL of these:
- `CLAUDE.md` — new endpoints, entities, architecture changes, design decisions
- `docs/FRONTEND_API.md` — REST endpoints, WS send/subscribe destinations, TypeScript interfaces, integration checklist
- `docs/openapi.json` — regenerate from running app if static spec exists

## Git & Team
Feature branches off `develop` → PR to `develop` → merge `develop` into `main`. Conventional commits.
Team structure: **Standard** profile (see ~/.claude/references/team-profiles.md).
Task management: `.tasks/` system (see ~/.claude/references/task-system.md). Planning on develop branch, execution on feature branches.
