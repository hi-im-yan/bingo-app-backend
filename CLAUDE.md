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
  database/   # JPA entity (RoomEntity) + repository
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

## API Endpoints
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /api/v1/room | Create room (optional drawMode, defaults MANUAL) | None |
| GET | /api/v1/room/{session-code} | Get room | X-Creator-Hash (optional, determines view) |
| DELETE | /api/v1/room/{session-code} | Delete room | X-Creator-Hash (required) |
| WS | /bingo-connect → /app/add-number | Manual draw (MANUAL rooms only) | creatorHash in payload |
| WS | /bingo-connect → /app/draw-number | Automatic draw (AUTOMATIC rooms only) | creatorHash in payload |

Both WS endpoints broadcast updated RoomDTO (player view) to /room/{sessionCode}.

## Exception Pattern
GlobalExceptionHandler maps: ConflictException→409, RoomNotFoundException→404,
MethodArgumentNotValidException→400, IllegalArgumentException→400 (incl. draw mode mismatch),
IllegalStateException→400 (e.g. all numbers drawn), generic Exception→500 (logged ERROR).
Follow this pattern when adding new exceptions.

## Testing Conventions
- Unit tests: Mockito mocks, @Nested + @DisplayName organization
- Integration tests: RestAssured, BDD given/when/then style
- Tests use dev profile (H2) by default

## Profiles
- dev (default): H2 in-memory, permissive CORS, /h2-console
- prod: PostgreSQL (env: DB_HOST/PORT/NAME/USER/PASSWORD), strict CORS, ddl-auto=validate

## Git & Team
Branch v2 for development. Feature sub-branches off v2 if needed. Conventional commits.
Team structure: **Standard** profile (see ~/.claude/references/team-profiles.md).
