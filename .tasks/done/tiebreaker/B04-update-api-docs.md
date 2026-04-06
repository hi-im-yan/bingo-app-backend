# B04 — Update FRONTEND_API.md

## What to build
Add tiebreaker endpoints documentation to `docs/FRONTEND_API.md` so the frontend has a clear contract to implement against.

## Acceptance Criteria
- [ ] WS Send `/app/start-tiebreak` documented with payload, constraints, and errors
- [ ] WS Send `/app/tiebreak-draw` documented with payload, constraints, and errors
- [ ] WS Subscribe `/room/{sessionCode}/tiebreak` documented with `TiebreakDTO` shape
- [ ] `TiebreakDTO` and `TiebreakDrawEntry` TypeScript interfaces added to Data Model Reference
- [ ] `StartTiebreakForm` and `TiebreakDrawForm` TypeScript interfaces added
- [ ] Quick Integration Checklist updated with tiebreaker step

## Technical Spec

### Files to MODIFY
| File | What to change |
|------|---------------|
| `docs/FRONTEND_API.md` | Add tiebreaker section after "Send — Correct Last Number" section |

### Content to Add

**Subscribe — Tiebreaker Updates** section:
- Destination: `/room/{sessionCode}/tiebreak`
- Receives `TiebreakDTO` on every tiebreaker event
- Document all three statuses: STARTED, IN_PROGRESS, FINISHED

**Send — Start Tiebreaker** section:
- Destination: `/app/start-tiebreak`
- Payload fields, types, constraints
- Errors: 404 room not found, 400 not automatic mode, 400 player count out of range, 409 tiebreaker already active

**Send — Tiebreaker Draw** section:
- Destination: `/app/tiebreak-draw`
- Payload fields, types, constraints
- Errors: 404 room not found, 400 no active tiebreaker, 400 slot out of range, 409 slot already drawn

**Data Model Reference** additions:
```typescript
interface TiebreakDTO {
  status: "STARTED" | "IN_PROGRESS" | "FINISHED";
  playerCount: number;
  draws: TiebreakDrawEntry[];
  winnerSlot?: number;  // set when FINISHED
}

interface TiebreakDrawEntry {
  slot: number;
  number: number;
  label: string;
}

interface StartTiebreakForm {
  "session-code": string;
  "creator-hash": string;
  "player-count": number;  // 2–6
}

interface TiebreakDrawForm {
  "session-code": string;
  "creator-hash": string;
  slot: number;  // 1-based
}
```

### Conventions
- Match existing doc structure and formatting
- Use same table styles for fields/constraints
- Include TypeScript interfaces in Data Model Reference section

## TDD Sequence
N/A — documentation only.

## Done Definition
All sections added. Doc builds/renders correctly. TypeScript interfaces match actual DTOs.
