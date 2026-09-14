# SPEC-4.6-0048: SingleAntigenVaccineGroup left contained forecast metadata and recommended outputs unwritten

**Status:** resolved
**Category:** IMPLEMENTATION_MISMATCH

Ported onto the Chapter 8 rewrite from Codex Chapter 9 commit 2 (`50514f1`). That commit originally used SPEC-4.6-0046, which this branch already assigned to the Chapter 8 series-selection rewrite.

## Evidence

After SPEC-4.6-0022, unit 9.2 still had four open reds:

- `aMissingPatientSeriesStatusFallsBackConsistentlyAcrossBothStatusFields`: `setVaccineGroupStatus` defaulted null to `NOT_COMPLETE`, but `setPatientSeriesStatus` stored the literal null.
- `theContainedPatientSeriesForecastIsRecordedOnTheVaccineGroupForecast`: `VaccineGroupForecast.forecastList` exists, but 9.2 never populated it.
- `singleantvgNineTheAntigensNeededAreTheContainedPatientSeriesTargetDisease`: the antigens-needed setter was commented out.
- `singleantvgTenTheVaccineGroupForecastCanCarryItsRecommendedSeriesDoseVaccines`: originally a field-existence probe; after SPEC-4.6-0042 it became a direct copy assertion.

## Fix

`SingleAntigenVaccineGroup` now records every matching contained `Forecast` in `forecastList`, defaults `patientSeriesStatus` to `NOT_COMPLETE` when the chosen series status is null, populates `antigensNeededList` with the one antigen only when the effective status is `NOT_COMPLETE`, and copies the chosen forecast's `recommendedVaccineList`.

SPEC-4.6-0022 is preserved: when more than one best patient series matches, 9.2 still chooses the most favorable status while computing the earliest date across all contained forecasts.

On the pre-Chapter-8 Codex branch this was FITS-neutral (0 changed cases). Combined FITS against the Chapter 8 rewrite baseline is still to be measured.

## Affected

- Spec sections: 9.2, plus the single-antigen branch of Table 9-2 FORECASTVG-1, FORECASTVG-8, and FORECASTVG-9
- Code locations: `SingleAntigenVaccineGroup.java`
- FITS cases: none expected; FITS does not assert contained forecast lists, antigens-needed lists, or recommended vaccine lists
