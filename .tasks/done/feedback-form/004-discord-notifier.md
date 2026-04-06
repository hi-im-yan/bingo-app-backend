# 004 — DiscordConfig + DiscordNotifier Service

## What to build
A configuration bean for RestClient and an async service that sends feedback message details to a Discord webhook. Must be fire-and-forget — failures are logged but never propagate.

## Acceptance Criteria
- [ ] `DiscordConfig` provides a `RestClient` bean
- [ ] `DiscordNotifier.notify()` is annotated with `@Async`
- [ ] When `webhookUrl` is blank, the method short-circuits (no HTTP call)
- [ ] When `webhookUrl` is set, it POSTs a JSON payload to Discord
- [ ] Exceptions from HTTP call are caught and logged, never thrown
- [ ] Unit test covers: blank URL skip, valid URL call, exception swallowing
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to CREATE
| File | Package/Path | Purpose |
|------|-------------|---------|
| `DiscordConfig.java` | `com.yanajiki.application.bingoapp.config` | RestClient bean |
| `DiscordNotifier.java` | `com.yanajiki.application.bingoapp.service` | Async Discord webhook caller |
| `DiscordNotifierTest.java` | `com.yanajiki.application.bingoapp.service` (test) | Unit test |

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `config/CorsConfig.java` | @Configuration + @Value pattern |
| `service/RoomService.java` | @Service + @RequiredArgsConstructor + @Slf4j pattern |

### Implementation Details

**DiscordConfig:**
```java
@Configuration
public class DiscordConfig {

	@Bean
	public RestClient discordRestClient() {
		return RestClient.builder().build();
	}
}
```

**DiscordNotifier:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordNotifier {

	private final RestClient discordRestClient;

	@Value("${app.discord.webhook-url:}")
	private String webhookUrl;

	@Async
	public void notify(FeedbackMessageEntity message) {
		if (!StringUtils.hasText(webhookUrl)) {
			log.debug("Discord webhook URL not configured, skipping notification");
			return;
		}
		try {
			String payload = buildPayload(message);
			discordRestClient.post()
				.uri(webhookUrl)
				.contentType(MediaType.APPLICATION_JSON)
				.body(payload)
				.retrieve()
				.toBodilessEntity();
			log.info("Discord notification sent for feedback message id={}", message.getId());
		} catch (Exception ex) {
			log.warn("Failed to send Discord notification: {}", ex.getMessage());
		}
	}

	private String buildPayload(FeedbackMessageEntity message) {
		return """
			{"embeds":[{"title":"New Feedback Message","fields":[{"name":"Name","value":"%s"},{"name":"Email","value":"%s"},{"name":"Phone","value":"%s"},{"name":"Message","value":"%s"}],"color":5814783}]}
			""".formatted(
				escape(message.getName()),
				emptyIfNull(message.getEmail()),
				emptyIfNull(message.getPhone()),
				escape(message.getContent())
			);
	}

	private String escape(String s) {
		return s == null ? "" : s.replace("\"", "\\\"").replace("\n", "\\n");
	}

	private String emptyIfNull(String s) {
		return s == null ? "—" : s;
	}
}
```

**Test notes:**
- Use `ReflectionTestUtils.setField(notifier, "webhookUrl", ...)` to set the @Value field
- Mock the RestClient fluent chain: `@Mock RestClient`, `@Mock RestClient.RequestBodyUriSpec`, `@Mock RestClient.RequestBodySpec`, `@Mock RestClient.ResponseSpec`
- Chain: `when(restClient.post()).thenReturn(bodyUriSpec)`, `when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec)`, etc.

### Conventions (from project CLAUDE.md)
- `@Service`, `@RequiredArgsConstructor`, `@Slf4j`
- `@Configuration` for bean definitions
- Tabs for indentation
- SLF4J for logging

## TDD Sequence
1. Write `DiscordNotifierTest` — test blank URL skip, valid URL call, exception swallowing
2. Write `DiscordConfig` — RestClient bean
3. Write `DiscordNotifier` — make tests pass
4. Run `mvn test` — all tests must pass

## Done Definition
All acceptance criteria checked. Tests green. No compilation warnings.
