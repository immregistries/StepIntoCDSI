# 8.6 No Valid Doses

> **Review status:** draft. See Review Findings for a verified always-scores-positive bug and undocumented extra scoring conditions.

## Source

Logic Specification for ACIP Recommendations v4.6, page 91. No figure. Table 8-11 (scoring table), Table 8-12 (business rules). Business rules SELECTB-3, SELECTB-12, SELECTB-14, SELECTB-23 (SELECTB-3/12/23 are the identical rules 8.5 also cites - genuinely shared calculations reused across both sections, confirmed by comparing the rule text verbatim in both sections' extracted text, not a duplicate-ID coincidence).

*Part of the 8.4/8.5/8.6 scoring family - see `08-04-complete-patient-series/index.md` for the shared pattern.*

## Purpose

**[SPEC]** "No valid doses provide the decision table for determining the number of points to assign to a scorable patient series when there are no valid doses." Applies when 8.3 classified the group as having 0 valid doses across all scorable series.

## Business Rules

**[SPEC]** Table 8-12: SELECTB-3 (completable, shared with 8.5), SELECTB-23 (product series, shared with 8.5), SELECTB-12 (forecast finish date calculation, shared with 8.5), SELECTB-14 (start earliest = start date before every other series' start date).

## Decision Tables

**[SPEC]** Table 8-11 How Many Points Are Awarded to a Scorable Patient Series That Has No Valid Doses?

| Condition | True alone | True for 2+ (tie) | Not true |
| --- | --- | --- | --- |
| Can start earliest (SELECTB-14) | +1 | 0 | -1 |
| Is completable (SELECTB-3) | +1 | n/a | -1 |
| Is a product patient series (SELECTB-23) | -1 | n/a | +1 |

**[IMPLEMENTATION]** Verified condition-by-condition, and this section has the most divergence from spec of the three scoring sections:
- **Can start earliest:** `evaluate_AScorablePatientSeriesCanStartEarliest()` - same `==`/`!=` Date reference-equality bug as 8.5's "can finish earliest" (see Review Findings), so tie detection here has the same defect.
- **Is completable:** `evaluate_ACandidatePatientSeriesIsCompletable()` - see Review Findings, both branches of its if/else call `incPatientScoreSeries()`, so this condition always adds +1 regardless of whether the series is actually completable.
- **Is a product patient series:** correctly scored +1/-1 (note the spec's sign convention is inverted from the other two conditions here - being a product series is a *penalty* in this table, unlike 8.5's Table 8-9 where it's part of a positive condition; the code matches the spec's sign correctly).
- Two additional conditions run that Table 8-11 does not define at all: `evaluate_ACandidatePatientSeriesGenderSpecific()` (bonus point if the series is gender-restricted and the patient's gender matches) and `evaluate_ACandidatePatientSeriesHasExceededTheMaximumAge()` (penalty if past the max-age-to-start date). See Review Findings.

## Next Steps

**[IMPLEMENTATION]** Unconditional to **8.7**. See `transitions.yaml`.

## Plain-Language Walkthrough

When nobody in the group has any valid doses at all, the tiebreakers shift to which series could plausibly still be started/finished in time, whether it's a "product" series (penalized here, unlike the in-process case), and - per two extra checks not in the spec's own table - whether the series is gender-appropriate or has already aged out. As implemented, "is completable" doesn't actually discriminate (see below), so in practice this table currently has three working-ish signals (start-earliest, gender match, age-exceeded) rather than the spec's three, with different content.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.NoValidDoses` (LogicStepType `NO_VALID_DOSES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Verified bug, `IMPLEMENTATION_MISMATCH` (draft):** `evaluate_ACandidatePatientSeriesIsCompletable()`'s if/else both call `patientSeries.incPatientScoreSeries()` - there is no code path that ever decrements for this condition. As written, "is completable" always contributes +1 regardless of the actual finish-date/max-age comparison, contradicting Table 8-11's +1/-1 split. This looks like a copy-paste slip (the `else` branch should very likely call `descPatientScoreSeries()`, matching the pattern every other condition in this scoring family uses).
- **Same Date reference-equality bug as 8.5** (`==`/`!=` instead of `.equals()`) in `evaluate_AScorablePatientSeriesCanStartEarliest()` - see 8.5's Review Findings for the general explanation; the effect here is the same: tie detection for "can start earliest" essentially never fires.
- **Two scoring conditions run that aren't in Table 8-11 at all** (draft `IMPLEMENTATION_MISMATCH`, unconfirmed as intentional or not): gender-match bonus and exceeded-maximum-age penalty. These may be a deliberate, undocumented refinement, or a copy-forward from a different section's logic that doesn't belong here - this pass could not determine which, and it's recorded as an open question rather than resolved by guessing. If real Supporting Data commonly has gender-restricted or near-max-age series competing under this scoring path, this materially changes the outcome versus a spec-literal implementation.
