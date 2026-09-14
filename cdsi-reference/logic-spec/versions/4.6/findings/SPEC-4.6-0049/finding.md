# SPEC-4.6-0049: Multiple-antigen priority earliest-date aggregation was per-forecast and ignored administered doses

**Status:** open
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

- Unit 9.3 on the Codex branch: 34 tests, 3 failures, 0 errors. The four priority-related reds are gone; the remaining three are the later issue #71 clusters (forecast reason union, aggregation outside the NOT_COMPLETE gate, contained forecast recording).
- Combined engine and FITS counts on this Chapter 8 rebase are recorded in the progress ledger after the Maven runs complete.

## Affected

- Spec section: 9.3
- Business rules: `MULTIANTVG-1`, `FORECASTPRIORITY-1`
- Code location: `MultipleAntigenVaccineGroup.java`
- FITS cases: DTaP group date movement pending re-measure against the Chapter 8 baseline (3749/4896)
