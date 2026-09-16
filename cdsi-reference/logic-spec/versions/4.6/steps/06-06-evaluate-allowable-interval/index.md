# 6.6 Evaluate Allowable Interval

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 58-60. Figures 6-12/6-13 (two timelines - "From Immediate Previous Dose Administered Flag," "From Target Dose Number in Series"), Figure 6-14 (Process Model). Table 6-20 (Attributes), Table 6-21 (decision table), Table 6-22 (Business Rules). Business rules CALCDTINT-1, CALCDTINT-2, CALCDTINT-3 - the same rule IDs as 6.5, reused here for a different threshold (the allowable, not preferable, absolute minimum interval); not a duplication error.

## Purpose

**[SPEC]** "Evaluate allowable interval validates the date administered of a vaccine dose administered against defined allowable interval(s) from previous vaccine dose(s) administered. In rare cases, intervals can be applied which are either abnormally early ... or intervals which differ following a not valid administration." **[SPEC]** If a target dose defines no allowable interval attributes at all, the interval "should be considered 'not valid'" (a fail-safe default, opposite of most other steps' "valid if unspecified" convention).

## Entry Conditions

**[SPEC]** Runs after 6.5, regardless of whether the preferable interval was satisfied.

## Inputs and Attributes

**[SPEC]** Table 6-20: Date Administered, Allowable Interval elements, and one calculated date - Absolute Minimum Interval Date (CALCDTINT-3), assumed `01/01/1900`.

**[IMPLEMENTATION]** One `LT` built per *relevant* `AllowableInterval` (RELEVANT-1 via `RelevantSupportingData.selectAllowableIntervals`, date administered), same one-table-per-interval pattern as 6.5.

## Business Rules

**[SPEC]** Table 6-22: CALCDTINT-1 (reference dose date from immediate previous dose), CALCDTINT-2 (reference dose date from a named target-dose-number), CALCDTINT-3 (absolute minimum interval date = reference date + absolute minimum interval).

**[IMPLEMENTATION]** Same as 6.5: this class calls `CALCDTINT_3`; CALCDTINT-1/2 live on `Interval.getPatientReferenceDoseDate`. After a skipped previous target, CALCDTINT-1 uses the previous administered dose's satisfied evaluation (SPEC-4.6-0057), not the skipped target's missing evaluation.

## Decision Tables

**[SPEC]** Table 6-21 Did the Vaccine Dose Administered Satisfy the Allowable Interval for the Target Dose? - a single condition, unlike 6.5's three-rule grace-period grid:

| Condition | Rule 1 | Rule 2 |
| --- | --- | --- |
| Is date administered < absolute minimum interval date? | Yes | No |
| **Outcome** | Not satisfied ("Too soon") | Satisfied |

## State Changes

**[IMPLEMENTATION]** `EvaluateAllowableInterval$LT` outcome 0 correctly sets `EvaluationReason.TOO_SOON` (unlike 6.5's equivalent case - see that step's Review Findings). If no `AllowableInterval` is defined at all (`logicTableList.size() == 0`), `process()` treats that the same as "not satisfied" (`statusCause += "Interval"`), matching the spec's explicit "should be considered 'not valid'" fallback for the undefined case.

## Next Steps

See `transitions.yaml` - unconditional to 6.7 either way; the failure (if any) is recorded via `statusCause` for 6.10 to read later, not branched on here.

## Plain-Language Walkthrough

Where 6.5 is forgiving (a slightly-early dose still often counts, with a note), 6.6 is the stricter backstop: if a dose falls before even the *allowable* absolute minimum, that's recorded as a real interval failure that will make the target dose "Not Satisfied" at 6.10 - unless something else already invalidated it first. A target dose with no allowable interval defined at all fails this check by design (the spec's explicit "not valid" fallback), which is the opposite default from most other CDSi attributes.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateAllowableInterval` (LogicStepType `EVALUATE_ALLOWABLE_INTERVAL`) - `cdsi-engine`.
- Tests: `EvaluateAllowableIntervalTest`.

## Review Findings

- **Documented fix (2026-09-15, SPEC-4.6-0057): §3.3 Allowable Interval selection and CALCDTINT-1 after skip.** Table 6-21 checks run only for allowable-interval rows whose Effective–Cessation window covers the date administered. CALCDTINT-1 measures from the last administered dose when the previous target was skipped, so 6.5 can fail and this empty-allowable fallback can run. Engine still skips 6.6 when 6.5 is satisfied — combining "always run 6.6" with empty-allowable = not valid would fail 465 of 484 series doses.
- **Documented fix (2026-09-15, SPEC-4.6-0058): CALCDTINT-1 after a Not Valid last shot.** Same helper as 6.5/7.5: when the previous AAR failed its target, the reference date comes from `evaluatedAgainstTargetDose` rather than a skipped previous target with no evaluation.
- Outcome 0 correctly sets `EvaluationReason.TOO_SOON` (unlike 6.5's historically wrong equivalent — see that step's Review Findings).
