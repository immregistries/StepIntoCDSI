# Step Servlet Modernization Plan

## Purpose

Modernize the `cdsi-web` `/step` experience without changing the core purpose of the page: it is a debugging and explanation view for CDSi schedule authors who want to inspect how a forecast is calculated step by step.

The redesign should preserve the useful relationship between `/forecast` and `/step`: the same scenario parameters should work for both endpoints, so a user can manually switch between a final forecast and an interactive step-through view.

## Current Behavior To Preserve

- `/forecast` runs the scenario to completion and returns the final answer.
- `/step` uses the same scenario URL shape and advances the calculation one displayed step at a time.
- `patientDob` and `evalDate` are the minimum required parameters to start a real step session.
- Scenario inputs are normally supplied by URL from another page or test workflow.
- Manual data entry is useful as a fallback, but it is not the primary workflow.
- The current step UI stores a mutable `DataModel` in the HTTP session and moves it forward.
- Jumping to a step means "process forward until this step is reached, or finish at End if it is not reached."
- Backward engine navigation is not a design goal.

## Problems With The Current Page

- The process diagram is a static PNG, with many transition-specific PNG variants.
- The diagram is not semantically clickable except as a generic form submit target.
- The page is built as a two-column grid of raw scrollable cells.
- Important state is mixed with large generated tables and logs.
- Some rendered steps produce very large HTML report-outs, making the page hard to scan.
- The jump dropdown is less intuitive than clicking the process model directly.
- Missing minimum inputs can currently result in an error instead of a guided start screen.

## Design Principle

The engine state should always move forward, but the user should control which displayed steps are worth saving and revisiting.

Do not show a complete execution trace by default. The CDSi model contains many loops, so a full numbered timeline quickly becomes noise. Instead, let the user's navigation create a curated inspection history.

## Recommended User Experience

Use a modern diagnostic layout:

- Sticky top summary showing patient, assessment date, supporting data set, current step, current antigen, patient series, target dose, administered dose, and forecast highlights.
- Clickable SVG process map laid out like the current `pm.png`.
- Main current-view area showing the current stable state and the selected pre/post/log report-outs.
- Saved Views tray containing only steps the user intentionally displayed.
- Detail tabs for each displayed step:
  - Summary
  - Post step
  - Pre next step
  - Log
  - Tables

The Saved Views tray should be compact. Example labels:

- `4.1 -> 4.2`
- `4.4 -> 6.1`
- `6.10 -> 4.4`

Avoid showing every internal step traversed during a jump unless the user explicitly asks for diagnostic trace output.

## SVG Process Map

Create a curated SVG asset rather than generating a graph layout dynamically.

The existing `pm.png` layout should be treated as the reference because time was spent arranging the process steps in a useful order. If the original source file is unavailable, recreate the SVG by tracing or rebuilding the diagram from the PNG.

A first-pass semantic SVG prototype is available at:

```text
docs/prototype/step-process-model.svg
```

Treat this as a hand-editable starting point. It preserves the approximate current layout, assigns stable IDs to step nodes and transition edges, and includes CSS classes intended for future current/previous/saved/active-edge styling. It should be reviewed visually and tuned before being moved into `cdsi-web/src/main/webapp`.

Recommended structure:

- Use an inline SVG, not an `<img>`, so nodes and edges can be styled and clicked.
- Give every step node a stable ID, such as `step-GATHER_NECESSARY_DATA` or `step-4_1`.
- Give important edges stable IDs, such as `edge-4_1-4_2`.
- Use CSS classes for state:
  - `is-current`
  - `is-previous`
  - `is-active-edge`
  - `is-clickable`
  - `is-disabled`
  - `is-saved`
- Clicking a node submits a forward jump to that step.
- If the step is not reached, the existing jump behavior can continue to End.

Steps that are being removed from the process should be ignored in the new design, even if enum values still exist temporarily.

## Forward-Only Navigation

Preserve the current mental model:

- `Next Step` processes one displayed transition.
- Clicking a step on the SVG processes forward until that step is reached.
- Clicking a step that appears visually "behind" the current position is still valid if the process loops back to it.
- No general rewind or replay should happen during normal operation.
- The browser back button or Saved Views should revisit captured report-outs, not roll back the live `DataModel`.

## Captured Inspection History

Add a session-scoped inspection history that captures only user-displayed views.

When the user clicks `Next Step` or clicks a process-map node, capture:

- previous step type
- current step type
- transition label
- stable summary HTML
- previous step post HTML
- previous step processing log HTML
- current step pre HTML
- compact state summary values for optional diffing

Do not capture all intermediate steps traversed during a jump. Only capture the step where the user intentionally lands.

This gives the user a quiet, curated history of the points they cared about while allowing the engine state to keep moving forward.

