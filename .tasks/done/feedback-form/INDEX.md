# Feature: Feedback Form Endpoint

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/feedback-form

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Entity + Repository + entity test | done | — | Implementer |
| 002 | FeedbackForm DTO (no custom validator needed) | done | — | Implementer |
| 003 | FeedbackMessageDTO response record | done | 001 | Implementer |
| 004 | DiscordConfig + DiscordNotifier + notifier test | done | — | Implementer |
| 005 | FeedbackService + service test | done | 001, 002, 003, 004 | Implementer |
| 006 | FeedbackController + integration test | done | 005 | Orchestrator |
| 007 | @EnableAsync + properties wiring + infra env var | done | — | Implementer |

## Decisions
- RestClient over WebClient/RestTemplate — already on classpath, no new dependency
- @Async fire-and-forget for Discord — failure must not block user response
- No custom validator — email and phone are both fully optional (anonymous submissions allowed)
- No new exception class — validation errors go through existing VALIDATION_ERROR path
- Empty webhook URL guard instead of @ConditionalOnProperty — simpler, dev/test naturally skips
