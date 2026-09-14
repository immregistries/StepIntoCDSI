# SPEC-4.6-0050: Chapter 9 remaining Table 9-2 aggregation (reason union, status gate, containment, recommended vaccines, dose number)

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

Issue #71 commits 4–6, after SPEC-4.6-0049's MULTIANTVG-1 rewrite.

## Evidence

Unit 9.3 had three remaining reds after SPEC-4.6-0049:

- `forecastvgSevenAReasonSharedByTwoContainedForecastsAppearsOnceInTheUnion`: expected `"Due Now"`, actual `"Due NowDue Now"`. FORECASTVG-7 requires the forecast reasons of all contained forecasts; concatenating raw strings neither de-duplicates nor separates distinct reasons.
- `forecastvgEightTheAntigensNeededAreComputedWhateverTheVaccineGroupStatusIs`: expected `[Mumps]`, actual `[]`. FORECASTVG-8 is defined over each contained forecast's own `'Not Complete'` status. `process()` ran `MULTIANTVG_1()` through `MULTIANTVG_8()` only inside `if (vgf.getVaccineGroupStatus() == NOT_COMPLETE)`, so a Contraindicated MMR group with one still-needed component reported no recommended antigens.
- `theContainedPatientSeriesForecastsAreRecordedOnTheVaccineGroupForecast`: expected the contributing `Forecast` objects, actual `[]`. FORECASTVG-1's containment relation has a field (`forecastList`) that nothing wrote.

Table 9-2 also requires FORECASTVG-9 (union of recommended series dose vaccines) and FORECASTDN-2 (min dose number when `administerFullVaccineGroup` is Y, max when N). 9.3's old `MULTIANTVG-9` block built a local `List<VaccineGroup>` from nested vaccine-group forecasts and discarded it — live dead code of the wrong type.

9.2 already recorded `forecastList`, copied recommended vaccines from the chosen series, and set `antigensNeededList` when the chosen status is Not Complete.

## Fix

`MultipleAntigenVaccineGroup.process()` now runs FORECASTVG-1 through 9 and FORECASTDN-2 for every Table 9-4 status. FORECASTVG-8's own contained-forecast Not Complete filter stays inside `MULTIANTVG_8()`. Forecast reasons are a `LinkedHashSet` joined with `"; "`. Contained `Forecast` objects are written to `forecastList`. Recommended `VaccineType` values are unioned. Dose number is the min of contained dose numbers when the group's administer-full flag is `YES` (MMR) and the max when it is `NO` (DTaP/Tdap/Td). The dead `MULTIANTVG-9` `List<VaccineGroup>` block is deleted.

`SingleAntigenVaccineGroup` copies the chosen forecast's dose number and unions recommended vaccines from every contained forecast, which is FORECASTVG-9 / FORECASTDN-2 for a group that classifies one antigen.

## Verification

- Unit 9.1: 17/17. Unit 9.2: 22/22. Unit 9.3: 37/37 (was 31/34; three new behavioral tests for FORECASTVG-9 union and FORECASTDN-2 min/max).
- Full `cdsi-engine`: 786 tests, 15 failures, 0 errors (was 783/18). The 15 remaining reds are the pre-existing 6.2/7.1/7.2/7.3/7.6/8.1/8.7 cluster, not this change.
- Combined FITS run `2026-09-14T210541-071194Z-15541a2` (working tree with this change; allowlist green): **3691/4896 passed**, 1204 failed assertions, 1 execution error. `changed-cases.json` vs the accepted Chapter 9 baseline has `statusChanged: []`. `known-passing-cases.txt` was not regenerated.

## Affected

- Spec sections: 9.1, 9.2, 9.3
- Business rules: `FORECASTVG-1`, `FORECASTVG-7`, `FORECASTVG-8`, `FORECASTVG-9`, `FORECASTDN-2`
- Code locations: `MultipleAntigenVaccineGroup.java`, `SingleAntigenVaccineGroup.java`
- FITS cases: recorded after the suite run
