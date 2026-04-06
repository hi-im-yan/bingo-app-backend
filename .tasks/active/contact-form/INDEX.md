# Feature: Contact Form Endpoint

**Status**: ready
**Blocked by feature**: —
**Branch**: feature/contact-form

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Entity + Repository + entity test | ready | — | — |
| 002 | ContactForm DTO (no custom validator needed) | ready | — | — |
| 003 | ContactMessageDTO response record | ready | 001 | — |
| 004 | DiscordConfig + DiscordNotifier + notifier test | ready | — | — |
| 005 | ContactService + service test | blocked | 001, 002, 003, 004 | — |
| 006 | ContactController + integration test | blocked | 005 | — |
| 007 | @EnableAsync + properties wiring + infra env var | ready | — | — |

## Decisions
- RestClient over WebClient/RestTemplate — already on classpath, no new dependency
- @Async fire-and-forget for Discord — failure must not block user response
- No custom validator — email and phone are both fully optional (anonymous submissions allowed)
- No new exception class — validation errors go through existing VALIDATION_ERROR path
- Empty webhook URL guard instead of @ConditionalOnProperty — simpler, dev/test naturally skips
