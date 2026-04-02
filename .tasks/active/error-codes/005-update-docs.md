# 005 — Update docs/FRONTEND_API.md with New Error Shapes and Codes

## What to build
Update the frontend API documentation to reflect the new error response format with `code` field, the WebSocket error subscription, and add the `code` column to every error table in the catalog.

## Acceptance Criteria
- [ ] Error Response Format section shows new `ErrorResponse` shape with `code` field
- [ ] WebSocket error subscription (`/user/queue/errors`) documented
- [ ] Every error table in the Error Catalog has a `Code` column
- [ ] VALIDATION_ERROR `fields` array documented with example
- [ ] Data Model Reference includes `ErrorResponse` and `FieldError` TypeScript interfaces
- [ ] Quick Integration Checklist updated to mention error subscription

## Technical Spec

### Files to MODIFY

| File | Change |
|------|--------|
| `docs/FRONTEND_API.md` | Update error sections, add code column, document WS errors |

### Files to READ

| File | Why |
|------|-----|
| `docs/FRONTEND_API.md` | Current content to modify |
| `src/main/java/com/yanajiki/application/bingoapp/exception/ErrorCode.java` | Exact enum values |

### Implementation Details

**1. Update Error Response Format section:**

Replace the current REST error shape with:
```json
{
  "status": 409,
  "code": "ROOM_NAME_TAKEN",
  "message": "Room already exists."
}
```

Add VALIDATION_ERROR example:
```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "name: must not be blank; description: size must be between 0 and 255",
  "fields": [
    { "field": "name", "code": "NOT_BLANK" },
    { "field": "description", "code": "SIZE" }
  ]
}
```

**2. Add WebSocket Error Subscription section** (after existing Subscribe sections):

```
### Subscribe — Errors (Personal Queue)

Destination: /user/queue/errors

Receives error responses when a WebSocket send fails. This is a user-specific queue — each client only receives their own errors.

{
  "status": 400,
  "code": "NUMBER_ALREADY_DRAWN",
  "message": "Number 42 has already been drawn in this room"
}
```

**3. Add `Code` column to every error table in the Error Catalog:**

For REST tables: add `Code` column between `Status` and `Message`.
For WebSocket tables: add `Code` column before `Message`.

Use exact codes from ErrorCode enum:
- `ROOM_NOT_FOUND`, `ROOM_NAME_TAKEN`, `VALIDATION_ERROR`
- `PLAYER_NAME_TAKEN`
- `DRAW_MODE_MISMATCH`, `NUMBER_OUT_OF_RANGE`, `NUMBER_ALREADY_DRAWN`
- `ALL_NUMBERS_DRAWN`, `NO_NUMBERS_DRAWN`
- `TIEBREAK_INVALID_PLAYER_COUNT`, `TIEBREAK_NOT_ENOUGH_NUMBERS`, `TIEBREAK_ALREADY_ACTIVE`
- `TIEBREAK_NOT_ACTIVE`, `TIEBREAK_INVALID_SLOT`, `TIEBREAK_SLOT_ALREADY_DRAWN`, `TIEBREAK_NO_NUMBERS_REMAINING`
- `INTERNAL_ERROR`

**4. Update Data Model Reference** — add TypeScript interfaces:

```typescript
interface ErrorResponse {
  status: number;
  code: string;               // SCREAMING_SNAKE_CASE error code
  message: string;            // human-readable, for debugging
  fields?: FieldError[];      // only present for VALIDATION_ERROR
}

interface FieldError {
  field: string;              // form field name
  code: string;               // e.g. "NOT_BLANK", "SIZE", "MIN", "MAX"
}
```

**5. Update Quick Integration Checklist** — add item about subscribing to `/user/queue/errors`.

**6. Add a summary table of all error codes** right after the Error Response Format section:

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `ROOM_NOT_FOUND` | 404 | Room doesn't exist or invalid creator hash |
| `ROOM_NAME_TAKEN` | 409 | Room name already in use |
| `PLAYER_NAME_TAKEN` | 409 | Player name already taken in room |
| `DRAW_MODE_MISMATCH` | 400 | Operation not allowed for this room's draw mode |
| `NUMBER_OUT_OF_RANGE` | 400 | Number outside valid range (1-75) |
| `NUMBER_ALREADY_DRAWN` | 400 | Number was already drawn |
| `ALL_NUMBERS_DRAWN` | 400 | All 75 numbers have been drawn |
| `NO_NUMBERS_DRAWN` | 400 | No numbers drawn yet (can't correct) |
| `TIEBREAK_INVALID_PLAYER_COUNT` | 400 | Player count below minimum (2) |
| `TIEBREAK_NOT_ENOUGH_NUMBERS` | 400 | More players than available numbers |
| `TIEBREAK_ALREADY_ACTIVE` | 400 | Tiebreaker already running |
| `TIEBREAK_NOT_ACTIVE` | 400 | No active tiebreaker |
| `TIEBREAK_INVALID_SLOT` | 400 | Slot number out of range |
| `TIEBREAK_SLOT_ALREADY_DRAWN` | 400 | Slot already has a drawn number |
| `TIEBREAK_NO_NUMBERS_REMAINING` | 400 | No undrawn numbers left for tiebreaker |
| `VALIDATION_ERROR` | 400 | Form field validation failure |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

### Conventions
- Follow existing FRONTEND_API.md markdown style
- Keep tables aligned
- TypeScript interfaces for data models

## TDD Sequence
1. Read current docs/FRONTEND_API.md
2. Apply all changes
3. Review for consistency

## Done Definition
Documentation accurately reflects the new error response format, all codes documented, WebSocket error subscription documented.
