# 001 — Add spring-boot-starter-actuator to Fix Docker Health Check

## What to build
Add the Spring Boot Actuator dependency so the Dockerfile's HEALTHCHECK (`/actuator/health`) actually returns 200 instead of 404.

## Acceptance Criteria
- [ ] `spring-boot-starter-actuator` added to `pom.xml`
- [ ] Only health endpoint exposed (minimize attack surface)
- [ ] Health endpoint responds 200 on `GET /actuator/health`
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `pom.xml` | Add `spring-boot-starter-actuator` dependency |
| `application-prod.properties` | Expose only health endpoint: `management.endpoints.web.exposure.include=health` |
| `application-dev.properties` | Expose health + info for dev convenience: `management.endpoints.web.exposure.include=health,info` |

### Implementation Details

**pom.xml** — add in the dependencies section (no version needed, managed by Spring Boot BOM):
```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**application-prod.properties** — restrict actuator:
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=never
```

**application-dev.properties** — slightly more permissive:
```properties
management.endpoints.web.exposure.include=health,info
```

### Verification
After adding, `mvn spring-boot:run` then `curl http://localhost:8080/actuator/health` should return `{"status":"UP"}`.

## TDD Sequence
1. Add dependency and config
2. Run `mvn test` — existing tests pass
3. Manual verification: start app, hit health endpoint

## Done Definition
`/actuator/health` returns 200. Docker health check will now succeed. Only health endpoint exposed in prod.
