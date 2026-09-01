# 8.5 In-process Patient Series

> **Review status:** draft. See Review Findings for a verified Date-comparison bug.

## Source

Logic Specification for ACIP Recommendations v4.6, page 90. No figure. Table 8-9 (scoring table), Table 8-10 (business rules). Business rules SELECTB-2, SELECTB-3, SELECTB-5, SELECTB-11, SELECTB-12, SELECTB-16, SELECTB-19, SELECTB-23 - the largest rule set of the 8.4/8.5/8.6 scoring family.

*Part of the 8.4/8.5/8.6 scoring family - see `08-04-complete-patient-series/index.md` for the shared pattern this section follows.*

## Purpose

**[SPEC]** "In-process patient series provides the decision table for determining the number of points to assign to an in-process patient series based on a specified condition." Applies when 8.3 classified the group as having 2+ in-process series and 0 complete series.

## Business Rules

**[SPEC]** Table 8-10: SELECTB-2 (all valid doses = every target dose evaluation is 'Valid'), SELECTB-3 (completable = forecast finish date < the last target dose's maximum age date), SELECTB-5 (closest to completion = fewest not-satisfied target doses among the group), SELECTB-11 (can finish earliest = completable AND finish date on/before every other completable series' finish date), SELECTB-12 (forecast finish date = earliest forecast date + latest minimum interval of remaining doses), SELECTB-16 (in-process, reused from 8.2/8.3), SELECTB-19 (has the most valid doses, reused from 8.4), SELECTB-23 (product series = product path flag 'Y').

**[IMPLEMENTATION]** Each has a directly corresponding private method in `InProcessPatientSeries.java` (`evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses`, `...IsCompletable`, `...HasTheMostValidDoses`, `...IsClosestToCompletion`, `...CanFinishEarliest`), matching the five Table 8-9 conditions.

## Decision Tables

**[SPEC]** Table 8-9 How Many Points Are Awarded to a Scorable Patient Series That Is an In-Process Patient Series?

| Condition | True alone | True for 2+ (tie) | Not true |
| --- | --- | --- | --- |
| Is a product series AND has all valid doses | +2 | n/a | -2 |
| Is completable | +3 | n/a | -3 |
| Has the most valid doses | +2 | 0 | -2 |
| Is closest to completion | +2 | 0 | -2 |
| Can finish earliest | +1 | 0 | -1 |

**[IMPLEMENTATION]** Verified condition-by-condition:
- **Product + all valid doses:** correctly scored +2/-2 for the whole group at once (no per-series tie concept applies here - "n/a" in the spec table, and the code doesn't attempt one).
- **Completable:** correctly scored +3/-3 per series independently (also "n/a" for ties, matching the spec).
- **Has the most valid doses:** correctly handles ties - `evaluate_ACandidatePatientSeriesHasTheMostValidDoses()` builds `greatestElementPosList` of every series at the max count and applies the tie treatment (net 0: +2 then -2 for everyone, or +2-only for a lone winner) to all of them. **This is the correctly-implemented sibling of 8.4's equivalent condition** - see 8.4's Review Findings, which cites this method as the reference implementation 8.4 should match.
- **Is closest to completion:** same tie-safe pattern as above, correctly implemented.
- **Can finish earliest:** see Review Findings - uses `==`/`!=` to compare `Date` objects (reference equality), not `.equals()`.

## Next Steps

**[IMPLEMENTATION]** Unconditional to **8.7**. See `transitions.yaml`.

## Plain-Language Walkthrough

For series that are actively in progress (some valid doses, not yet complete), five separate factors each nudge the score up or down: is it a "product" series with everything valid so far, is it realistically completable before aging out, does it already have the most valid doses, is it closest to being done, and would it finish soonest. Four of the five correctly treat ties as a wash (nobody up, nobody down); the fifth - "can finish earliest" - has a bug that means it essentially never detects a tie at all (see Review Findings), so it behaves more like "whichever series happens to be compared first wins" than the spec's intended tie-safe comparison.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.InProcessPatientSeries` (LogicStepType `IN_PROCESS_PATIENT_SERIES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Verified bug, `IMPLEMENTATION_MISMATCH` (draft):** `evaluate_ACandidatePatientSeriesCanFinishEarliest()` compares forecast dates with `tmpDate == patientSeries.getForecast().getLatestDate()` and `patientSeries.getForecast().getLatestDate() != tmpDate` - Java reference equality on `java.util.Date` objects, not `.equals()`. Two different `PatientSeries` objects with forecasts computed to the identical calendar date will almost always be *different* `Date` instances in memory, so `==` will be `false` even when the dates genuinely match - meaning the tie-detection this method is supposed to perform (`j` counting how many series share the earliest date) essentially never counts a real tie as a tie in practice. Confirmed by inspecting `Forecast`'s date fields, which are ordinary `java.util.Date`, never a cached/interned/shared instance. This is a distinct bug from 8.4's tie-handling gap, in a different condition, but the same category of defect (spec says "treat ties specially," code fails to detect the tie).
- The other four conditions in this class are correctly implemented, including proper tie handling for "has the most valid doses" - see 8.4's Review Findings, which references this class as the correct reference implementation for that specific condition.
