# SPEC-4.6-0058: CALCDTINT-1 from a Not Valid last shot (0640 leftover dates)

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH (extends SPEC-4.6-0057)

## Why

SPEC-4.6-0057 taught CALCDTINT-1 to read the previous administered dose's *satisfied* `targetDose` evaluation when the previous target was skipped. That fixed evaluation of Dose 4 after a skipped Dose 3 (`POL-2013-0655`, 0640 status). Forecast dates for `POL-2013-0640` stayed due-now.

The leftover shape is different: the last shot *failed* Dose 4 (`NOT_VALID` / too soon). 6.10 leaves `VaccineDoseAdministered.targetDose` null unless the outcome is SATISFIED, and writes the evaluation onto `evaluatedAgainstTargetDose` for every outcome. Forecast then sets `previousAntigenAdministeredRecord` to that failed shot while `previousTargetDose` is still the skipped Dose 3 (no evaluation). CALCDTINT-1 saw neither a satisfied-target evaluation nor a previous-target evaluation, returned a null reference date, and 7.5 omitted the 6-month minimum interval.

## Evidence

Table 6-19 CALCDTINT-1: the patient's reference dose date is the date administered of the most immediate previous vaccine dose administered when the interval's from-immediate-previous flag is `Y`, that dose's evaluation status is Valid or Not Valid, and it is not an inadvertent administration.

Live leftover after 0057 for `689dc945e4b05decee50df54-POL-2013-0640`:

- Polio 4-dose series: Dose 3 `SKIPPED`, Dose 4 `NOT_SATISFIED` (`NOT_VALID` / too soon) by the 09/01/2026 IPV.
- Forecast `previousAAR` = 09/01/2026 (`targetDose` null, `evaluatedAgainstTargetDose` = Dose 4). `previousTargetDose` = Dose 3 SKIPPED / no eval.
- Earliest/recommended dates were `2026-09-01` (minimum age 4 years and last AAR date). Expected `2027-03-01` = last shot + 6 months.

## Fix (scoped)

`Interval.evaluationOfImmediatePreviousDoseAdministered` uses `evaluatedAgainstTargetDose.getEvaluation()` when `targetDose` is null, then still falls back to the previous target for the isolated 6.5/6.6 unit-test shape.

Out of scope: always-running 6.6 after a satisfied 6.5; remaining POL RD off-by-1 (`0631`/`0681`/`0721`), grace COMPLETE (`0639`), OPV `2024-0071`.

## Verification

Unit: `GenerateForecastDatesAndRecommendedVaccinesTest.forecastdtOneMinimumIntervalUsesNotValidPreviousDoseWhenPreviousTargetWasSkipped`, `EvaluatePreferableIntervalTest.preferableIntervalAfterNotValidPreviousDoseUsesTheEvaluatedAgainstTarget`, `EvaluateAllowableIntervalTest.calcdtintOneMeasuresFromANotValidPreviousDoseWhenPreviousTargetWasSkipped`. Engine suite 816 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T005852` vs SPEC-4.6-0057 run `2026-09-15T225201`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4209 | **4229** | **+20** |
| POL | 609 | **614** | **+5** |
| PCV | 337 | **342** | **+5** |
| MEN B | 84 | **88** | **+4** |
| COVID-19 | 133 | 139 | +6 |

- Oracle `POL-2013-0640` PASS (all five copies): ED/RD `2027-03-01`.
- Same-cluster FAIL→PASS: `PCV-2013-0597`, `MEN-B-2024-0069`.
- Newly failing outside COVID: none.
- COVID `2023-0053` PASS→FAIL (3 copies; dates due-now → +28 days). Parked — do not regenerate allowlist. Allowlist still has 6 CVX 213 assertion failures (was 11).
- POL remaining: 24 fails / 5 unique uids (`0631`/`0681`/`0721` RD off-by-1, `0639` grace COMPLETE, `2024-0071` OPV).
