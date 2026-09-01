# 7.1 Evaluate Conditional Skip

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 72. No figures, tables, or business rules of its own - see below.

## Purpose

**[SPEC]** "Evaluate Conditional Skip addresses times when a target dose can be skipped... The process model, attribute table, business rules, and decision tables are used to determine if the target dose can be skipped is the same as described in Chapter 6.2." Only Conditional Skip instances with a context of **Forecast** or **Both** apply here (6.2 uses Evaluation-or-Both instances instead - the same underlying data, filtered differently by context).

This step exists so the exact same skip logic that ran once during evaluation (6.2) is reconsidered during forecasting, since a dose that wasn't skippable when it was administered/evaluated might become skippable by the time forecasting runs (or vice versa) as the patient's other data changes.

## Entry Conditions

**[SPEC]** Runs as part of the forecast-dates-and-reasons sequence (Chapter 7), for the target dose currently being forecast.

## Inputs and Attributes, Business Rules, Decision Tables

**[SPEC]** Identical to 6.2's Tables 6-4 through 6-11 - see [6.2's index.md](../06-02-evaluate-conditional-skip/index.md) for the full attribute table, business rules (CALCDTSKIP-3/4/5, CONDSKIP-1/2), and all six decision tables (6-6 through 6-11). This package does not duplicate that content.

**[IMPLEMENTATION]** Confirmed by reading the source directly: `EvaluateConditionalSkipForForecast` is a two-line subclass of the exact same `EvaluateConditionalSkip` base class that 6.2's `EvaluateConditionalSkipForEvaluation` also subclasses - the same `LT66`/`LT67`/`LT68`/`LT69`/`LT610`/`LT611` decision-table logic runs either way. The only difference in the base class's behavior is `ConditionalSkipType` (`EVALUATE` for 6.2 vs. `FORECAST` here), which affects CONDSKIP-2's reference-date calculation (date administered vs. assessment date) and which Conditional Skip instances are filtered in (context Evaluation-or-Both vs. Forecast-or-Both).

The one gap already documented for 6.2 - `LT67`'s "Completed Series" condition hardcoded to `LogicResult.NO` - applies identically here, since it's the same code.

## State Changes

**[IMPLEMENTATION]** Same as 6.2: on "skip," the target dose is marked `TargetDoseStatus.SKIPPED`.

## Next Steps

**[SPEC]** Not stated explicitly as a transition rule (Table 7-1's chapter overview lists 7.1 as the first forecasting activity, implying it precedes 7.2, but doesn't describe the skip branch).

**[IMPLEMENTATION]** Verified in `EvaluateConditionalSkipForForecast`'s constructor call to the shared base class: skip → **4.4** (loop back to the per-series evaluate/forecast driver, same loop-back target 6.2 uses); no skip → **7.2** Determine Evidence of Immunity. See `transitions.yaml`.

## Plain-Language Walkthrough

This step doesn't introduce any new logic of its own - it re-runs 6.2's entire conditional-skip decision tree (age window, prior series completion, interval, dose count - see 6.2 for the full picture) but in the forecasting context instead of the evaluation context. If the target dose being forecast turns out to be skippable, the engine abandons this forecast attempt and loops back to 4.4 to move on to the next target dose or series; otherwise it proceeds to check evidence of immunity (7.2).

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateConditionalSkipForForecast` (LogicStepType `EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST`) - a near-empty subclass, structurally identical to 6.2's `EvaluateConditionalSkipForEvaluation`.
- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateConditionalSkip` - the shared base implementation (see 6.2's package for the deep dive).
- Tests: no dedicated unit test.

## Review Findings

- No new findings specific to this section - see 6.2's Review Findings for the `LT67` "Completed Series" gap, which applies here identically since the code is shared.
