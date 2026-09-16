# SPEC-4.6-0060: CALCDTINT-1 must not measure from an inadvertent previous dose

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

6.3 correctly rejects bOPV (CVX 178) as inadvertent on every Polio series dose. Forecast then measured Dose 2's 4-week interval from that rejected shot (`2016-05-06` + 4 weeks = `2016-06-03`) instead of treating the series as due now.

The leftover shape after SPEC-4.6-0057/0058: 6.3 returns to 4.4 without running 6.10, so the inadvertent VDA had no `evaluatedAgainstTargetDose`. 4.4 stores it as `previousAntigenAdministeredRecord` while `previousTargetDose` is still Dose 1 (`VALID`). CALCDTINT-1 borrowed Dose 1's Valid evaluation and paired it with 178's date.

## Evidence

Table 6-19 CALCDTINT-1: the patient's reference dose date is the date administered of the most immediate previous vaccine dose administered when the interval's from-immediate-previous flag is `Y`, that dose's evaluation status is Valid or Not Valid, and it is not an inadvertent administration. Date and evaluation must belong to the same VDA.

Live `689dc945e4b05decee50df54-POL-2024-0071`:

- DOB `2015-09-13`; tOPV CVX 02 `2016-02-06`; bOPV CVX 178 `2016-05-06`; assessment `2016-05-06`.
- Polio 4-dose: Dose 1 `SATISFIED` / `VALID`, Dose 2 `NOT_SATISFIED` / `NOT_VALID` / `INADVERTENT_ADMINISTRATION`.
- Expected `NOT_COMPLETE` ED/RD `2016-05-06`. Engine ED/RD `2016-06-03`.

tOPV `2016-02-06` + 4 weeks = `2016-03-05`. FORECASTDTCAN-1 then takes the latest of that interval and the inadvertent/last-administered date `2016-05-06` → due now.

## Fix (scoped)

1. 6.3 writes `evaluatedAgainstTargetDose` on the rejected VDA (6.10 never runs on this path).
2. CALCDTINT-1 uses that VDA's own evaluation when it is eligible; if the immediate previous VDA is inadvertent, walk the selected list (then `previousTargetDose.satisfiedBy`) for the last Valid/Not Valid non-inadvertent dose. Isolated 6.5/6.6 fixtures that put a Valid evaluation on `previousTargetDose` and the date on an unlinked previous AAR keep working.

Out of scope: POL RD off-by-1 vs CALCDT-5 (`0631`/`0681`/`0721`) — those remain `FITS_DIFFERENCE` (FITS Nov 30 vs spec Dec 1). Polio cannot reach 0 FITS fails by engine fixes alone while that cluster stands.

## Verification

Unit: `EvaluatePreferableIntervalTest.calcdtintOneMeasuresFromTheLastEligibleDoseWhenTheImmediatePreviousWasInadvertent`, `EvaluateAllowableIntervalTest.calcdtintOneMeasuresFromTheLastEligibleDoseWhenTheImmediatePreviousWasInadvertent`, `GenerateForecastDatesAndRecommendedVaccinesTest.forecastdtOneDoesNotMeasureIntervalFromAnInadvertentPreviousDose`, `EvaluateForInadvertentVaccineTest.ruleOneRejectsAnInadvertentAdministrationAndReturnsToFourFour` (now also asserts the VDA flag and `evaluatedAgainstTargetDose`). Engine suite 821 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T021103` vs SPEC-4.6-0059 run `2026-09-16T014058`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4239 | **4248** | **+9** |
| POL | 619 | **623** | **+4** |
| DTAP | 749 | **754** | **+5** |

- Oracle `POL-2024-0071` PASS (all four copies): ED/RD `2016-05-06`.
- Same-cluster FAIL→PASS: `DTAP-2013-0060` (Tdap as dose 3 under age 4 — inadvertent last shot must not become the interval reference date).
- Newly failing: none. COVID unchanged (139 PASS / 202 FAIL). Allowlist not regenerated (still 6 parked CVX 213 assertion failures).
- POL remaining: 15 fails / 3 unique uids (`0631`/`0681`/`0721` RD off-by-1 vs CALCDT-5).
