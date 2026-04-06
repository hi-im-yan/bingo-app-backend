# Bingo App — Frontend Integration Guide

> **Base URL (dev):** `http://localhost:8080`
> **Swagger UI:** `/swagger-ui.html` | **OpenAPI JSON:** `/api-docs`

---

## Authentication Model

Two-tier access — no login/signup:

| Role | How it works |
|------|-------------|
| **Creator** | Receives a `creatorHash` (UUID) on room creation. Must store it client-side (e.g. localStorage). Required for delete and draw operations. |
| **Player** | No auth needed. Uses `sessionCode` (6-char public code) to view rooms and subscribe to updates. |

The `X-Creator-Hash` header controls which view you get on GET requests. If present and valid, response includes `creatorHash`. Otherwise, it's omitted.

---

## Draw Modes

Rooms have one of two draw modes, set at creation. The mode determines how numbers are drawn and which WebSocket endpoint to use. **The frontend should adapt its UI based on the room's `drawMode`.**

### MANUAL Mode
The creator **picks which number** to draw. The frontend should show a number board (1–75) where the creator selects a specific number. That number is sent via `/app/add-number` with the `number` field in the payload.

**Creator UI:** Clickable number grid (1–75), already-drawn numbers disabled/highlighted.

### AUTOMATIC Mode
The **server picks a random** undrawn number. The creator just triggers a draw (e.g. a "Draw Next" button). The request goes to `/app/draw-number` with no number in the payload — the server selects one.

**Creator UI:** Single "Draw Next Number" button. No number selection needed.

### Both Modes
- Players see the same view regardless of mode — a live-updating board of drawn numbers.
- The mode is fixed at room creation and cannot be changed.
- All draws broadcast the updated room state to all subscribers via WebSocket.

---

## REST API

### Create Room

```
POST /api/v1/room
Content-Type: application/json
```

**Request:**
```json
{
  "name": "Friday Night Bingo",
  "description": "Optional description",
  "drawMode": "MANUAL"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | string | yes | 1–255 chars, unique |
| `description` | string | no | max 255 chars |
| `drawMode` | string | no | `MANUAL` (default) or `AUTOMATIC` |

**Response (200):** Creator view — includes `creatorHash`.
```json
{
  "name": "Friday Night Bingo",
  "description": "Optional description",
  "sessionCode": "A3X9K2",
  "creatorHash": "550e8400-e29b-41d4-a716-446655440000",
  "drawnNumbers": [],
  "drawnLabels": [],
  "drawMode": "MANUAL"
}
```

**Errors:** `400` validation failure | `409` name already taken

---

### Get Room

```
GET /api/v1/room/{session-code}
X-Creator-Hash: {uuid}          # optional
```

**Response (200):**
- With valid `X-Creator-Hash`: creator view (includes `creatorHash`)
- Without header: player view (`creatorHash` field omitted entirely)

```json
{
  "name": "Friday Night Bingo",
  "sessionCode": "A3X9K2",
  "drawnNumbers": [42, 7, 63],
  "drawnLabels": ["N-42", "B-7", "O-63"],
  "drawMode": "MANUAL"
}
```

**Errors:** `404` room not found

---

### Delete Room

```
DELETE /api/v1/room/{session-code}
X-Creator-Hash: {uuid}          # required
```

**Response:** `200` (no body)

**Errors:** `400` invalid/missing hash | `404` room not found

---

### List Players in Room

```
GET /api/v1/room/{session-code}/players
X-Creator-Hash: {uuid}          # required
```

**Response (200):**
```json
[
  {
    "name": "Alice",
    "joinDateTime": "2026-03-28T12:00:00"
  },
  {
    "name": "Bob",
    "joinDateTime": "2026-03-28T12:01:30"
  }
]
```

Returns an empty array if no players have joined yet.

**Errors:** `404` room not found or invalid creator hash

---

### Get Room QR Code

```
GET /api/v1/room/{session-code}/qrcode
```

**Response:** `200` with `Content-Type: image/png` — 250x250 PNG binary. No auth required.

**Errors:** `404` room not found

---

### Submit Feedback

```
POST /api/v1/feedback
Content-Type: application/json
```

**Request:**
```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "phone": "555-1234",
  "content": "Love the app!"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `name` | string | yes | max 100 chars |
