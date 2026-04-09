# Bingo Room Management API

Real-time bingo room backend powering [gritabingo.com.br/pt](https://gritabingo.com.br/pt).

REST API for room lifecycle and player management, plus STOMP WebSocket for live number drawing, player joins, and tiebreakers.

## Features

- Create/delete bingo rooms with `MANUAL` or `AUTOMATIC` draw modes
- Privileged creator identity (`creatorHash`) vs. public `sessionCode` for players
- Bulk room recovery by creator hash list
- Real-time number draws broadcast over STOMP WebSocket
- Player join flow with live roster updates
- Sequential tiebreakers with ephemeral draw pool (AUTOMATIC rooms)
- Feedback submission with async Discord webhook notification
- Scheduled cleanup of stale rooms (configurable TTL)
- 75-ball bingo out of the box, extensible via `NumberLabelMapper`

## Stack

Java 21 · Spring Boot 3.4.4 · Maven · Lombok · Spring WebSocket (STOMP) · SpringDoc OpenAPI · H2 (dev/test) · PostgreSQL (prod) · JUnit 5 + Mockito · RestAssured · JaCoCo

## Getting Started

Requires Java 21 and Maven.

```bash
mvn spring-boot:run                              # run locally (dev profile, H2)
mvn clean test                                   # run all tests
mvn clean package                                # build JAR
docker-compose up                                # Postgres only
docker-compose -f docker-compose-app-bd.yml up   # app + Postgres
```

Dev profile exposes the H2 console at `/h2-console` and OpenAPI docs at `/swagger-ui.html`.

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/room` | Create room (optional `drawMode`, defaults `MANUAL`) |
| POST | `/api/v1/room/lookup` | Bulk lookup rooms by `creatorHash` list |
| GET | `/api/v1/room/{session-code}` | Get room (view depends on `X-Creator-Hash`) |
| DELETE | `/api/v1/room/{session-code}` | Delete room (requires `X-Creator-Hash`) |
| GET | `/api/v1/room/{session-code}/players` | List players (creator only) |
| POST | `/api/v1/feedback` | Submit feedback message |

WebSocket endpoint: `/bingo-connect`

| Destination | Purpose |
|-------------|---------|
| `/app/add-number` | Manual draw (MANUAL rooms) |
| `/app/draw-number` | Automatic draw (AUTOMATIC rooms) |
| `/app/join-room` | Player joins a room |
| `/app/start-tiebreak` | Start a tiebreaker (AUTOMATIC only) |
| `/app/tiebreak-draw` | Draw a tiebreaker slot (AUTOMATIC only) |
| `/room/{sessionCode}` | Broadcast: room state updates |
| `/room/{sessionCode}/players` | Broadcast: player joins |
| `/room/{sessionCode}/tiebreak` | Broadcast: tiebreak state |
| `/user/queue/errors` | Per-client WS error channel |

Full contract: [`docs/FRONTEND_API.md`](docs/FRONTEND_API.md).

## Architecture

Layered: `controller → service → repository`. Controllers are thin; all business logic lives in `RoomService`. Domain errors go through a `BingoException` hierarchy with typed `ErrorCode`s, handled globally for both REST (`GlobalExceptionHandler`) and WebSocket (`WebSocketErrorHandler`).

```
com.yanajiki.application.bingoapp
├── api/         # Controllers, forms, responses
├── service/     # Business logic
├── database/    # JPA entities + repositories
├── websocket/   # STOMP controller + config
├── exception/   # Domain exceptions + handlers
├── game/        # NumberLabelMapper, DrawMode
└── config/      # CORS, OpenAPI
```

## Profiles

- **dev** (default): H2 in-memory, permissive CORS, H2 console enabled
- **prod**: PostgreSQL (`DB_HOST`/`PORT`/`NAME`/`USER`/`PASSWORD`), strict CORS, `ddl-auto=update`

## License

Private project. All rights reserved.
