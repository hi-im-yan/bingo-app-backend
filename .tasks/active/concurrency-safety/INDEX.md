# Feature: Concurrency Safety

**Status**: ready
**Blocked by feature**: —
**Branch**: bugfix/concurrency-safety

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Add @Transactional to compound read-write service methods | ready | — | — |
| 002 | Handle DataIntegrityViolationException for room name uniqueness | ready | — | — |
| 003 | Integration tests for concurrent draw and duplicate room name | blocked | 001, 002 | — |

## Decisions
- @Transactional on all service methods that do read-modify-save cycles (drawNumber, drawRandomNumber, and any future compound methods)
- DataIntegrityViolationException from unique constraint on room name should map to 409 Conflict, not bubble up as 500
- Handle via GlobalExceptionHandler, not try-catch in service — keeps service clean
- Integration test should prove two concurrent draws cannot produce the same number