| `email` | string | no | valid email format, max 254 chars |
| `phone` | string | no | max 20 chars |
| `content` | string | yes | max 2000 chars |

No authentication required. Email and phone are both optional — anonymous submissions are allowed.

**Response (200):**
```json
{
  "id": 1,
  "name": "Alice",
  "email": "alice@example.com",
  "phone": "555-1234",
  "content": "Love the app!",
  "createdAt": "2026-04-06T10:30:00Z"
}
```

Null `email`/`phone` fields are omitted from the response (not included as `null`).

**Errors:** `400` validation error (blank name/content, invalid email format)

**TypeScript interface:**
```typescript
interface FeedbackForm {
  name: string;
  email?: string;
  phone?: string;
  content: string;
}

interface FeedbackMessageDTO {
  id: number;
  name: string;
  email?: string;
  phone?: string;
  content: string;
  createdAt: string;
}
```

---

## WebSocket (STOMP over SockJS)

### Connection

```
Endpoint: /bingo-connect
Protocol: STOMP over SockJS
```

**JavaScript example (SockJS + STOMP.js):**
```javascript
const socket = new SockJS('http://localhost:8080/bingo-connect');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to room updates
  stompClient.subscribe('/room/A3X9K2', (message) => {
    const roomDTO = JSON.parse(message.body);
    // roomDTO has same shape as GET response (player view)
  });
});
```

### Subscribe — Room Updates

```
Destination: /room/{sessionCode}
```

Receives a `RoomDTO` (player view) every time a number is drawn or corrected. Same JSON shape as the GET response without `creatorHash`.

---

### Subscribe — Number Corrections

```
Destination: /room/{sessionCode}/corrections
```

Receives a `NumberCorrectionDTO` whenever the GM corrects the last drawn number. Use this to show a toast/notification to players about the correction.

```json
{
  "oldNumber": 42,
  "oldLabel": "N-42",
  "newNumber": 12,
  "newLabel": "B-12",
  "message": "GM changed N-42 to B-12"
}
```

> **Note:** The `/room/{sessionCode}` topic also receives the updated `RoomDTO` when a correction happens — so the board state stays in sync automatically. The `/corrections` topic is an *additional* notification for displaying correction alerts.

---

### Subscribe — Player Joins

```
Destination: /room/{sessionCode}/players
```

Receives a `PlayerDTO` every time a new player joins the room.

```json
{
  "name": "Alice",
  "joinDateTime": "2026-03-28T12:00:00"
}
```

---

### Subscribe — Tiebreaker Updates

```
Destination: /room/{sessionCode}/tiebreak
```

Receives a `TiebreakDTO` on every tiebreaker event (start, each slot draw, finish). Only applicable to AUTOMATIC rooms. A room can have multiple sequential tiebreakers during a game, but only one active at a time.

```json
{
  "status": "IN_PROGRESS",
  "playerCount": 3,
  "draws": [
    { "slot": 1, "number": 42, "label": "N-42" }
  ],
  "winnerSlot": null
}
```

| Status | Meaning |
|--------|---------|
| `STARTED` | Tiebreaker created, no draws yet |
| `IN_PROGRESS` | At least one slot has drawn, but not all |
| `FINISHED` | All slots drawn, `winnerSlot` is set |

---

### Subscribe — Errors (Personal Queue)

```
Destination: /user/queue/errors
```

Receives error responses when a WebSocket send fails. This is a user-specific queue — each client only receives their own errors. Subscribe to this destination immediately after connecting.

