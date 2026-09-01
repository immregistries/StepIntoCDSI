# 6.10 Satisfy Target Dose

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 69-70. Figure 6-23 (Process Model). Table 6-31 (decision table). No business rules.

## Purpose

**[SPEC]** "Satisfy target dose uses the results from the previous evaluation sections as conditions to determine if the target dose is satisfied." This is Chapter 6's final, combining step - every prior 6.x result (age validity, interval satisfaction, conflict, vaccine-preference) is read here, not recomputed.

## Entry Conditions

**[SPEC]** Runs after 6.8 or 6.9 (whichever the dose reached).

## Inputs and Attributes

**[SPEC]** No separate attribute table for this section - Table 6-31's four conditions are themselves the "inputs," each a summary of a prior step's outcome rather than a raw data value.

**[IMPLEMENTATION]** Reads `dataModel.getTargetDose().getEvaluation().getEvaluationStatus()` (set by 6.4) directly, and three boolean-ish checks against `dataModel.getTargetDose().getStatusCause()` - a single string that 6.5/6.6 append `"Interval"` to, 6.7 appends `"VirusConflict"` to, and 6.8/6.9 append `"Vaccine"` to when they fail. This is how state accumulates across Chapter 6's steps into one place 6.10 can read: a concatenated string flag rather than a richer typed object.

## Business Rules

None (spec or implementation).

## Decision Tables

**[SPEC]** Table 6-31 Was the Target Dose Satisfied? (4 conditions, 6 outcomes):

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 | Rule 6 |
| --- | --- | --- | --- | --- | --- | --- |
| Valid age for the target dose? | Yes | Extraneous | No | - | - | - |
| Satisfied all preferable/allowable intervals? | Yes | - | - | No | - | - |
| Is this an impacted (conflicting) dose? | No | - | - | - | Yes | - |
| Preferable or allowable vaccine? | Yes | - | - | - | - | No |
| **Outcome** | Satisfied (Valid) | Not Satisfied (Extraneous) | Not Satisfied (Not Valid) | Not Satisfied (Not Valid) | Not Satisfied (Not Valid) | Not Satisfied (Not Valid) |

## State Changes

**[IMPLEMENTATION]** `SatisfyTargetDose$LT`'s six outcomes match the six rules exactly on `TargetDoseStatus` (`SATISFIED` for Rule 1, `NOT_SATISFIED` for all others) and `EvaluationStatus` (`VALID`, `EXTRANEOUS`, `NOT_VALID` × 4). Rule 1 additionally calls `setSatisfiedByVaccineDoseAdministered(...)` and links the `VaccineDoseAdministered` back to this `TargetDose` - the actual "this dose satisfies this target dose" linkage the rest of the engine relies on. `process()` clears `statusCause` back to `""` immediately after, so it doesn't leak into the next target dose's evaluation.

## Next Steps

See `transitions.yaml` - unconditional to 4.4 regardless of outcome; this is the loop-closing step for Chapter 6's per-dose evaluation (see `docs/01-overview-subchapter-loops.md` and `docs/11-processing-model-orchestration.md` in the main project for the outer loop this feeds into).

## Plain-Language Walkthrough

Everything Chapter 6 figured out about one vaccine dose against one target dose gets consolidated here into a single yes/no: is the target dose satisfied? A dose can fail for exactly one of several reasons - too old (extraneous), wrong age otherwise, bad interval, live-virus conflict, or wrong vaccine type/brand - and Table 6-31 picks the single most relevant reason to report even though several conditions could theoretically overlap (the rule ordering effectively prioritizes age validity first, then interval, then conflict, then vaccine type). Either way, control returns to 4.4 to continue with the next target dose or vaccine dose in the patient's history.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.SatisfyTargetDose` (LogicStepType `SATISFY_TARGET_DOSE`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

None for this section - it matched the specification's decision table exactly on inspection, and its state-accumulation design (a concatenated `statusCause` string set by five different earlier steps) is a documented pattern worth carrying into `concepts/` documentation once that phase is built, rather than a defect.
