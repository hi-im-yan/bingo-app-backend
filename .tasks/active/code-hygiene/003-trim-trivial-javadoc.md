# 003 — Trim Trivial AI-Generated Javadoc

## What to build
Remove Javadoc from trivial one-liner methods, getters, and self-explanatory factory methods. Keep Javadoc only on complex methods where the logic isn't self-evident from the method name.

## Acceptance Criteria
- [ ] Trivial method Javadoc removed (e.g., getters, simple delegations, obvious factory methods)
- [ ] Complex method Javadoc preserved (business logic, non-obvious algorithms, public API contracts)
- [ ] Class-level Javadoc preserved where it provides useful context
- [ ] All tests pass (`mvn test`)

## Technical Spec

### Audit scope
All `src/main/java/` files. Look for Javadoc on:
- Getter/setter methods (Lombok should handle these, but any manual ones)
- One-line delegation methods in controllers
- Factory methods where the name already explains the purpose (e.g., `createEntityObject`)
- Repository interface methods where Spring Data naming convention is self-documenting

### Keep Javadoc on
- Service methods with business rules (drawNumber, drawRandomNumber)
- Exception handler methods (the mapping logic isn't obvious from the name)
- NumberLabelMapper interface and implementations (the strategy pattern needs explanation)
- Any method where the parameter or return semantics aren't obvious from types/names

### Guideline
If the Javadoc just restates the method name in prose ("Gets the room" on `getRoom()`), remove it.

## Done Definition
Codebase has meaningful documentation only. No "documentation for documentation's sake." Tests green.
