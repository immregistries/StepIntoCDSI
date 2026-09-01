# 8.4 Complete Patient Series

> **Review status:** draft. See Review Findings for a verified tie-handling bug in this step's scoring logic.

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

**[IMPLEMENTATION]** `evaluate_ACandidatePatientSeriesHasTheMostValidDoses()` finds the maximum valid-dose count among COMPLETE series, then loops a second time: any non-complete or below-max series gets `descPatientScoreSeries()` (-1); the **first** series found at the max gets `incPatientScoreSeries()` (+1) and the loop **breaks immediately** - see Review Findings, this does not correctly implement the tie ("0") case.

## State Changes

**[IMPLEMENTATION]** Each scorable patient series in the group has `PatientSeries.incPatientScoreSeries()`/`descPatientScoreSeries()` called on it (a running integer score field), consumed later by 8.7.

## Next Steps

**[SPEC]** Table 8-1 implies scoring flows to prioritized-series selection (8.7) after any of 8.4/8.5/8.6.

**[IMPLEMENTATION]** Unconditional to **8.7**. See `transitions.yaml`.

## Plain-Language Walkthrough

When multiple series are already complete, the only thing distinguishing them (per the spec) is how many valid doses each accumulated - the one with strictly the most gets a point bump, a tie between two or more gets nobody a bump or a penalty, and anyone with fewer gets penalized. In practice, the current code only reliably handles the "one clear winner" case; see Review Findings for what happens when two or more series are actually tied.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.CompletePatientSeries` (LogicStepType `COMPLETE_PATIENT_SERIES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Verified tie-handling bug, `IMPLEMENTATION_MISMATCH` (draft):** `evaluate_ACandidatePatientSeriesHasTheMostValidDoses()` increments the score of only the *first* series it finds at the maximum valid-dose count, then `break`s out of the scoring loop entirely. Any *other* series also at the maximum (a genuine tie) is never reached by that loop iteration again - it receives neither the +1 a lone winner should get nor the 0 a tie should produce, and critically, it was already given -1 in the first pass over non-max series... no, re-reading: the second loop's `continue`/`break` structure means a series tied for the max but iterated *after* the first max-series is simply never visited by the scoring branch, leaving its score contribution as whatever it already was (not explicitly documented as 0, but not the spec's specified "0" outcome either - it's "no code path touched it this round," which is a different thing from "the spec-mandated tie score of 0" whenever this scoring step runs more than once across the overall pipeline). Compare with 8.5's `evaluate_ACandidatePatientSeriesHasTheMostValidDoses()` (`InProcessPatientSeries.java`), which implements the identical spec pattern **correctly** using a `greatestElementPosList` that collects every tied series and applies the tie treatment to all of them - confirming this is a real, fixable gap in 8.4 specifically, not an inherent limitation of the framework. Needs prompt engineering/domain-expert attention.
