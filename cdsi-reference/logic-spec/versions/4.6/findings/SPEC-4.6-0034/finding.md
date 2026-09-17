# SPEC-4.6-0034: Inadvertent vaccine data never loaded, its attribute never published, and its outcome could crash

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Three compounding gaps in unit 6.3 (Evaluate for Inadvertent Vaccine):

**1. `<inadvertentVaccine>` was never parsed.** `DataModelLoader.readSeriesDose` handles `doseNumber`, `age`, `interval`, `allowableInterval`, `preferableVaccine`, `allowableVaccine`, `conditionalSkip`, `recurringDose`, `conditionalNeed`, `seasonalRecommendation`, `substituteDose`, and `requiredGender` - and silently drops everything else, including `<inadvertentVaccine>`. The bundled Supporting Data declares 307 such entries, over 26 distinct vaccine types, on 115 of its 484 series doses, across 8 antigens (COVID-19, Polio, Tetanus, Diphtheria, RSV, Pneumococcal, HPV, Pertussis) - every one discarded at load time, making Table 6-13's Rule 1 unreachable on any real forecast.

**2. Table 6-12's own attribute was dead code.** `caInadvertentVaccine` was constructed and registered but never assigned - and mistyped as `ConditionAttribute<VaccineDoseAdministered>` (a copy-paste artifact from the neighboring `caVaccineDoseAdministered`) when it should represent the target dose's own list of inadvertent vaccine types. The step's actual decision condition bypasses the attribute entirely, reading `targetDose.getTrackedSeriesDose().getInadvertentVaccineList()` directly - so this gap only ever affected what the step publishes/renders for display, not any real decision.

**3. Rule 1's outcome could NPE.** and writes to `dataModel.getTargetDose().getEvaluation()` without checking it exists - on the path that reaches 6.3, it doesn't yet: 4.4 dispatches to 6.1 without creating one, and neither 6.1's Rule 3 nor 6.2's cannot-be-skipped outcome record anything before 6.3 runs. The same shape as [SPEC-4.6-0030](../SPEC-4.6-0030/finding.md)'s fix for 6.1.

Gaps (1) and (3) mask each other in production today: because no inadvertent vaccine type ever loads (gap 1), Rule 1 never fires, so gap (3)'s `NullPointerException` is never actually reached either. Fixing the loader alone would have turned a silent miss into a crash - both needed fixing together.

## Fix

- `DataModelLoader` now parses `<inadvertentVaccine>` - resolving its `<cvx>` child to a shared `VaccineType` via `supportingDataModel.getCvx(...)` (the same pattern already used for every other CVX-referencing element) - into `SeriesDose.getInadvertentVaccineList()`.
- `caInadvertentVaccine` is now correctly typed `ConditionAttribute<List<VaccineType>>` and set from that same list.
- Rule 1's outcome now lazily creates an `Evaluation` on the target dose when `getEvaluation()` returns `null`, immediately before setting its status - preserving the existing mutate-in-place behavior when one is already attached.

## Verification

- Unit 6.3's own suite (`EvaluateForInadvertentVaccineTest`): 11/11 green (up from 8/11).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 3 tests flipped and nothing else.
- Full FITS run: 3627 passed both before and after, 0 changed cases - none of the bundled fixtures happens to record an administered dose matching one of the 307 real inadvertent-vaccine entries against the specific target dose it's declared on. A genuine correctness fix the current fixture set doesn't happen to exercise, the same category as SPEC-4.6-0026's CALCDT-5 fix and SPEC-4.6-0030's 6.1 NPE guard.

## Affected

- Spec sections: 6.3 (page 48, Tables 6-12/6-13)
- Code locations: `DataModelLoader.java` (`readSeriesDose`), `EvaluateForInadvertentVaccine.java`
- FITS cases: none currently exercise the real 307 inadvertent-vaccine entries
