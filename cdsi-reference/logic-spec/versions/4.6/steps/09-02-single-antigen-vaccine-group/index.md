# 9.2 Single Antigen Vaccine Group

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 95. No figure of its own. Table 9-3 (Single Antigen Vaccine Group Business Rules). Business rules SINGLEANTVG-1, SINGLEANTVG-2.

## Purpose

**[SPEC]** "The forecasting rules which need to be applied to a single antigen vaccine group are listed in the table below." A single antigen vaccine group (Hib, HepB, Polio, ...) classifies exactly one antigen, so its vaccine group forecast is derived from that one antigen's best patient series forecast.

## Entry Conditions

**[SPEC]** Reached from 9.1 when VACCINEGROUP-1 determines the vaccine group classifies exactly one antigen.

## Business Rules

**[SPEC]** Table 9-3 Single Antigen Vaccine Group Business Rules:

| Business Rule ID | Business Rule |
| --- | --- |
| SINGLEANTVG-1 | The vaccine group status of a vaccine group forecast made for a single antigen vaccine group must be the patient series status of the patient series forecast contained in the vaccine group forecast. |
| SINGLEANTVG-2 | The earliest date of a vaccine group forecast made for a single antigen vaccine group must be the earliest date of all the patient series forecasts contained in the vaccine group forecast. |

**[IMPLEMENTATION]** `SingleAntigenVaccineGroup` implements both, plus copies the remaining vaccine-group-forecast fields (adjusted recommended/past due date, latest date, unadjusted recommended/past due date, forecast reason) directly from the one matching patient series forecast - labeling these additional copies `SINGLEANTVG-3` through `SINGLEANTVG-10` in code comments. **Note:** the specification's own Table 9-3 (as extracted, and confirmed by reading the raw page directly) defines only SINGLEANTVG-1 and SINGLEANTVG-2 by name - the higher-numbered labels are not present in the specification text seen in this pass. This isn't a functional problem (copying every forecast field from the one contributing series is the only sensible behavior for a single-antigen group, and matches what FORECASTVG-2 through 9 - see 9.1's Review Findings - would produce anyway when there's exactly one contained forecast) - it's a labeling choice, most likely the original developer extending the SINGLEANTVG- naming pattern by analogy to document each field copy, not evidence of additional specification rules this pass failed to find.

## Decision Tables

**[SPEC]** None - this section applies rules directly rather than through a Yes/No decision grid.

## State Changes

**[IMPLEMENTATION]** For the best patient series whose forecast's antigen matches the vaccine group's one antigen: builds a `VaccineGroupForecast` with `vaccineGroupStatus` and `patientSeriesStatus` set from the patient series status (SINGLEANTVG-1), and `earliestDate`, `adjustedRecommendedDate`, `adjustedPastDueDate`, `latestDate`, `unadjustedRecommendedDate`, `unadjustedPastDueDate`, `forecastReason` all copied from that one forecast (SINGLEANTVG-2 plus the unlabeled/extended copies). If the patient series status is unexpectedly `null`, logs an `ALERT.MISSING` and defaults to `NOT_COMPLETE` rather than failing. If no best patient series' forecast antigen matches the vaccine group's antigen at all, logs an `ALERT.SPECGAP` and the vaccine group forecast list simply doesn't gain an entry for this group (rather than throwing) - both are defensive fallbacks, not spec-defined behavior, but reasonable given the specification doesn't say what should happen in either edge case.

## Next Steps

**[IMPLEMENTATION]** Unconditional return to section "9" (the chapter's vaccine-group loop driver), whether or not a match was found. See `transitions.yaml`.

## Plain-Language Walkthrough

For a single-antigen group, "assembling a vaccine group forecast" is really just relabeling: find the one best patient series whose forecast is for this group's antigen, and copy its status and every date/reason field straight across. There's no real aggregation math here (that's 9.3's job) - the whole section exists because the vaccine-group *concept* still needs a forecast object of its own, even when only one antigen backs it.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.SingleAntigenVaccineGroup` (LogicStepType `SINGLE_ANTIGEN_VACCINE_GROUP`) - `cdsi-engine`.
- Structured log events: `CONTROL`/`STATE`/`TRACE`/`REASONING` level logs throughout (e.g. "SINGLEANTVG: Starting single antigen vaccine group processing..."), plus two `ALERT.MISSING`/`ALERT.SPECGAP`-prefixed alerts for the edge cases noted above.
- Tests: no dedicated unit test.

## Review Findings

- **Business-rule labeling beyond what the spec names (not a defect):** see Business Rules, above - `SINGLEANTVG-3` through `SINGLEANTVG-10` appear in code comments but not in the specification text this pass extracted. Recorded so a future reviewer checking rule-ID citations against the spec doesn't mistake this for either a missing extraction or an invented spec citation - it's most likely a reasonable code-comment convention, but wasn't verified against every possible version/errata of Table 9-3.
