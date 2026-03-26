# 004 — Add Manual Bean Validation to WebSocket Controller

## What to build
STOMP `@MessageMapping` methods do NOT automatically trigger Jakarta Bean Validation. The `@NotBlank`/`@Min`/`@Max` annotations on form classes are currently decorative. Add explicit validation in the WebSocket controller.

## Acceptance Criteria
- [ ] WebSocket controller validates incoming forms before delegating to service
- [ ] Validation errors send an error response back to the sender (not broadcast to room)
- [ ] Existing service-level validation still works as a second line of defense
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `WebSocketController.java` | Inject `Validator`, call `validator.validate()` on incoming forms, handle `ConstraintViolation` set |

### Files to READ (for context)
| File | What to check |
|------|---------------|
| `AddNumberForm.java` | Existing validation annotations |
| `DrawNumberForm.java` | Existing validation annotations |

### Implementation Details

Option A (preferred — clean): Inject `jakarta.validation.Validator` into WebSocketController. Before calling service methods, validate the form:

```java
Set<ConstraintViolation<AddNumberForm>> violations = validator.validate(form);
if (!violations.isEmpty()) {
	String errors = violations.stream()
		.map(ConstraintViolation::getMessage)
		.collect(Collectors.joining(", "));
	messagingTemplate.convertAndSend(
		"/room/" + form.getSessionCode() + "/errors",
		new ApiResponse(errors));
	return;
}
```

Option B: Use `@Validated` + `MethodValidationPostProcessor` — but this is less predictable with STOMP.

Decide on error delivery mechanism: `/room/{sessionCode}/errors` topic vs `@MessageExceptionHandler`. The topic approach lets the sender subscribe to errors for their room.

## TDD Sequence
1. Write test (from task 005) that sends invalid payload and expects error response
2. Implement validation in controller
3. Run `mvn test`

## Done Definition
WS form validation annotations are enforced. Invalid payloads get clean error responses. No broadcast pollution.
