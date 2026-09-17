# SPEC-4.6-0014: The unadjusted recommended date fell back to today's wall-clock date instead of the series' own earliest date

**Status:** confirmed (reviewed and merged by the project owner - see "Fix merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

FORECASTDT-2's third bullet: "The earliest date of the patient series forecast if there is no earliest recommended age date or earliest recommended interval date."

`GenerateForecastDatesAndRecommendedVaccines.computeUnadjustedRecommendedDate()`'s `else` branch read:

```java
if (dataModel.getVaccineGroupForecastList().size() > 0) {
  Forecast forecast = dataModel.getVaccineGroupForecastList()
      .get(dataModel.getVaccineGroupForecastList().size() - 1);
  unadjustedRecommendedDate = forecast.getEarliestDate();
}
```

`dataModel.getVaccineGroupForecastList()` is a list of *other* patient series' forecasts, built by an entirely separate, later stage of the pipeline (`SingleAntigenVaccineGroup`/`MultipleAntigenVaccineGroup`) - at the point 7.5 runs for the current series, it's still empty. When this yielded nothing, the method fell through to `return new Date()` - literal wall-clock "now", not anything derived from the patient's own data.

The same class already computes exactly the value the rule asks for, in a different method: `computeEarliestDate()`, already used elsewhere in the same class (`forecast.setEarliestDate(computeEarliestDate())`, and as the floor both `computeAdjustedRecommendedDate()` and `computeAdjustedPastDueDate()` compare against).

## Interpretation

A straightforward wrong-variable defect: whoever wrote this branch reached for "the patient series forecast's own earliest date" and grabbed the wrong list entirely, instead of calling the step's own `computeEarliestDate()`, which already existed in the same class for exactly this purpose.

Confirmed live by `forecastdtTwoFallsBackToTheForecastsOwnEarliestDate`, which asserts `step.computeEarliestDate() == step.computeUnadjustedRecommendedDate()` directly. Role A's own note called this "the ordinary case for the majority of series doses (no earliest recommended age, no earliest recommended interval), and it makes the same patient's recommended date change every day the engine is run" - a claim now independently confirmed against real FITS data.

**Real-world confirmation:** 25 real FITS cases (5 distinct scenarios, all in the Polio group, each appearing across 5 test-plan snapshots) had their computed output change between the before and after runs. Inspecting one (`POL-2013-0684`) directly:

| | Before | After |
| --- | --- | --- |
| `recommendedDate` | 2026-09-08 (the date the suite happened to run) | 2026-09-01 (stable, matches `computeEarliestDate()`) |

**None of the 25 changed cases flip from FAIL to PASS.** Each has a separate, unrelated defect - `POL-2013-0684`'s own expected date is 2030-03-21, years away from anything either the before or after value produces, pointing at a different bug entirely in that scenario, not touched by this fix. What this fix demonstrably does is remove a real nondeterminism: before it, any FITS case landing on this ordinary no-earliest-age/no-earliest-interval path reported a different recommended date depending solely on what day the suite was run - not a property a deterministic regression suite should have, independent of whether any particular case happens to pass.

## Fix merged

Reviewed and approved by the project owner, merged to `develop`.

```diff
       log(LogLevel.REASONING,
           "+ unadjusted recommended age date set to the earliest date of the patient series forecast");
-      if (dataModel.getVaccineGroupForecastList().size() > 0) {
-        Forecast forecast = dataModel.getVaccineGroupForecastList()
-            .get(dataModel.getVaccineGroupForecastList().size() - 1);
-        unadjustedRecommendedDate = forecast.getEarliestDate();
-      }
+      unadjustedRecommendedDate = computeEarliestDate();
     }
```

### Test verification

`GenerateForecastDatesAndRecommendedVaccinesTest` (39 tests): before, 30 green / 8 failures / 1 error; after, **31 green / 7 failures / 1 error** - the targeted test flips to green. The 8 remaining reds are the unit's other, previously-documented, unrelated defects (a cross-cutting FORECASTDTCAN-1 discrepancy with 7.4, a latent null-check gap, and the "vaccine/dose-number/guidance half of the section is unimplemented" cluster - none touched by this fix). Full `cdsi-engine` suite: 770 tests, 179 failures, 1 error - down from 180 failures before this round, a reduction of exactly 1. No other test in the suite changed status in either direction.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (4896 fixtures), verified by `ReferenceSetVerifier`. `mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run so `cdsi-fits-tests` resolved the freshly-built jar.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | 3364 |
| Failed assertions | 1531 | 1531 |
| Execution errors | 1 | 1 |

Case-by-case comparison (`results.jsonl`, not just aggregate counts, per the lesson from SPEC-4.6-0013's investigation): **0 cases changed pass/fail status** in either direction, but **25 cases' computed output changed** (all Polio, 5 distinct scenarios × 5 test-plan snapshots each) - a real, verified behavioral change that happens not to flip any case's final verdict, because each of those 25 has its own separate, unrelated defect keeping it in the failed set regardless.

## Affected

- Spec sections: 7.5 (page 82)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.GenerateForecastDatesAndRecommendedVaccines`
- FITS cases: 25 cases (5 distinct Polio scenarios) show a changed computed output; none change pass/fail status (see FITS verification above)
