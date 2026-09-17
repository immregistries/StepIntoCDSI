# SPEC-4.6-0042: Generate Forecast Dates and Recommended Vaccines implemented only the dates - two rule bugs, two dead attributes, and three business rules with nowhere to write their answer

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

7.5's own title is "Generate Forecast Dates **and Recommended Vaccines**" - only the first half existed. Four groups of gaps, fixed together:

**1. An unguarded fallback.** `computeUnadjustedPastDueDate()` dereferenced `interval.getLatestRecommendedInterval().getDateFrom(...)` without checking for null, throwing `NullPointerException` for any interval defining no latest recommended interval - the sibling `findLatestRecommendedIntervalDate()` in the same class already guards the identical call.

**2. Two dead Table 7-12 attributes.** `caVaccineType` and `caForecastVaccineType` (the Supporting Data preferable-vaccine attributes) were constructed and registered but nothing ever set them - unlike every other Table 7-12 attribute, there was no `findVaccineType()`.

**3. The unadjusted dates were computed and discarded.** `computeDates()` assigned only the four adjusted/earliest/latest dates onto the `Forecast`, even though `Forecast` already had fields and accessors for the two unadjusted dates FORECASTDT-2/FORECASTDT-3 define and Figure 7-7's timeline prints.

**4. Three business rules had nowhere to write their answer.** FORECASTRECVAC-1 (recommended series dose vaccines), FORECASTDN-1 (forecast dose number) and FORECASTGUIDANCE-1 (administrative guidance) were entirely unimplemented - `Forecast` had no recommended-vaccine list, no dose number field, and no guidance field for any of the three to write to. status.yaml's own notes frame this as "one story, not five": the section's title promises both halves, and only the date half was ever built.

## Fix

- `computeUnadjustedPastDueDate()` now checks `interval.getLatestRecommendedInterval() != null` before dereferencing it.
- A new `findVaccineType()` reads the series dose's own (first) preferable vaccine into `caVaccineType`/`caForecastVaccineType`.
- `computeDates()` now also assigns `Forecast.unadjustedRecommendedDate`/`unadjustedPastDueDate`.
- `Forecast` gains `doseNumber` (`Integer`) and `recommendedVaccineList` (`List<VaccineType>`), both genuinely computed: `computeDoseNumber()` implements FORECASTDN-1 exactly (count of `Satisfied` target doses in the relevant patient series' own `getTargetDoseList()`, plus 1); `computeRecommendedVaccineList()` implements FORECASTRECVAC-1's identifying bullet (the series dose's own preferable vaccines whose `forecastVaccineType` flag is `Yes`).
- `Forecast` also gains `getAdministrativeGuidanceList()` (`List<String>`) for FORECASTGUIDANCE-1, but it stays structurally empty - its three sources (antigen series regimen guidance, indication guidance, contraindication guidance) have no representation anywhere else in the domain model or loader yet. Populating it for real is a separate, larger loader-plus-domain-model effort, not a wiring change, matching status.yaml's own scoping note that "sub-conditions cannot be asserted against a value the domain model has nowhere to hold."

## Verification

- Unit 7.5's own suite (`GenerateForecastDatesAndRecommendedVaccinesTest`): 39/39 green (was 32/39 - two reds had already gone green via round 33's cross-unit consistency fix before this round started).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 7 tests flipped, plus five welcome collateral fixes in three different Chapter 9 test classes (`ApplyGeneralVaccineGroupRulesTest` x2, `MultipleAntigenVaccineGroupTest` x2, `SingleAntigenVaccineGroupTest` x1) that depend on `Forecast` now actually carrying a dose number and a recommended-vaccine list. Across all 783 tests, nothing else changed.
- Full FITS run: 3637 passed both before and after, case-by-case diffed to 0 changed cases - exactly as predicted, since the bundled FITS fixtures assert forecast dates and series status only, never recommended CVXs or dose numbers.

## Affected

- Spec sections: 7.5 (pages 81-84, Tables 7-12/7-13)
- Code locations: `GenerateForecastDatesAndRecommendedVaccines.java`, `Forecast.java`
- FITS cases: none - not observable via the bundled fixture assertions
