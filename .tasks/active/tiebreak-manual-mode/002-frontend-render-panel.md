# 002 — Frontend: Show tiebreaker panel in Manual mode

## What to build
Render the `AdminTiebreakPanel` for MANUAL-mode rooms in the admin page, the same way it
already renders for AUTOMATIC rooms. All handlers and the overlay already exist at page
level and are mode-agnostic — this is purely about where the panel is mounted.

Repo: `/home/yanaj/projects/bingo-app-frontend`

## Acceptance Criteria
- [ ] In the admin page, a MANUAL room shows the `AdminTiebreakPanel` (start button + flow).
- [ ] The AUTOMATIC room behavior is unchanged.
- [ ] The panel uses the same props/handlers in both modes (`tiebreak`, `onStart`, `onDrawSlot`).
- [ ] An admin page test asserts the tiebreak panel renders in MANUAL mode.
- [ ] All tests pass (`npm test`).

## Technical Spec

### Files to MODIFY
| File | Change |
|------|--------|
| `app/[locale]/room/[code]/admin/page.tsx` | Render `<AdminTiebreakPanel>` in the MANUAL branch too |
| `app/[locale]/room/[code]/admin/__tests__/page.test.tsx` | Add/adjust a test for the panel in MANUAL mode |

### Implementation Details

In `admin/page.tsx`, the draw-mode conditional currently mounts the tiebreak panel only in
the AUTOMATIC branch:
```tsx
{displayRoom.drawMode === "MANUAL" ? (
    <>
        <HelpText className="text-xs">{t("help.manualMode")}</HelpText>
        <ManualDrawPanel
            drawnNumbers={displayRoom.drawnNumbers}
            onDrawNumber={handleAddNumber}
        />
    </>
) : (
    <>
        <HelpText className="text-xs">{t("help.automaticMode")}</HelpText>
        <AutomaticDrawPanel allDrawn={allDrawn} onDraw={handleDrawNumber} />
        <AdminTiebreakPanel
            tiebreak={tiebreak}
            onStart={handleStartTiebreak}
            onDrawSlot={handleTiebreakDraw}
        />
    </>
)}
```
Change so the `AdminTiebreakPanel` (with the identical props shown above) also renders in the
MANUAL branch — after `ManualDrawPanel`. Do not duplicate the panel for the automatic branch;
keep the existing automatic placement. The cleanest approach: render the mode-specific draw
panel inside the conditional, then mount `<AdminTiebreakPanel .../>` once, after the
conditional, so it shows in both modes. Use whichever of the two approaches (duplicate in the
manual branch, or hoist below the conditional) keeps the JSX simplest — both are acceptable;
the props must be identical to the current automatic usage.

### Files to READ (for patterns — do NOT modify)
| File | What to copy |
|------|-------------|
| `app/[locale]/room/[code]/admin/page.tsx` (lines ~281-306) | Existing AUTOMATIC placement + exact props |
| `app/[locale]/room/[code]/admin/__tests__/page.test.tsx` | Existing test setup — how a MANUAL vs AUTOMATIC room is mocked/rendered, how `AdminTiebreakPanel` presence is asserted |
| `components/admin-tiebreak-panel.tsx` | What the panel renders (to pick a stable query/test id for the assertion) |

### Conventions
- TypeScript + React (Next.js app router), existing component/test idioms.
- Match the test file's existing mocking style (it already renders the admin page for both
  modes — extend that, don't invent a new harness).

## TDD Sequence
1. Add a test: render the admin page with a MANUAL-mode room, assert the tiebreak panel is
   present. Run it; it fails (panel only in automatic branch today).
2. Update `page.tsx` so the panel renders in MANUAL mode.
3. Run `npm test` — all green. Confirm the existing AUTOMATIC tests still pass.

## Done Definition
All acceptance criteria checked. Tests green. No TypeScript/lint errors.
