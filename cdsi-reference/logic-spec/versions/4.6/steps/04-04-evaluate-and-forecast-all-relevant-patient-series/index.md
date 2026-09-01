# 4.4 Evaluate and Forecast all Relevant Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 35-38. Figure 4-5 (Evaluate and Forecast Process Model), Figure 4-6 (Evaluate Immunization History Process Model). No tables. No business rules of its own (the extraction inventory's auto-detected "COVID-19" for this section is a false positive from the business-rule-ID regex matching an antigen name mentioned as a recurring-dose example, not an actual rule ID - see this project's fork report, not a real citation).

## Purpose

**[SPEC]** "This step is the core of the business logic and decision points many people think of when describing evaluation and forecasting. In the Logic Specification, this step contains all of the clinical business rules and decision logic in the form of business rules and decision tables [but those live in Chapters 6 and 7, which this step orchestrates]. At the end of this step, each relevant patient series will have an evaluated history and a forecast."

## Entry Conditions

**[SPEC]** Follows 4.3 - one or more relevant patient series exist, each with its own target dose list.

## Inputs and Attributes

**[SPEC]** None directly (see Chapter 6/7 step packages for the actual attribute tables used during evaluation/forecasting).

## Business Rules

**[SPEC]** None in this section.

## Decision Tables

**[SPEC]** None in this section - see Chapters 6 (evaluation) and 7 (forecasting).

## State Changes

**[SPEC]** Per Figure 4-6 and its numbered description: two collections are walked together - the current patient series' target doses, and its antigen administered records (AARs) - until either is exhausted:
1. Get the first target dose, then the first AAR.
2. Evaluate the AAR against the target dose (Chapter 6).
3. If satisfied, advance to the next target dose (checking first whether the just-satisfied dose is a *recurring* dose, in which case a duplicate target dose is inserted immediately after it, e.g. yearly flu, decennial Td); if not satisfied, keep the same target dose but advance to the next AAR.
4. Repeat until target doses are exhausted (remaining AARs become `EXTRANEOUS`) or AARs are exhausted (evaluation for this series ends and forecasting - Chapter 7 - begins for whichever target dose evaluation stopped on).

**[IMPLEMENTATION]** `EvaluateAndForecastAllPatientSeries.process()` implements exactly this state machine via a `Neighborhood` enum (`EVALUATE`, `FORECAST`, `SETUP`, `SELECT_BEST_SERIES` - itself an implementation concept with no direct spec name) and a `TargetDoseStatus` switch (`SKIPPED`/`NOT_SATISFIED`/`SATISFIED`/`SUBSTITUTED`/`UNNECESSARY`) that decides whether to advance the AAR position, the target-dose position, both, or neither, matching the spec's four numbered rules. Recurring-dose handling (`moveToNextTargetDoseIfAvailable`) inserts a duplicate `TargetDose` immediately after the current one when `RecurringDose.getValue() == YES`, exactly as the spec describes. When target doses run out with no recurring dose, `markRestAsExtraneous()` sets every remaining AAR's evaluation to `EvaluationStatus.EXTRANEOUS` - matching "Any remaining antigen administered records should have their evaluation statuses set to 'extraneous.'"

## Next Steps

**[SPEC]** Not a single fixed transition - Figure 4-6 shows the loop can re-enter evaluation (Chapter 6), move into forecasting (Chapter 7), or (once all relevant patient series are exhausted) proceed to 4.5.

**[IMPLEMENTATION]** Verified against `process()`/`finalizeStep()` - see `transitions.yaml`. Two transitions have **no specification counterpart at all**: a total-cycle-count guard (>1000 calls) and a repeated-loop-state guard (>200 identical states), both of which force an early transition to 4.5 rather than let the engine hang. See Review Findings.

## Plain-Language Walkthrough

This is the busiest step in the entire engine - the core double loop (patient series × target doses × antigen administered records) that Figure 4-5/4-6 describe runs many times over the course of a single forecast, re-entering this same class repeatedly as it steps through evaluation (Chapter 6) and forecasting (Chapter 7) for each relevant patient series in turn. The `Neighborhood` concept in the code (EVALUATE vs. FORECAST vs. SETUP) is how the implementation keeps track of *where in the spec's two-collection walk* it currently is between re-entries, since each call to `process()` only advances the state machine by one step before handing off to whichever Chapter 6/7 step needs to run next.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateAndForecastAllPatientSeries` (LogicStepType `EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES`) - `cdsi-engine`.
- Structured log events: extensive, at `TRACE`/`STATE`/`CONTROL`/`REASONING` levels, tracing every loop-state transition (e.g. "STATE: Current patient series: X", "CONTROL: Continuing evaluation - selecting AAR #N") plus `alert(...)` calls for invariant violations (e.g. `ALERT.INVARIANT: aarIndex out of range`, `ALERT.MISSING: currentPatientSeries is null`) and for the loop-guard escapes themselves (`ALERT.MAX_PROCESS_CALLS`, `ALERT.LOOP_DETECTED`).
- Tests: no dedicated unit test (see `step.yaml`).

## Review Findings

- **Implementation-only loop guards, no specification basis (informational, not necessarily a defect):** `MAX_TOTAL_CYCLES` (1000) and `MAX_REPEATED_STATE_CYCLES` (200), with a `buildLoopSignature()` state-repeat detector, force a transition to 4.5 if the evaluate/forecast loop runs unexpectedly long or gets stuck repeating the same state. The specification's process model (Figures 4-5/4-6) describes the loop as terminating naturally when both collections are exhausted; it says nothing about a maximum iteration count. This is a defensive engineering safeguard against a real risk (an infinite loop from a data or logic bug elsewhere producing incorrect forecasts silently, or hanging) - worth flagging so a reviewer doesn't mistake it for a specification requirement, and so anyone debugging an `ALERT.LOOP_DETECTED`/`ALERT.MAX_PROCESS_CALLS` log knows it means "something upstream produced a state this loop can't resolve," not "this is expected termination."
