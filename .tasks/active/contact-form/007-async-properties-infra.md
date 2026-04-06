# 007 — @EnableAsync + Properties Wiring + Infra Env Var

## What to build
Enable Spring async support, wire the Discord webhook URL property across all profiles, and add the env var to the VPS infra repo.

## Acceptance Criteria
- [ ] `Application.java` has `@EnableAsync` annotation
- [ ] `application.properties` has `app.discord.webhook-url=` (blank default)
- [ ] `application-prod.properties` has `app.discord.webhook-url=${DISCORD_WEBHOOK_URL}`
- [ ] `vps-infra/docker-compose.yml` passes `DISCORD_WEBHOOK_URL` to bingo-backend
- [ ] `vps-infra/env.template` documents `DISCORD_WEBHOOK_URL`
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `src/main/java/.../Application.java` | Add `@EnableAsync` annotation |
| `src/main/resources/application.properties` | Add `app.discord.webhook-url=` |
| `src/main/resources/application-prod.properties` | Add `app.discord.webhook-url=${DISCORD_WEBHOOK_URL}` |

### Files to MODIFY (vps-infra repo)
| File | Change |
|------|--------|
| `/home/yanaj/projects/vps-infra/docker-compose.yml` | Add `- DISCORD_WEBHOOK_URL=${DISCORD_WEBHOOK_URL}` to bingo-backend environment |
| `/home/yanaj/projects/vps-infra/env.template` | Add `DISCORD_WEBHOOK_URL=` under Backend section |

### Implementation Details

**Application.java — add one annotation:**
```java
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class Application { ... }
```

**application.properties — append:**
```properties
# Discord webhook (blank = disabled)
app.discord.webhook-url=
```

**application-prod.properties — append:**
```properties
# Discord webhook
app.discord.webhook-url=${DISCORD_WEBHOOK_URL}
```

**vps-infra docker-compose.yml — add to bingo-backend environment:**
```yaml
      - DISCORD_WEBHOOK_URL=${DISCORD_WEBHOOK_URL}
```

**vps-infra env.template — add under Backend section:**
```
DISCORD_WEBHOOK_URL=https://discordapp.com/api/webhooks/your-webhook-url
```

### Conventions (from project CLAUDE.md)
- Tabs for indentation
- Properties: env var substitution `${VAR}` with blank default for dev/test

## TDD Sequence
1. Apply all changes
2. Run `mvn test` — all tests must pass (async is transparent to existing tests)

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
