# 7.5 Generate Forecast Dates and Recommended Vaccines

> **Review status:** draft. The date-calculation half of this section (FORECASTDT-1 through 6) was traced and verified; the recommended-vaccine/dose-number/guidance rules (FORECASTRECVAC-1, FORECASTDN-1, FORECASTGUIDANCE-1) were not confirmed as implemented anywhere in this pass - see Review Findings.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 80-82. Figure 7-7 (Forecast Dates Timeline), Figure 7-8 (Process Model). Table 7-12 (Attributes), Table 7-13 (Business Rules - 8 rules: FORECASTDT-1 through 6, FORECASTGUIDANCE-1, FORECASTRECVAC-1, FORECASTDN-1). No decision table in this section.

## Purpose

**[SPEC]** "Generate forecast dates and recommend vaccines determines the forecast dates for the next target dose and identifies one or more recommended vaccines if the target dose warrants specific vaccine recommendations... If the patient has not adhered to the preferred schedule, then the forecast dates are adjusted to provide the best dates for the next target dose." **[SPEC]** "If an attribute value is empty, then the date calculations will remain empty. No assumptions will be made for the attribute" - unlike most other sections, this one has no "assumed value if empty" fallbacks in its attribute table.

## Entry Conditions

**[SPEC]** Runs only when 7.4 determined a dose is needed (Rule 1).

## Inputs and Attributes

**[SPEC]** Table 7-12: ten attributes, all calculated dates or Supporting Data values with no assumed-empty fallback - Minimum/Earliest Recommended/Latest Recommended/Maximum Age Date (CALCDTAGE-4/3/2/1), Minimum/Earliest Recommended/Latest Recommended Interval Date(s) (CALCDTINT-4/5/6), Latest Conflict End Interval Date (CALCDTLIVE-4), Seasonal Recommendation Start Date, and the preferable vaccine's type/forecast-flag.

**[IMPLEMENTATION]** Each has a corresponding `find*()` method in the constructor (`findMinimumAgeDate`, `findMaximumAgeDate`, `findEarliestRecommendedAgeDate`, `findLatestRecommendedAgeDate`, `findMinimumIntervalDates`, `findEarliestRecommendedIntervalDates`, `findLatestRecommendedIntervalDate`, `findLatestConflictEndIntervalDate`, `findSeasonalRecommendationStartDate`) - all genuinely implemented, not placeholders, verified by reading each one.

## Business Rules

**[SPEC]** Table 7-13 (see full text in `extracted/sections/07-05-generate-forecast-dates-and-recommended-vaccines.txt` - it's long; summarized here): FORECASTDT-1 (earliest date = candidate earliest date), FORECASTDT-2 (unadjusted recommended date = earliest recommended age date, or latest earliest-recommended-interval date, or the earliest date itself, in that preference order), FORECASTDT-3 (unadjusted past due date, mirroring FORECASTDT-2 with "latest recommended" dates minus 1 day), FORECASTDT-4 (latest date = maximum age date minus 1 day), FORECASTDT-5 (adjusted recommended date = earliest date, or the unadjusted recommended date if later), FORECASTDT-6 (adjusted past due date = later of earliest date and unadjusted past due date), FORECASTGUIDANCE-1 (administrative guidance text to include), FORECASTRECVAC-1 (which series-dose vaccines count as "recommended" - preferable, forecast-flagged, non-contraindicated, within the preferable vaccine's own age window), FORECASTDN-1 (forecast dose number = count of prior satisfied target doses, +1, with a seasonal-seasoning-start-date wrinkle).

**[IMPLEMENTATION]** FORECASTDT-1 through 6 map one-to-one, in order, to six public methods verified in the source: `computeEarliestDate()`, `computeUnadjustedRecommendedDate()`, `computeUnadjustedPastDueDate()`, `computeLatestDate()`, `computeAdjustedRecommendedDate()`, `computeAdjustedPastDueDate()` - each implements the exact preference order the corresponding rule specifies. FORECASTRECVAC-1, FORECASTDN-1, and FORECASTGUIDANCE-1 have **no correspondingly-named method or rule object in this class**, and this pass did not locate them implemented elsewhere either - see Review Findings; not confirmed as missing, only as unverified.

## Decision Tables

**[SPEC]** None in this section.

## State Changes

**[IMPLEMENTATION]** `process()` calls `computeDates(forecast)` (which internally calls all six `computeX()` methods above and assigns their results onto the `Forecast` object: earliest, unadjusted/adjusted recommended, unadjusted/adjusted past due, latest date), adds the forecast to `dataModel.getForecastList()`, and logs a full summary (antigen, series, dose number, all four key dates) at `STATE` level.

## Next Steps

**[SPEC]** Not stated as a transition rule.

**[IMPLEMENTATION]** Unconditional to **7.6**. See `transitions.yaml`.

## Plain-Language Walkthrough

By the time this step runs, everything needed to say "the next dose is due around this date" has already been calculated as individual candidate dates (age windows, interval windows, conflict windows). This step's job is just to combine them, in the priority order the six FORECASTDT rules specify, into the four dates a forecast actually reports: earliest (the soonest it could possibly be given), recommended (the ideal target), past-due (when it becomes overdue), and latest (the hard cutoff, e.g. aging out). "Adjusted" versions exist because a patient who's behind schedule might have an unadjusted recommended date that's already in the past - the adjusted version pulls it forward to at least the earliest date, so the forecast always makes sense relative to today.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.GenerateForecastDatesAndRecommendedVaccines` (LogicStepType `GENERATE_FORECAST_DATES_AND_RECOMMENDED_VACCINES`) - `cdsi-engine`.
- Tests: no dedicated unit test isolating this class; exercised end-to-end by every `cdsi-fits-tests` fixture whose forecast is `NOT_COMPLETE`.

## Review Findings

- **FORECASTRECVAC-1 (recommended-vaccine selection) and FORECASTDN-1 (forecast dose number) were not found implemented in this class**, despite Table 7-13 listing them as this section's own business rules. This pass did not exhaustively search the rest of the codebase for them (e.g. dose-number counting might live in a `Forecast`/`TargetDose` accessor rather than a named rule method) - recorded as **unverified, not confirmed missing**. Worth a focused follow-up pass specifically searching for where (if anywhere) a `Forecast`'s dose number and recommended-vaccine list actually get set.
- FORECASTGUIDANCE-1 (administrative guidance text) similarly not found - the `Forecast` domain object may simply not have a guidance-text field yet; not confirmed either way.
- **FORECASTDTCAN-1's implementation (traced from 7.4, which owns that rule) appears to omit two of the six candidate dates it specifies** (conflict-end date, seasonal start date) - see [07-04's Review Findings](../07-04-determine-forecast-need/index.md) for the detail; noted here too since it directly affects this section's own FORECASTDT-1 output (earliest date = candidate earliest date).
