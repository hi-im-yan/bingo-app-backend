# 003 — Documentation updates

## What to build
Update project docs to reflect the new lookup endpoint, per the Feature Closeout
Checklist in CLAUDE.md.

## Acceptance Criteria
- [ ] `CLAUDE.md` API Endpoints table includes `POST /api/v1/room/lookup`
- [ ] `docs/FRONTEND_API.md` documents the endpoint: request body, response shape,
      integration guidance (recommend pruning stale hashes from localStorage when a
      hash is missing from the response)
- [ ] `docs/openapi.json` regenerated if it exists as a static file

## Files to MODIFY
| File | Change |
|------|--------|
| `CLAUDE.md` | Add row to API Endpoints table |
| `docs/FRONTEND_API.md` | Add endpoint section with TS interface for the request/response |
| `docs/openapi.json` | Regenerate from running app (only if static spec is checked in) |

## Notes
- Frontend integration tip to include in FRONTEND_API.md: after calling `/lookup`,
  diff returned rooms against the localStorage hash list and remove hashes that
  no longer resolve — keeps storage clean as rooms expire (24h TTL).

## Done Definition
All acceptance criteria checked. Docs accurately reflect the implemented endpoint.
