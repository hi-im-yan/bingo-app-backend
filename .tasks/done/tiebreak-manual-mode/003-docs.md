# 003 — Docs: Reflect tiebreaker available in all draw modes

## What to build
Update documentation so it no longer claims the tiebreaker is AUTOMATIC-only.

Repos: `/home/yanaj/projects/bingoapp` (backend) and `/home/yanaj/projects/bingo-app-frontend` (frontend)

## Acceptance Criteria
- [ ] Backend `CLAUDE.md` tiebreak design note no longer says "AUTOMATIC rooms only".
- [ ] Backend `CLAUDE.md` endpoint table rows for `start-tiebreak` / `tiebreak-draw` no longer say "AUTOMATIC only".
- [ ] Frontend `docs/FRONTEND_API.md` updated if it states the tiebreaker is automatic-only.
- [ ] No other doc claims the restriction.

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `/home/yanaj/projects/bingoapp/CLAUDE.md` | TiebreakService design bullet + endpoint table notes |
| `/home/yanaj/projects/bingo-app-frontend/docs/FRONTEND_API.md` | Tiebreak section, if it mentions automatic-only |

### Implementation Details

**Backend `CLAUDE.md`:**
- In the Design Conventions section, the TiebreakService bullet ends with "AUTOMATIC rooms only."
  Remove/replace that clause — tiebreakers now work in both MANUAL and AUTOMATIC rooms.
- In the API Endpoints table, the `start-tiebreak` and `tiebreak-draw` rows have descriptions
  ending "(AUTOMATIC only)". Remove the "AUTOMATIC only" qualifier from both.

**Frontend `docs/FRONTEND_API.md`:**
- Search for "tiebreak" / "automatic". If the tiebreak WS destinations or notes state the
  feature is automatic-only, update to reflect availability in both modes. If no such claim
  exists, leave the file unchanged and note that in the report.

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| existing doc sections | Match surrounding tone/format; minimal edits only |

### Conventions
- Don't over-document — only correct the now-false statements. No new sections unless needed.

## TDD Sequence
N/A (docs only). Verify by grepping for "AUTOMATIC only" / "automatic draw mode rooms" /
"automatic" near tiebreak text and confirming no stale claim remains.

## Done Definition
All acceptance criteria checked. No remaining doc text restricting tiebreak to AUTOMATIC.
