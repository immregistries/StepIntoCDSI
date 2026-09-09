# SPEC-4.6-0024: FORECASTDTCAN-1's sixth bullet (inadvertent/any-outcome administered dates) was never implemented

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

FORECASTDTCAN-1 (Table 7-9, page 80) has **six** bullets, not five - an earlier full-text extraction pass in this campaign missed the last one:

> The candidate earliest date of a patient series forecast must be the latest of the following dates:
> - Minimum age date
> - Latest of all minimum interval dates
> - Latest of all forecast conflict end dates
> - Seasonal recommendation start date
> - Latest of all dates administered of any inadvertent administration being evaluated against a target dose that is part of a patient series that is the basis of the patient series forecast
> - **Date administered of the most recent vaccine dose administered being evaluated against a target dose that is part of a patient series that is the basis of the patient series forecast.**

`GenerateForecastDatesAndRecommendedVaccines.computeEarliestDate()` only ever checked `vda.getTargetDose() != null` - a field `SatisfyTargetDose` (6.10) sets *only* on its one VALID/SATISFIED outcome. Every other administered dose - inadvertent, extraneous, or otherwise not valid - was invisible to this computation.

Table 6-13's "yes, this is an inadvertent administration" outcome (`EvaluateForInadvertentVaccine.java`) sets the target dose's evaluation status/reason and shortcuts straight back to 4.4, entirely bypassing 6.10 - so neither `VaccineDoseAdministered.targetDose` nor its sibling `evaluatedAgainstTargetDose` (the general "evaluated regardless of outcome" link 6.10's other outcomes do set) is ever populated for an inadvertent administration.

Traced from FITS case `683d9fbee4b07196f3a1f7df-RSV-2025-0010`: an infant given Abrysvo - declared an inadvertent vaccine type for the pediatric "RSV 1-dose series" in the bundled Supporting Data - at 1 month old. Expected earliest/recommended date `2025-12-01` (the administered dose's own date). Actual: `2025-11-01` (the birth date) - the administered dose contributed nothing to the computation at all.

## Fix

Added `VaccineDoseAdministered.inadvertentAdministration` (a plain boolean), set by `EvaluateForInadvertentVaccine`'s inadvertent-yes outcome. This is deliberately a fact recorded about *that one administration* rather than something inferred after the fact from the shared, mutable `TargetDose.evaluation` object - the same target dose can be evaluated against several administered doses in sequence over the course of Chapter 6, and a later dose's outcome would silently overwrite what an earlier dose's own evaluation had been, making any after-the-fact inference from the target dose unreliable.

`computeEarliestDate()` now includes an administered dose's date when `isInadvertentAdministration()` is true (the fifth bullet) **or** `getEvaluatedAgainstTargetDose() != null` **or** `getTargetDose() != null` (the sixth bullet, any outcome that actually reached 6.10). Both of the latter two checks were kept together after an already-written unit test regressed using `getEvaluatedAgainstTargetDose()` alone: that test's hand-built fixture predates the `evaluatedAgainstTargetDose` convention and only calls `setTargetDose(...)` directly, so checking both is what makes the fix robust to both the real pipeline (which always sets both fields together on the VALID path) and this simplified test fixture.

## Verification

- Full `cdsi-engine` suite: confirmed the failure *set* is identical before/after (not just the count) via a sorted diff - one intermediate regression (`GenerateForecastDatesAndRecommendedVaccinesTest.forecastdtOneTheEarliestDateAccountsForTheMostRecentDateAdministered`) was caught and fixed before the final run.
- Full FITS run, case-by-case diffed against the pre-fix baseline (which already included SPEC-4.6-0022/0023): RSV's CVX-314 case and `RSV-2025-0010` both moved from FAIL to PASS, plus 5 incidental HepB improvements, with zero further regressions beyond SPEC-4.6-0023's already-accepted 30.

## Affected

- Spec sections: 6.3 (Table 6-13), 7.5 (Table 7-9, FORECASTDTCAN-1)
- Code locations: `EvaluateForInadvertentVaccine.java`, `GenerateForecastDatesAndRecommendedVaccines.java`, `VaccineDoseAdministered.java`
- FITS cases: any antigen where an inadvertent or otherwise-not-satisfied administered dose should floor the next forecast date