## Browser History

Use browser history for captured views, not engine rollback.

For example:

```text
/step/step?view=7
```

Selecting a previous browser history entry should display cached report-outs for that saved view. It should not restore the `DataModel` to that older state.

The UI should distinguish:

- Current engine position
- Saved view currently being inspected

## AJAX Endpoint Shape

Start with an incremental AJAX layer around the existing servlet and renderer.

Example request:

```text
POST /step/step
action=next
format=json
capture=true
```

Example jump request:

```text
POST /step/step
action=jump
jumpTo=Evaluate Age
format=json
capture=true
```

Example response:

```json
{
  "snapshotId": 7,
  "previousStep": "GATHER_NECESSARY_DATA",
  "currentStep": "ORGANIZE_IMMUNIZATION_HISTORY",
  "transition": "4.1-4.2",
  "stableSummaryHtml": "...",
  "postHtml": "...",
  "logHtml": "...",
  "preHtml": "...",
  "mapState": {
    "previous": "GATHER_NECESSARY_DATA",
    "current": "ORGANIZE_IMMUNIZATION_HISTORY",
    "activeEdge": "edge-4_1-4_2"
  }
}
```

Keep full form submission as a fallback where practical.

## Missing Input Start Screen

If `/step` is reached without `patientDob` or `evalDate`, show a guided start screen instead of throwing an error.

The start screen should preserve the URL-oriented workflow:

- Patient DOB
- Evaluation date
- Sex
- Supporting data set
- Optional vaccinations table
- Optional observations table
- `Start Stepping`
- `Forecast`
- `Load Example`

Submitting should build the same parameter shape already used by `/forecast` and `/step`.

## Manual Vaccination Entry

Manual entry is not the primary use case, but it should be smoother than today.

Recommended first version:

- Repeatable table rows
- Date
- CVX
- MVX
- Dose condition
- Add row
- Remove row

Possible later improvements:

- CVX autocomplete
- date validation
- example scenario picker
- import from existing test case URLs

## State Summary And Diffs

Do not attempt a full object graph diff of `DataModel` initially.

Create a compact `StepStateSummary` with fields schedule authors actually use:

- current step
- antigen
- patient series
- target dose
- administered record
- selected antigen administered record
- prior/current target dose
- patient series counts
- best/prioritized/scorable series counts
- forecast count
- vaccine group forecast statuses
- earliest/recommended date highlights

Then compare captured summaries and show small human-readable changes:

```text
Target dose changed: none -> Hib dose 2
Patient series count changed: 0 -> 3
Forecast added: Hib due 08/26/2016
```

This can become very useful without requiring a broad diff engine.

## Rendering Strategy

Reuse `LogicStepRenderer` at first, but add a cleaner boundary around page rendering.

Suggested pieces:

- `StepServlet`: request handling and session coordination
- `StepSessionState`: current `DataModel` plus captured views
- `StepSnapshot`: saved report-out for a user-selected transition
- `StepStateSummary`: compact state summary for sticky UI and diffs
- `StepPageRenderer`: full page shell
- `StepFragmentRenderer`: AJAX fragments
- `StepProcessMapRenderer`: inline SVG and map state

Longer term, split rendering modes:

- summary
- detail
- full

The existing full report-out can remain available, while the normal UI defaults to summary/detail panels with collapsible large tables.

## Implementation Phases

1. Add a non-error start screen for missing `patientDob` or `evalDate`.
2. Create the curated inline SVG process map based on the existing `pm.png` layout.
3. Replace the visible jump dropdown with clickable map nodes while keeping the old controls available as fallback or hidden debug controls.
4. Add AJAX next/jump responses that update the current panels.
5. Add session-scoped captured views for only user-displayed steps.
6. Add Saved Views tray and browser-history navigation over captured snapshots.
7. Add sticky state summary.
8. Add compact state diffing between captured views.
9. Gradually collapse, tab, or summarize the largest generated tables.

## Important Non-Goals

- Do not build a complete execution timeline as the default UI.
- Do not implement general backward engine navigation.
- Do not generate a fresh graph layout dynamically.
- Do not require a frontend framework unless there is a strong reason.
- Do not change core engine semantics for the UI redesign.

## Notes For The Implementing Agent

- Treat this as a `cdsi-web` modernization project. Avoid changing `cdsi-engine` unless a small helper or summary object clearly belongs there.
- Preserve `/forecast` compatibility and the shared URL parameter shape.
- Preserve forward-only stepping and jump semantics.
- Ignore steps being removed from the process image/design for this redesign.
- Prefer incremental changes that keep the current servlet usable during the transition.
- The current PNG variants can remain during transition, but the target design should not depend on transition-specific PNG files.
