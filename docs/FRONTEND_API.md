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

Receives a `RoomDTO` (player view) every time a number is drawn. Same JSON shape as the GET response without `creatorHash`.

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

### ApiResponse (Error)

```typescript
interface ApiResponse {
  status: number;
  message: string;
}
```

---

## Quick Integration Checklist

1. **Create room** → store `creatorHash` in localStorage, `sessionCode` for sharing
2. **Share room** → give players the `sessionCode` (or QR code URL)
3. **Players join** → `GET /api/v1/room/{sessionCode}` (no auth)
4. **Connect WebSocket** → SockJS to `/bingo-connect`, subscribe to `/room/{sessionCode}`
5. **Draw numbers** → creator sends to `/app/add-number` (manual) or `/app/draw-number` (auto)
6. **All clients** receive real-time updates via the subscription
7. **Delete room** → `DELETE` with `X-Creator-Hash` when done
