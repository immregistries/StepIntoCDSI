# SPEC-4.6-0057: 6.6 §3.3 selection and CALCDTINT-1 after a skipped previous target

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH (extends SPEC-4.6-0056)

## Why

SPEC-4.6-0056 left 6.6 Allowable Interval out of scope. Two separate holes sat on the POL COMPLETE-vs-another cluster (`2013-0640`, `2013-0655`):

1. **§3.3 / RELEVANT-1** — 6.6 looped every `<allowableInterval>` on the series dose instead of the rows whose Effective/Cessation window covers the date administered.
2. **CALCDTINT-1 after skip** — Table 6-22 measures the interval from the immediate previous *vaccine dose administered* whose evaluation status is Valid or Not Valid. The engine read `previousTargetDose.getEvaluation()`. A skipped target has no evaluation, so CALCDTINT-3/4 returned null, Table 6-17/6-20 assumed `01/01/1900`, 6.5 Rule 3 treated the interval as satisfied, and 6.6 never ran.

## Evidence

Live dump of `689dc945e4b05decee50df54-POL-2013-0640` (title: dose at age 4 years but &lt;6 months after dose 2 is not valid):

- Polio 4-dose series: Dose 3 `SKIPPED`, Dose 4 `SATISFIED` by the 09/01/2026 IPV.
- 6.5 on Dose 4: `prevTd=SKIPPED/no-eval`, `prevAar=05/01/2026`. 6.6 did not run.

Live dump of `689dc945e4b05decee50df54-POL-2013-0655` (Dose 3 to Dose 4 interval 6 months − 5 days):

- Polio 4-dose series: Dose 4 `NOT_SATISFIED` `NOT_VALID/TOO_SOON`. 6.6 **did** run. Empty allowable already recorded the interval failure.
- Polio 5-dose series: Dose 4 `SKIPPED`, Dose 5 `SATISFIED` by the early shot (`prevTd=SKIPPED/no-eval`). Chapter 8 selected that Complete series.

`EvaluateAllowableInterval` constructed one `LT` per `seriesDose.getAllowableintervalList()` with no `RelevantSupportingData.selectAllowableIntervals`.

## Fix (scoped)

- 6.6: `selectAllowableIntervals` anchored on the date administered (RELEVANT-1). Unspecified / all-ceased allowable still `statusCause += "Interval"`.
- `Interval.getPatientReferenceDoseDate`: CALCDTINT-1 uses the previous antigen-administered-record’s satisfied-target evaluation when that VDA is linked; otherwise falls back to the previous target (the isolated unit-test shape).

Out of scope: always-running 6.6 after a satisfied 6.5 (would combine with empty-allowable = not valid and fail 465 of 484 series doses); Conditional Skip effective dates; Chapter 8 series-selection scoring.

## Verification

Unit: `EvaluateAllowableIntervalTest.onlyAllowableIntervalsRelevantForTheDateAdministeredAreEvaluated`, `EvaluateAllowableIntervalTest.calcdtintOneMeasuresFromThePreviousAdministeredDoseWhenPreviousTargetWasSkipped`, `EvaluatePreferableIntervalTest.preferableIntervalAfterSkippedPreviousTargetUsesThePreviousAdministeredDose`, `RelevantSupportingDataTest.selectAllowableIntervalsDropsCeasedRow` (green). Engine suite 813 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-15T225201` vs SPEC-4.6-0056 run `2026-09-15T215530`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4094 | **4209** | **+115** |
| POL | 554 | **609** | **+55** |
| DTAP | 719 | **749** | **+30** |
| Hib | 480 | **500** | **+20** |
| PCV | 332 | **337** | **+5** |
| COVID-19 | 128 | 133 | +5 |

- Newly failing: none.
- Oracle `POL-2013-0655` PASS (all five copies).
- Oracle `POL-2013-0640` status now NOT_COMPLETE (was COMPLETE); ED/RD still assessment date vs expected +6 months (`2026-09-01` vs `2027-03-01`). Same leftover catch-up-date cluster as the former 0630 group, which this round also cleared (`2013-0630` PASS).
- POL remaining: 29 fails / 6 unique uids (`0631`/`0681`/`0721` RD off-by-1, `0639` grace COMPLETE, `0640` dates, `2024-0071` OPV).
- Allowlist: 11 COVID-19 (CVX 213) assertion failures, parked — do not regenerate allowlist.

