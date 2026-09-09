# SPEC-4.6-0031: Table 6-4's Administered Dose Count read the wrong list, and Earliest Date was never constructed

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Two independent gaps in `EvaluateConditionalSkip`'s shared Table 6-4 attribute construction (the base class shared by 6.2, 7.1, and 7.6):

**1. Administered Dose Count read the wrong list.** Table 6-4 types this attribute "Patient Immunization History" - the whole patient's recorded doses - but the code sourced it from `dataModel.getSelectedAntigenAdministeredRecordList()`, the per-antigen-filtered subset built for whichever antigen is currently being evaluated. `theAdministeredDoseCountComesFromThePatientImmunizationHistory` records 3 doses via a `historicDose()` helper that only touches `ImmunizationHistory` and expects the attribute to read 3; it read 1 (the size of the unrelated per-antigen list, which this test never touches).

**2. `caEarliestDate` was declared but never constructed.** `Table 6-4's "Runtime data / Earliest Date"` attribute was added to `conditionAttributesList` while still `null` - the field was declared but no code path ever assigned it a `new ConditionAttribute<>(...)` instance. `tableSixFourRegistersTheEarliestDateAttributeItLists` asserts the published attribute list contains no null entries and that this attribute's type is "Runtime data" - both failed.

## Fix

- `caAdministeredDoseCount.setInitialValue(...)` now reads `dataModel.getImmunizationHistory().getVaccineDoseAdministeredList().size()` (null-guarded to 0, since `getImmunizationHistory()` is only guaranteed non-null in production after 4.1 `GatherNecessaryData` runs - many hand-built unit-test `DataModel`s across the engine never call it).
- `caEarliestDate` is now constructed as `new ConditionAttribute<Date>("Runtime data", "Earliest Date")`, with its initial value read from `dataModel.getForecast()` (also null-safe - no `Forecast` exists yet in the EVALUATE/FORECAST contexts this attribute is unused in, and its value isn't read by any of Table 6-4's own conditions; only its presence and type are tested).

## Collateral fixture fixes (not behavior changes)

Sourcing the dose count from `ImmunizationHistory` broke four other, previously-green tests that all depend on Table 6-8's "has at least one dose been administered" condition (which reads this same attribute): `tableSixEightRuleOneIsMetWhenADoseWasGivenAndTheIntervalDateHasPassed`, `tableSixEightRuleTwoIsNotMetBeforeTheIntervalDate`, `theIntervalConditionIsMetWhenTheReferenceDateEqualsTheIntervalDate` (all `EvaluateConditionalSkipForEvaluationTest`), and `EvaluateConditionalSkipForForecastTest.theIntervalConditionIsAnsweredAgainstTheAssessmentDateToo`. Each relies on the fixture's default `AntigenAdministeredRecord` to satisfy "at least one dose administered" - but their own `setUp()` never mirrors that record into `ImmunizationHistory`, a completeness gap in the hand-built fixtures, not a real production discrepancy: in production, `OrganizeImmunizationHistory` always builds every `AntigenAdministeredRecord` from a `VaccineDoseAdministered` that is already present in `ImmunizationHistory`, so the two lists can never actually diverge the way these fixtures did. Fixed by adding an explicit dose to each of those four tests' own setup, matching what production wiring already guarantees - not a change to what any of them assert.

The dose-count fix also NPE'd across roughly 20 unrelated test classes engine-wide on first attempt, for the same null-`ImmunizationHistory` reason described above; resolved by the null-guard.

## Verification

- Both targeted tests green; the four collateral-fixture tests corrected and green.
- Full `cdsi-engine` suite: sorted diff confirms exactly these six tests flipped, plus a bonus seventh (`ValidateRecommendationTest`'s own "Earliest Date registered" test, which shares the same base class) - and nothing else.
- Full FITS run: 3511 passed both before and after, 0 changed cases - neither attribute is read by any Table 6-4 condition that decides a real outcome, so this is a genuine correctness fix with no FITS materiality today, the same category as SPEC-4.6-0026's CALCDT-5 fix.

## Affected

- Spec sections: 6.2 (page 51, Table 6-4)
- Code locations: `EvaluateConditionalSkip.java`; test fixtures in `EvaluateConditionalSkipForEvaluationTest.java` and `EvaluateConditionalSkipForForecastTest.java`
- FITS cases: none currently exercise either attribute's value
