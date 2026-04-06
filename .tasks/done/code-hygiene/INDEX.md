# Feature: Code Hygiene Cleanup

**Status**: ready
**Blocked by feature**: —
**Branch**: chore/code-hygiene

## Tasks

| ID | Task | Status | Blocked By | Assignee |
|----|------|--------|------------|----------|
| 001 | Remove @Temporal annotations from RoomEntity | ready | — | — |
| 002 | Remove @ResponseStatus from custom exceptions | ready | — | — |
| 003 | Trim trivial AI-generated Javadoc | ready | — | — |
| 004 | Upgrade CrudRepository to JpaRepository | ready | — | — |
| 005 | Reuse static SecureRandom in selectRandomNumber | ready | — | — |

## Decisions
- @Temporal is unnecessary in Hibernate 6+ (Spring Boot 3.x) — safe to remove
- @ResponseStatus on ConflictException and RoomNotFoundException is dead code since GlobalExceptionHandler handles them explicitly
- Keep Javadoc only on complex methods; remove from trivial one-liners, getters, and self-explanatory factory methods
- JpaRepository extends CrudRepository — drop-in replacement that adds pagination and flush control
- selectRandomNumber() should use RoomEntity's existing static SecureRandom instead of creating new instances
- All tasks are independent — can be done in any order or in parallel
