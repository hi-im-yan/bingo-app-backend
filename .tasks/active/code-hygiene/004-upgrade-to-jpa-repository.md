# 004 — Upgrade CrudRepository to JpaRepository

## What to build
Change `RoomRepository` from extending `CrudRepository` to `JpaRepository`. This is a drop-in replacement that adds pagination, sorting, and flush control for free.

## Acceptance Criteria
- [ ] `RoomRepository` extends `JpaRepository<RoomEntity, Long>` instead of `CrudRepository`
- [ ] Import updated
- [ ] All tests pass (`mvn test`) — JpaRepository is a superset of CrudRepository

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `RoomRepository.java` | Change `extends CrudRepository<RoomEntity, Long>` to `extends JpaRepository<RoomEntity, Long>` |

## Done Definition
Repository extends JpaRepository. Tests green. Pagination available for future use.
