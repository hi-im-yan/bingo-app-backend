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

All errors return:

```json
{
  "status": 400,
  "message": "Human-readable error description"
}
```

| Status | When |
|--------|------|
| `400` | Validation failure, invalid arguments, draw mode mismatch, all numbers drawn, invalid creator hash |
| `404` | Room not found |
| `409` | Room name conflict |
| `500` | Unexpected server error |

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

### ApiResponse (Error)

```typescript
interface ApiResponse {
  status: number;
  message: string;
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
4. **Connect WebSocket** → SockJS to `/bingo-connect`, subscribe to `/room/{sessionCode}` and `/room/{sessionCode}/players`
5. **Draw numbers** → creator sends to `/app/add-number` (manual) or `/app/draw-number` (auto)
6. **Correct last number** → creator sends to `/app/correct-number` (manual rooms only)
7. **All clients** receive real-time updates via `/room/{sessionCode}` subscription
8. **Correction alerts** → optionally subscribe to `/room/{sessionCode}/corrections` for toast notifications
9. **Tiebreaker** (auto rooms) → subscribe to `/room/{sessionCode}/tiebreak`, creator sends `/app/start-tiebreak` then `/app/tiebreak-draw` per slot
10. **List players** → creator calls `GET /api/v1/room/{sessionCode}/players` with `X-Creator-Hash`
11. **Delete room** → `DELETE` with `X-Creator-Hash` when done
