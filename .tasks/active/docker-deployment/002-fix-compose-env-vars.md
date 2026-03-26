# 002 — Fix docker-compose-app-bd.yml Env Var Mismatch

## What to build
Align the environment variables in `docker-compose-app-bd.yml` with what `application-prod.properties` actually expects, and ensure the prod profile is activated.

## Acceptance Criteria
- [ ] docker-compose-app-bd.yml uses DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
- [ ] SPRING_PROFILES_ACTIVE=prod is set in the compose file
- [ ] App service connects to DB successfully when run with `docker-compose -f docker-compose-app-bd.yml up`
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `docker-compose-app-bd.yml` | Replace DB_URL/DB_USERNAME/DB_PASSWORD with DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD; add SPRING_PROFILES_ACTIVE=prod |

### Files to READ (for reference — do NOT modify)
| File | What to check |
|------|---------------|
| `application-prod.properties` | Expected env var names and defaults |

### Implementation Details

Replace the app service's environment section:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
  DB_HOST: db
  DB_PORT: 5432
  DB_NAME: bingo
  DB_USER: postgres
  DB_PASSWORD: postgres
```

The prod profile datasource URL template in application-prod.properties is:
`jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:bingo}`

## Done Definition
`docker-compose -f docker-compose-app-bd.yml up` starts app with prod profile, connects to DB. No env var mismatch.
