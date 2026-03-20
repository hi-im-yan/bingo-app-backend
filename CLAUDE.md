# Bingo App Backend — v2 Refactor

## Project Overview
Bingo room management API with real-time number drawing via WebSocket.
Spring Boot + PostgreSQL, layered architecture (controller/service/repository).

## Stack
- **Java 21**, **Spring Boot 3.4.x** (latest stable)
- **Maven**, **Lombok**, **PostgreSQL**
- **WebSocket (STOMP)** for real-time number broadcasting
- **SpringDoc OpenAPI** for API docs
- **JUnit 5 + Mockito** for unit tests, **RestAssured** for integration tests
- **JaCoCo** for coverage (minimum 80%)
- **Docker Compose** for local dev

## Architecture
Layered: `controller → service → repository`
- Controllers handle HTTP/WebSocket mapping and validation
- Services contain business logic
- Repositories handle persistence
- Global exception handler via `@RestControllerAdvice`
- Records for DTOs
- Input validation with Bean Validation annotations

## Team Structure (Standard)

| Role | Model | Responsibility |
|------|-------|---------------|
| Orchestrator | Sonnet | Manages issues, delegates, reviews output, git workflow |
| Implementer | Sonnet | Writes tests and implementation (TDD in single agent) |
| Explorer | Haiku | Quick codebase searches and lookups |

## Git Workflow
- Branch: `v2` (all refactor work here)
- Conventional commits
- Feature sub-branches off `v2` if needed, otherwise commit directly to `v2`
- Merge to `main` only after full refactor is validated

## Refactor Issues

### Issue 1: Upgrade pom.xml
- Java 17 → 21
- Spring Boot 3.2.3 → 3.4.x (latest stable)
- Remove duplicate websocket dependency
- Update commons-lang3 to latest
- Add: springdoc-openapi, jacoco-maven-plugin, restassured, h2 (test scope)
- Update Dockerfile to JDK 21

### Issue 2: Introduce service layer
- Create `RoomService` with all business logic extracted from controllers
- Controllers become thin — delegate to service
- WebSocketController delegates to service too

### Issue 3: Global exception handler
- Create `@RestControllerAdvice` class
- Handle: ConflictException (409), RoomNotFoundException (404), MethodArgumentNotValidException (400), generic Exception (500)
- Remove exception handlers from RoomController
- Add `@ResponseStatus` to custom exceptions

### Issue 4: Fix data model
- Fix RoomRepository generic type (String → Long)
- Replace drawnNumbers CSV string with `@ElementCollection` or JSON column
- Use `SecureRandom` for session code generation
- Remove dead password field
- Validate drawn numbers (1-75 range, no duplicates)

### Issue 5: Modernize DTOs and forms
- Convert ApiResponse to record
- Convert RoomDTO to record
- Add Bean Validation to CreateRoomForm (@NotBlank, @Size, etc.)
- Add Bean Validation to AddNumberForm
- Move creatorHash from URL path to request header

### Issue 6: Clean up code
- Delete RoomMapper.java (dead code)
- Replace System.out.println with SLF4J logger
- Add Javadoc to all classes and public methods
- Rename WebSocketController.greeting() to meaningful name

### Issue 7: Spring profiles and configuration
- Create application-dev.properties (H2)
- Create application-prod.properties (PostgreSQL)
- Clean up application.properties as base config
- Tighten CORS (configurable origins per profile)
- Add room TTL / expiration (optional)

### Issue 8: Docker updates
- Update Dockerfile to JDK 21
- Pin PostgreSQL image version
- Add volume persistence to dev docker-compose
- Add health checks

### Issue 9: Tests (TDD)
- Unit tests for RoomService (Mockito)
- Integration tests for RoomController (RestAssured)
- WebSocket integration tests
- Target: 80%+ coverage (JaCoCo)

### Issue 10: API documentation
- Add SpringDoc OpenAPI dependency
- Annotate controllers with @Operation, @ApiResponse, @Tag
- Swagger UI available at /swagger-ui.html
