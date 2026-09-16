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
- Tests: `GenerateForecastDatesAndRecommendedVaccinesTest`.

## Review Findings

- **Documented fix (2026-09-15, SPEC-4.6-0055): §3.3 Age/Preferable Interval selection for forecast.** Table 7-12's age and preferable-interval candidates must come from the Supporting Data rows whose Effective–Cessation window covers the assessment date (RELEVANT-2), not `getAgeList().get(0)`. `DataModelLoader` now parses those dates; `RelevantSupportingData.selectAge` / `selectIntervals` feed the attribute finds. Polio Dose 4's ceased 18-week minAge row no longer overrides the current 4-year row on modern assessments (e.g. `POL-2013-0632`). Evaluation-path §3.3 selection (6.4/6.5/6.6) is still outstanding.
- **Documented fix (2026-09-15, SPEC-4.6-0058): CALCDTINT-1 from a Not Valid last shot.** Table 7-12's minimum-interval candidate uses the same CALCDTINT-1 helper as 6.5/6.6. After a too-soon attempt at the target being forecast, the reference date is that Not Valid administration (via `evaluatedAgainstTargetDose`) so FORECASTDT-1/5 land at last-shot plus minimum interval rather than due-now (`POL-2013-0640`).
- **Documented fix (2026-09-15, SPEC-4.6-0060): CALCDTINT-1 must not measure from an inadvertent previous dose.** Table 7-12's minimum-interval candidate skips an inadvertent immediate previous VDA and measures from the last Valid/Not Valid non-inadvertent dose. FORECASTDTCAN-1 still includes the inadvertent date as a floor, so `POL-2024-0071` is due now rather than last-shot plus 4 weeks.
- **Documented deviation (2026-09-15): assessment-relative seasonal recommendation start date.** Table 7-12's Seasonal Recommendation Start Date and FORECASTDT-1's use of it as an earliest-date candidate are specified against the literal Supporting Data dates. This class reads the *effective* start via `SeasonalRecommendationDates.effectiveStartDate` (same projection 7.4 applies to the seasonal end date and FORECASTDTCAN-1). Closed windows: SPEC-4.6-0051. Open-ended start-only templates (COVID-19): SPEC-4.6-0054. See [07-04's Review Findings](../07-04-determine-forecast-need/index.md).
- **FORECASTRECVAC-1 (recommended-vaccine selection) and FORECASTDN-1 (forecast dose number) were not found implemented in this class**, despite Table 7-13 listing them as this section's own business rules. This pass did not exhaustively search the rest of the codebase for them (e.g. dose-number counting might live in a `Forecast`/`TargetDose` accessor rather than a named rule method) - recorded as **unverified, not confirmed missing**. Worth a focused follow-up pass specifically searching for where (if anywhere) a `Forecast`'s dose number and recommended-vaccine list actually get set.
- FORECASTGUIDANCE-1 (administrative guidance text) similarly not found - the `Forecast` domain object may simply not have a guidance-text field yet; not confirmed either way.
- **FORECASTDTCAN-1's implementation (traced from 7.4, which owns that rule) appears to omit two of the six candidate dates it specifies** (conflict-end date, seasonal start date) - see [07-04's Review Findings](../07-04-determine-forecast-need/index.md) for the detail; noted here too since it directly affects this section's own FORECASTDT-1 output (earliest date = candidate earliest date).
