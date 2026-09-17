# SPEC-4.6-0049: Multiple-antigen priority earliest-date aggregation was per-forecast and ignored administered doses

**Status:** resolved
**Category:** IMPLEMENTATION_MISMATCH

Ported onto the Chapter 8 rewrite from Codex Chapter 9 uncommitted commit 3. That draft originally used SPEC-4.6-0047.

## Evidence

Unit 9.3 had four related reds for `MULTIANTVG-1` and `FORECASTPRIORITY-1`:

- `multiantvgOneOnePriorityForecastMakesTheEarliestDateTheEarliestOfAllContainedForecasts`: one priority contained forecast must flip the whole group calculation to the earliest date across all contained forecasts.
- `multiantvgOneThePriorityBranchIsNoEarlierThanTheLatestDoseAdministeredInTheVaccineGroup`: the priority branch must also be no earlier than the latest administered dose belonging to the vaccine group.
- `forecastpriorityOneATargetDoseWhosePreferableIntervalHasThePriorityFlagIsAPriorityForecast`: priority is defined by the target dose's preferable intervals, even when `Forecast.interval` is unset.
- `forecastpriorityOneOneUnflaggedPreferableIntervalMeansTheForecastIsNotAPriorityForecast`: every preferable interval on that target dose must carry the priority flag.

The old implementation inspected one forecast at a time and treated `Forecast.interval.intervalPriority != null` as the branch selector. That made the result order-dependent and missed both the target-dose universal quantifier and the administered-dose floor.

On the pre-Chapter-8 Codex branch, FITS moved 92 DTaP cases and no other group. Typical shape: after a complete 5-dose childhood series, FITS wants Tdap at 11 (2033); the rewrite surfaces 7 (2029) when Diphtheria or Tetanus Dose 7 (single preferable interval with `intervalPriority` `override`) is a priority forecast. DTaP antigen order is Diphtheria, Pertussis, Tetanus, so the old per-element walk let a later non-priority Pertussis age-11 date pull the group later and accidentally match FITS.

## Fix

`MultipleAntigenVaccineGroup.MULTIANTVG_1()` now computes both candidate contained-date aggregates up front. It decides whether any contained forecast is priority using the forecast target dose's tracked series dose interval list: at least one interval must exist, and every interval must have `IntervalPriority.OVERRIDE`.

When no contained forecast is priority, the group keeps the non-priority branch: the latest earliest date across contained forecasts. When any contained forecast is priority, the group uses the later of the earliest contained earliest date and the latest date administered for a vaccine belonging to the current vaccine group.

This is the spec-correct aggregation. Do not restore the old per-element min/max switch to recover FITS. Remaining 2033-to-2029 movement after this rebase should be classified as exposed upstream (Chapter 8 best-series selection and/or forecast-time conditional skip of Diphtheria/Tetanus Dose 7).

## Verification

- Unit 9.3 after the Chapter 8 rebase: 34 tests, 3 failures, 0 errors. The four priority-related reds are gone; the remaining three are the later issue #71 clusters (forecast reason union, aggregation outside the NOT_COMPLETE gate, contained forecast recording).
- Full `cdsi-engine`: 783 tests, 18 failures, 0 errors (was 26 on `d6aff99`).
- Combined FITS on commit `130d206`, run `2026-09-14T201255-673843200Z-130d206`, vs SPEC-4.6-0046 Chapter 8 baseline:

| | Chapter 8 (`85234203`) | This port (`130d206`) | Delta |
| --- | --- | --- | --- |
| Passed | 3749 | 3691 | **−58** |
| Failed assertions | 1146 | 1204 | **+58** |
| Execution errors | 1 | 1 | 0 |

Every vaccine group other than DTAP is identical to Chapter 8 (HIB/HPV/PCV gains kept; COVID/FLU/POL unchanged). DTAP 745 → 687 passed (147 → 205 failed).

DTAP non-pass shape (205 cases, including the 147 that already failed on Chapter 8): 67 expected 2033 got 2029; 88 other actual-earlier (e.g. `2013-0008` 2027-03-01 → 2026-09-01); 35 actual-later (e.g. `2013-0010` 2027-03-01 → 2029-05-05); 15 same earliest, other field (usually recommended date). Representative 2033→2029 case: `2013-0028` / `AART-DTAP-6` (“#4 at age 4 is UTD until age 11”).

Maven reported 145 known-passing allowlist assertion failures. Only 81 are DTAP. The other 64 (FLU 27, HepB 15, COVID-19 3, HIB/PCV/POL 5 each, MEN B 4) are in groups whose pass counts did not move, so they are stale allowlist entries from `known-passing-cases.txt` never being regenerated after Chapter 8, not this rewrite.

## Decision

Project owner accepted this FITS movement on 2026-09-14 and kept the 9.3 rewrite. `known-passing-cases.txt` was regenerated from run `2026-09-14T201255-673843200Z-130d206` (3691 PASS ids). Do not restore the old per-element min/max switch. The 2033→2029 cluster is spec-correct aggregation of a 7-year Diphtheria/Tetanus Dose 7 override-interval target; FITS and ACIP want Tdap at 11, which means that 7-year target should not still be current — Chapter 8 series selection and/or forecast-time conditional skip, not 9.3. Issue #71 commits 4–6 (reason union, NOT_COMPLETE gate, forecastList/dose number) can continue; they do not change this date rule.

## Affected

- Spec section: 9.3
- Business rules: `MULTIANTVG-1`, `FORECASTPRIORITY-1`
- Code location: `MultipleAntigenVaccineGroup.java`
- FITS cases: DTAP-only −58 vs Chapter 8; examples `2013-0028`, `2013-0008`, `2013-0010`, `AART-DTAP-6`
