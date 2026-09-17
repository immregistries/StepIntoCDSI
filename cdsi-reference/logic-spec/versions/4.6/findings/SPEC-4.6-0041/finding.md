# SPEC-4.6-0041: Determine Forecast Need read the wrong contraindication field, missed two Table 7-9 defaults, and dropped two FORECASTDTCAN-1 candidate dates

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Unit 7.4's six reds, all fixed together:

**1. The contraindication condition read the wrong field.** Table 7-10's fourth condition ("is the relevant patient series a contraindicated patient series?") read `dataModel.getPatient().getMedicalHistory().getContraindicationSet().isEmpty()` - a patient-scoped set of the unrelated `Contraindication_TO_BE_REMOVED` type that nothing anywhere populates - instead of the per-series `PatientSeriesStatus.CONTRAINDICATED` that [SPEC-4.6-0040](../SPEC-4.6-0040/finding.md) now genuinely sets in Table 7-7. This made Rule 5 permanently unreachable in one direction; in the other, it would have let a contraindication recorded against one antigen silence every other antigen's own series, once that set was ever populated by anything. Importantly, this was an independent defect on 7.4's own side, not something a 7.3 fix alone could resolve - the evidence-of-immunity condition right next to it already correctly read the per-series status, so 7.2's own state change needed no fix here at all.

**2. Two Table 7-9 attributes had no assumed value.** `caEvidenceOfImmunity` and `caContraindicatedPatientSeries` were created and registered but never given Table 7-9's own documented defaults ("no evidence", "not contraindicated"), so both published `null` whenever 7.2/7.3 did not mark the series.

**3. A missing candidate date.** FORECASTDTCAN-1's fourth candidate date - the seasonal recommendation start date - was commented out of `computeEarliestDate()` entirely.

**4. Another missing candidate date.** FORECASTDTCAN-1's sixth candidate date - the date administered of the most recent vaccine dose administered evaluated against a target dose - was absent from 7.4's `computeEarliestDate()` altogether, though present in 7.5's own copy of the identical business rule.

## Fix

- Condition 4 now reads `dataModel.getPatientSeriesStepper().getCurrent().getPatientSeriesStatus() == PatientSeriesStatus.CONTRAINDICATED` - the same per-series field the evidence-of-immunity condition already used correctly.
- `caEvidenceOfImmunity`/`caContraindicatedPatientSeries` now carry Table 7-9's documented assumed values, with a real value published when the current patient series actually is Immune or Contraindicated.
- `computeEarliestDate()` now includes the seasonal recommendation start date and, mirroring 7.5's own `computeEarliestDate()` exactly, folds in the latest date administered of any inadvertent administration or a target-dose-evaluated administration.

## Verification

- Unit 7.4's own suite (`DetermineForecastNeedTest`): 22/22 green (up from 16/22).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 6 tests flipped, plus one welcome bonus - `GenerateForecastDatesAndRecommendedVaccinesTest`'s own `forecastdtOneTheEarliestDateIsTheSameCandidateEarliestDateSevenFourTested`, a cross-unit consistency check between 7.4's and 7.5's two `computeEarliestDate()` implementations that could not pass while they disagreed. Across all 783 tests, nothing else changed.
- Full FITS run: 3637 passed both before and after, case-by-case diffed to 0 changed cases. The contraindication fix is FITS-inert for the same reason SPEC-4.6-0040 is - the fixture format cannot encode a patient observation, so `PatientSeriesStatus.CONTRAINDICATED` is never actually set for any FITS case. The two new FORECASTDTCAN-1 candidate dates only affect Table 7-10's narrow Aged-Out boundary conditions, and no bundled case happens to sit on that boundary.

## Affected

- Spec sections: 7.4 (pages 78-80, Tables 7-9 through 7-11)
- Code locations: `DetermineForecastNeed.java`
- FITS cases: none currently exercise the fixed paths
