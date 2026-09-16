# 8.4 Complete Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 89-90. No figure. Table 8-7 (a **scoring** table, not a Yes/No decision grid), Table 8-8 (business rules). Business rules SELECTB-6, SELECTB-19.

*8.4, 8.5, and 8.6 are a family: each scores a specific patient-series shape (Complete/In-process/No-Valid-Doses respectively, as classified by 8.3) using the same "+N / tie / -N" scoring pattern rather than a Yes/No decision table. Read together for consistency - see also `08-05-in-process-patient-series/index.md` and `08-06-no-valid-doses/index.md`.*

## Purpose

**[SPEC]** "Complete patient series provides the decision table for determining the number of points to assign to a complete patient series based on a specified condition."

## Entry Conditions

**[SPEC]** Runs only when 8.3 classified the series group as having 2+ complete patient series (Table 8-5, rule 1).

## Business Rules

**[SPEC]** Table 8-8: SELECTB-6 (complete = forecast status 'Complete', reused from 8.2/8.3), SELECTB-19 (a series "has the most valid doses" if its valid-dose count is >= every other scorable series' count in the group - note this defines a *tie-inclusive* "most," i.e. more than one series can simultaneously "have the most").

## Decision Tables

**[SPEC]** Table 8-7 How Many Points Are Awarded to a Scorable Patient Series That Is a Complete Patient Series?

| Condition | True for this series alone | True for 2+ series (tie) | Not true for this series |
| --- | --- | --- | --- |
| Has the most valid doses (SELECTB-19) | +1 | 0 | -1 |

**[IMPLEMENTATION]** `evaluate_ACandidatePatientSeriesHasTheMostValidDoses()` finds the maximum valid-dose count among COMPLETE series, counts how many share that maximum, then scores: below-max complete series get -1; a lone series at the maximum gets +1; a tie at the maximum is left at 0 (SELECTB-19). Non-complete series are not scored. After scoring, they are dropped from `scorablePatientSeriesList` so 8.7 cannot select them (Table 8-5 Rule 1 / SPEC-4.6-0064).

## State Changes

**[IMPLEMENTATION]** Each complete scorable patient series in the group has `PatientSeries.incPatientScoreSeries()`/`descPatientScoreSeries()` called on it (a running integer score field), consumed later by 8.7. Non-complete series are then removed from `scorablePatientSeriesList` (Table 8-5 Rule 1 / SPEC-4.6-0064).

## Next Steps

**[SPEC]** Table 8-1 implies scoring flows to prioritized-series selection (8.7) after any of 8.4/8.5/8.6.

**[IMPLEMENTATION]** Unconditional to **8.7**. See `transitions.yaml`.

## Plain-Language Walkthrough

When multiple series are already complete, the only thing distinguishing them (per the spec) is how many valid doses each accumulated - the one with strictly the most gets a point bump, a tie between two or more gets nobody a bump or a penalty, and anyone with fewer gets penalized. In-process siblings are dropped before 8.7 so they cannot win a preference tie at score 0.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.CompletePatientSeries` (LogicStepType `COMPLETE_PATIENT_SERIES`) - `cdsi-engine`.
- Tests: `CompletePatientSeriesTest` (17 tests).

## Review Findings

- **Tie handling is implemented:** `countAtMax` awards 0 to every complete series at the maximum valid-dose count and +1 only when exactly one series is at the maximum (Table 8-7). The earlier "first series at max gets +1 then break" gap is closed.
- **SPEC-4.6-0064 (open):** Table 8-5 Rule 1 requires in-process series to be dropped from consideration. 8.4 now removes non-COMPLETE series from the scorable list after Table 8-7 so 8.7 cannot pick an in-process default at score 0 via SELECTBEST-2 preference.