```json
{
  "status": 400,
  "code": "NUMBER_ALREADY_DRAWN",
  "message": "Number 42 has already been drawn in this room"
}
```

**JavaScript example:**
```javascript
stompClient.connect({}, () => {
  stompClient.subscribe('/user/queue/errors', (message) => {
    const error = JSON.parse(message.body);
    console.error(`[${error.code}] ${error.message}`);
    // Use error.code to distinguish error types programmatically
  });
});
```

---

### Send — Join Room

```
Destination: /app/join-room
```

**Payload:**
```json
{
  "session-code": "A3X9K2",
  "player-name": "Alice"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `session-code` | string | yes | non-blank |
| `player-name` | string | yes | non-blank, max 50 chars, unique per room |

**Result:** Broadcasts `PlayerDTO` to `/room/{sessionCode}/players`.

**Errors:** `404` room not found | `409` player name already taken in this room

---

### Send — Manual Draw (MANUAL rooms only)

```
Destination: /app/add-number
```

**Payload:**
```json
{
  "session-code": "A3X9K2",
  "creator-hash": "550e8400-e29b-41d4-a716-446655440000",
  "number": 42
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `session-code` | string | yes | non-blank |
| `creator-hash` | string | yes | must match room creator |
| `number` | integer | yes | 1–75, not already drawn |

**Result:** Broadcasts updated `RoomDTO` to `/room/{sessionCode}`.

---

### Send — Automatic Draw (AUTOMATIC rooms only)

```
Destination: /app/draw-number
```

**Payload:**
```json
{
  "session-code": "A3X9K2",
  "creator-hash": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `session-code` | string | yes | non-blank |
| `creator-hash` | string | yes | must match room creator |

**Result:** Server picks a random undrawn number and broadcasts updated `RoomDTO` to `/room/{sessionCode}`.

---

### Send — Correct Last Number (MANUAL rooms only)

```
Destination: /app/correct-number
```

**Payload:**
```json
{
  "session-code": "A3X9K2",
  "creator-hash": "550e8400-e29b-41d4-a716-446655440000",
  "new-number": 12
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `session-code` | string | yes | non-blank |
| `creator-hash` | string | yes | must match room creator |
| `new-number` | integer | yes | 1–75, not already drawn (excluding the number being replaced) |

**Result:** Replaces the last drawn number. Broadcasts:
1. Updated `RoomDTO` to `/room/{sessionCode}` (board state)
2. `NumberCorrectionDTO` to `/room/{sessionCode}/corrections` (correction notification)

**Errors (via STOMP error frame):** room not found, AUTOMATIC mode room, no numbers drawn, new number out of range, new number already drawn.

---

### Send — Start Tiebreaker (AUTOMATIC rooms only)

```
Destination: /app/start-tiebreak
```

**Payload:**
```json
{
  "session-code": "A3X9K2",
  "creator-hash": "550e8400-e29b-41d4-a716-446655440000",
  "player-count": 3
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `session-code` | string | yes | non-blank |
| `creator-hash` | string | yes | must match room creator |
| `player-count` | integer | yes | 2–6 |

**Result:** Creates a new tiebreaker session. Broadcasts `TiebreakDTO` with status `STARTED` to `/room/{sessionCode}/tiebreak`.

**Errors:** `404` room not found | `400` not automatic mode, player count out of range | `409` tiebreaker already active

---

### Send — Tiebreaker Draw (AUTOMATIC rooms only)

```
Destination: /app/tiebreak-draw
```

**Payload:**
```json
{
  "session-code": "A3X9K2",
  "creator-hash": "550e8400-e29b-41d4-a716-446655440000",
  "slot": 1
}
```

| Field | Type | Required | Constraints |
|-------|------|----------|-------------|
| `session-code` | string | yes | non-blank |
| `creator-hash` | string | yes | must match room creator |
| `slot` | integer | yes | 1-based, max = `playerCount` |

**Result:** Draws a random number from the undrawn pool (excluding other tiebreaker draws) for the given slot. Broadcasts updated `TiebreakDTO` to `/room/{sessionCode}/tiebreak`. When all slots have drawn, the tiebreaker finishes and state is auto-cleared — a new tiebreaker can then be started.

**Errors:** `404` room not found | `400` no active tiebreaker, slot out of range, slot already drawn

---

## Error Response Format

All REST errors return an `ErrorResponse`:

```json
{
  "status": 409,
  "code": "ROOM_NAME_TAKEN",
  "message": "Room already exists."
}
```

For `VALIDATION_ERROR`, the response also includes a `fields` array with per-field details:

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

WebSocket errors are delivered to the personal queue `/user/queue/errors` as an `ErrorResponse` JSON payload. Subscribe to this destination after connecting to receive errors from failed send operations.

| Status | When |
|--------|------|
| `400` | Validation failure, invalid arguments, draw mode mismatch, all numbers drawn, invalid state |
| `404` | Room not found or invalid creator hash |
| `409` | Room name or player name conflict |
| `500` | Unexpected server error |

### Error Code Summary

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `ROOM_NOT_FOUND` | 404 | Room doesn't exist or invalid creator hash |
| `ROOM_NAME_TAKEN` | 409 | Room name already in use |
| `PLAYER_NAME_TAKEN` | 409 | Player name already taken in room |
| `DRAW_MODE_MISMATCH` | 400 | Operation not allowed for this room's draw mode |
| `NUMBER_OUT_OF_RANGE` | 400 | Number outside valid range (1–75) |
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

---

## Error Catalog

Every error the backend can return, organized by endpoint. Use the **message pattern** to match errors in frontend code and show user-friendly messages.

### REST — Create Room (`POST /api/v1/room`)

| Status | Code | Message | Cause | Suggested UX |
|--------|------|---------|-------|-------------|
| 409 | `ROOM_NAME_TAKEN` | `Room already exists.` | Room with the same name already exists | "A room with this name already exists. Choose a different name." |
| 400 | `VALIDATION_ERROR` | `name: must not be blank` | Name field is empty/missing | Inline field validation — "Room name is required" |
| 400 | `VALIDATION_ERROR` | `name: size must be between 0 and 255` | Name exceeds 255 characters | Inline field validation — "Room name is too long" |
| 400 | `VALIDATION_ERROR` | `description: size must be between 0 and 255` | Description exceeds 255 characters | Inline field validation — "Description is too long" |

> **Note:** Validation errors may combine multiple fields separated by `; ` (e.g. `name: must not be blank; description: size must be between 0 and 255`).

### REST — Get Room (`GET /api/v1/room/{session-code}`)

| Status | Code | Message | Cause | Suggested UX |
|--------|------|---------|-------|-------------|
| 404 | `ROOM_NOT_FOUND` | `not found` | Session code doesn't exist, or invalid creator hash provided | "Room not found — it may have expired or the code is incorrect." |

### REST — Delete Room (`DELETE /api/v1/room/{session-code}`)

| Status | Code | Message | Cause | Suggested UX |
|--------|------|---------|-------|-------------|
| 404 | `ROOM_NOT_FOUND` | `not found` | Session code doesn't exist or creator hash doesn't match | "Room not found or you don't have permission to delete it." |

### REST — List Players (`GET /api/v1/room/{session-code}/players`)

| Status | Code | Message | Cause | Suggested UX |
|--------|------|---------|-------|-------------|
| 404 | `ROOM_NOT_FOUND` | `Room not found or invalid creator hash` | Session code doesn't exist or creator hash doesn't match | "Room not found or you don't have permission to view players." |

### REST — QR Code (`GET /api/v1/room/{session-code}/qrcode`)

| Status | Code | Message | Cause | Suggested UX |
|--------|------|---------|-------|-------------|
| 404 | `ROOM_NOT_FOUND` | `not found` | Session code doesn't exist | "Room not found." |
| 500 | `INTERNAL_ERROR` | `If the error persists, open a ticket.` | QR code generation failed internally | "Could not generate QR code. Try again later." |

### WebSocket — Join Room (`/app/join-room`)

| Code | Message | Cause | Suggested UX |
|------|---------|-------|-------------|
| `ROOM_NOT_FOUND` | `Room not found with session code: {sessionCode}` | Invalid session code | "Room not found — check the code and try again." |
| `PLAYER_NAME_TAKEN` | `Player name '{playerName}' is already taken in this room` | Duplicate player name in room | "That name is taken. Choose a different name." |
| `VALIDATION_ERROR` | `playerName: must not be blank` | Player name empty | Inline validation — "Name is required" |
| `VALIDATION_ERROR` | `playerName: size must be between 0 and 50` | Player name too long | Inline validation — "Name must be 50 characters or less" |
| `VALIDATION_ERROR` | `sessionCode: must not be blank` | Session code empty | Inline validation — "Session code is required" |

### WebSocket — Manual Draw (`/app/add-number`)

| Code | Message | Cause | Suggested UX |
|------|---------|-------|-------------|
| `ROOM_NOT_FOUND` | `Room not found.` | Invalid session code or creator hash | "Room not found." |
| `DRAW_MODE_MISMATCH` | `This room uses automatic draw mode` | Tried manual draw on an AUTOMATIC room | "This room uses automatic draws. Use the Draw Next button instead." |
| `NUMBER_OUT_OF_RANGE` | `Drawn number must be between {min} and {max}, got: {number}` | Number outside 1–75 range | "Invalid number — must be between 1 and 75." |
| `NUMBER_ALREADY_DRAWN` | `Number {number} has already been drawn in this room` | Number was already drawn | "That number has already been drawn." (disable it on the board) |

### WebSocket — Automatic Draw (`/app/draw-number`)

| Code | Message | Cause | Suggested UX |
|------|---------|-------|-------------|
| `ROOM_NOT_FOUND` | `Room not found` | Invalid session code or creator hash | "Room not found." |
| `DRAW_MODE_MISMATCH` | `This room uses manual draw mode` | Tried auto draw on a MANUAL room | "This room uses manual draws. Select a number from the board." |
| `ALL_NUMBERS_DRAWN` | `All numbers have been drawn` | No undrawn numbers remain (all 75 drawn) | "All numbers have been drawn! The game is complete." (disable draw button) |

### WebSocket — Correct Last Number (`/app/correct-number`)

| Code | Message | Cause | Suggested UX |
|------|---------|-------|-------------|
| `ROOM_NOT_FOUND` | `Room not found.` | Invalid session code or creator hash | "Room not found." |
| `DRAW_MODE_MISMATCH` | `Number correction is only available for manual draw mode rooms` | Tried correction on AUTOMATIC room | N/A — hide correction UI for automatic rooms |
| `NO_NUMBERS_DRAWN` | `No numbers have been drawn yet` | No drawn numbers to correct | "No numbers to correct yet." |
| `NUMBER_OUT_OF_RANGE` | `Drawn number must be between {min} and {max}, got: {number}` | Corrected number outside 1–75 | "Invalid number — must be between 1 and 75." |
| `NUMBER_ALREADY_DRAWN` | `Number {number} has already been drawn in this room` | Corrected number already in drawn pool | "That number has already been drawn. Pick a different one." |

### WebSocket — Start Tiebreaker (`/app/start-tiebreak`)

| Code | Message | Cause | Suggested UX |
|------|---------|-------|-------------|
| `ROOM_NOT_FOUND` | `Room not found` | Invalid session code or creator hash | "Room not found." |
| `DRAW_MODE_MISMATCH` | `Tiebreaker is only available for automatic draw mode rooms` | Tried on MANUAL room | N/A — hide tiebreaker UI for manual rooms |
| `TIEBREAK_INVALID_PLAYER_COUNT` | `Player count must be at least 2, got: {playerCount}` | Player count < 2 | "Tiebreaker needs at least 2 players." |
| `TIEBREAK_NOT_ENOUGH_NUMBERS` | `Player count ({playerCount}) exceeds available numbers ({availableNumbers})` | More players than remaining undrawn numbers | "Not enough numbers remaining for {playerCount} players." |
| `TIEBREAK_ALREADY_ACTIVE` | `Room '{sessionCode}' already has an active tiebreaker` | Tiebreaker already running | "A tiebreaker is already in progress. Finish it first." |

### WebSocket — Tiebreaker Draw (`/app/tiebreak-draw`)

| Code | Message | Cause | Suggested UX |
|------|---------|-------|-------------|
| `ROOM_NOT_FOUND` | `Room not found` | Invalid session code or creator hash | "Room not found." |
| `TIEBREAK_NOT_ACTIVE` | `No active tiebreaker for room '{sessionCode}'` | No tiebreaker started | "No tiebreaker is active. Start one first." |
| `TIEBREAK_INVALID_SLOT` | `Slot must be between 1 and {playerCount}, got: {slot}` | Invalid slot number | "Invalid player slot." |
| `TIEBREAK_SLOT_ALREADY_DRAWN` | `Slot {slot} has already drawn` | Slot already used | "This player has already drawn." (disable the slot button) |
| `TIEBREAK_NO_NUMBERS_REMAINING` | `No numbers remaining for tiebreaker draw` | Pool exhausted (edge case) | "No numbers remaining for the tiebreaker." |

### Validation Errors (All Endpoints)

Form validation errors (from `@NotBlank`, `@Size`, `@Min`, `@Max`, `@NotNull`) follow this pattern:

```
fieldName: validation message; fieldName: validation message
```

Multiple field errors are joined with `; `. Parse on `; ` to show per-field errors. Common validation messages:

| Validation | Message |
|------------|---------|
| `@NotBlank` | `must not be blank` |
| `@NotNull` | `must not be null` |
| `@Size(max=N)` | `size must be between 0 and N` |
| `@Min(N)` | `must be greater than or equal to N` |
| `@Max(N)` | `must be less than or equal to N` |

### Catch-All Error

Any unhandled server error returns:

| Status | Code | Message |
|--------|------|---------|
| 500 | `INTERNAL_ERROR` | `If the error persists, open a ticket.` |

This indicates a bug. Log the full response for debugging.

---

## Game Rules — 75-Ball Bingo

Numbers 1–75 mapped to columns:

| Letter | Range | Example |
|--------|-------|---------|
| **B** | 1–15 | `B-7` |
| **I** | 16–30 | `I-22` |
| **N** | 31–45 | `N-42` |
| **G** | 46–60 | `G-51` |
| **O** | 61–75 | `O-63` |

The `drawnLabels` array in responses uses this format (e.g. `["B-7", "N-42", "O-63"]`).

---

## CORS

| Environment | Allowed Origins |
|-------------|----------------|
| dev | `*` (all origins) |
| prod | Set via `CORS_ALLOWED_ORIGINS` env var (default: `http://localhost:3000`) |

Allowed methods: `GET`, `POST`, `DELETE`, `OPTIONS`. All headers allowed.

---

## Data Model Reference

### RoomDTO

```typescript
interface RoomDTO {
  name: string;
  description?: string;       // omitted if null
  sessionCode: string;        // 6-char public ID
  creatorHash?: string;       // UUID, omitted in player view
  drawnNumbers: number[];     // e.g. [42, 7, 63]
  drawnLabels: string[];      // e.g. ["N-42", "B-7", "O-63"]
  drawMode: "MANUAL" | "AUTOMATIC";
}
```

### CreateRoomForm

```typescript
interface CreateRoomForm {
  name: string;               // required, 1-255 chars
  description?: string;       // optional, max 255 chars
  drawMode?: "MANUAL" | "AUTOMATIC";  // defaults to MANUAL
}
```

### AddNumberForm (WebSocket)

```typescript
interface AddNumberForm {
  "session-code": string;
  "creator-hash": string;
  number: number;             // 1-75
}
```

### DrawNumberForm (WebSocket)

```typescript
interface DrawNumberForm {
  "session-code": string;
  "creator-hash": string;
}
```

### CorrectNumberForm (WebSocket)

```typescript
interface CorrectNumberForm {
  "session-code": string;
  "creator-hash": string;
  "new-number": number;         // 1-75, replacement for last drawn number
}
```

### NumberCorrectionDTO (WebSocket notification)

```typescript
interface NumberCorrectionDTO {
  oldNumber: number;
  oldLabel: string;             // e.g. "N-42"
  newNumber: number;
  newLabel: string;             // e.g. "B-12"
  message: string;              // e.g. "GM changed N-42 to B-12"
}
```

### TiebreakDTO (WebSocket notification)

```typescript
interface TiebreakDTO {
  status: "STARTED" | "IN_PROGRESS" | "FINISHED";
  playerCount: number;
  draws: TiebreakDrawEntry[];
  winnerSlot?: number;            // set when FINISHED
}

interface TiebreakDrawEntry {
  slot: number;                   // 1-based
  number: number;                 // raw drawn number
  label: string;                  // e.g. "N-42"
}
```

### StartTiebreakForm (WebSocket)

```typescript
interface StartTiebreakForm {
  "session-code": string;
  "creator-hash": string;
  "player-count": number;         // 2–6
}
```

### TiebreakDrawForm (WebSocket)

```typescript
interface TiebreakDrawForm {
  "session-code": string;
  "creator-hash": string;
  slot: number;                   // 1-based
}
```

### PlayerDTO

```typescript
interface PlayerDTO {
  name: string;
  joinDateTime: string;         // ISO datetime, e.g. "2026-03-28T12:00:00"
}
```

### JoinRoomForm (WebSocket)

```typescript
interface JoinRoomForm {
  "session-code": string;
  "player-name": string;        // max 50 chars, unique per room
}
```

### ErrorResponse

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

---

## Room Expiration

Rooms are **automatically deleted** after 24 hours of inactivity. Any mutation (number draw, player join, etc.) resets the timer. This is server-side only — no frontend action required.

**What this means for the frontend:**
- Long-idle rooms will disappear. If a `GET /api/v1/room/{sessionCode}` returns `404`, the room may have expired.
- Consider showing a "room not found — it may have expired" message instead of a generic 404.
- No keepalive mechanism is needed — normal game activity (draws, joins) resets the expiration automatically.

---

## Quick Integration Checklist

1. **Create room** → store `creatorHash` in localStorage, `sessionCode` for sharing
2. **Share room** → give players the `sessionCode` (or QR code URL)
3. **Players join** → `GET /api/v1/room/{sessionCode}` (no auth), then send `/app/join-room` via WS
4. **Connect WebSocket** → SockJS to `/bingo-connect`, subscribe to `/room/{sessionCode}`, `/room/{sessionCode}/players`, and `/user/queue/errors`
5. **Draw numbers** → creator sends to `/app/add-number` (manual) or `/app/draw-number` (auto)
6. **Correct last number** → creator sends to `/app/correct-number` (manual rooms only)
7. **All clients** receive real-time updates via `/room/{sessionCode}` subscription
8. **Correction alerts** → optionally subscribe to `/room/{sessionCode}/corrections` for toast notifications
9. **Tiebreaker** (auto rooms) → subscribe to `/room/{sessionCode}/tiebreak`, creator sends `/app/start-tiebreak` then `/app/tiebreak-draw` per slot
10. **List players** → creator calls `GET /api/v1/room/{sessionCode}/players` with `X-Creator-Hash`
11. **Delete room** → `DELETE` with `X-Creator-Hash` when done
