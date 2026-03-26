# Feature: Docker & Deployment Fixes

**Status**: ready
**Blocked by feature**: —
**Branch**: bugfix/docker-deployment

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Add spring-boot-starter-actuator to fix Docker health check | ready | — | — |
| 002 | Fix docker-compose-app-bd.yml env var mismatch with prod profile | ready | — | — |
| 003 | Remove redundant artifact workflow from CI pipeline | ready | — | — |

## Decisions
- Add actuator with only the health endpoint exposed (minimize attack surface)
- Align docker-compose-app-bd.yml to use DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD matching application-prod.properties
- Set SPRING_PROFILES_ACTIVE=prod in docker-compose-app-bd.yml so the prod profile is actually used
- Remove sub_artifact_workflow.yml and its reference from main_test_build_docker.yml — Docker workflow rebuilds from source anyway
