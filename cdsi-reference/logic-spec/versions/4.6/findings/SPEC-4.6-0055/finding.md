# SPEC-4.6-0055: Forecast Age/Interval rows ignored Effective/Cessation Dates (§3.3)

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

Section 3.3 / Table 3-5 (RELEVANT-1 / RELEVANT-2) requires selecting Supporting Data Age and Preferable Interval instances whose Effective–Cessation window covers the anchor date (assessment date for forecasting). Unvalued dates default to 01/01/1900 and 12/31/2999.

`GenerateForecastDatesAndRecommendedVaccines` always used `getAgeList().get(0)` and the full interval list. `DataModelLoader` also never parsed Age/Interval `effectiveDate` / `cessationDate`, so even a later filter would have seen nulls.

Polio Dose 4 is the concrete failure: two `<age>` rows, ceased `20090806` (minAge **18 weeks**) then effective `20090807` (minAge **4 years**). Modern assessments took the ceased 18-week row for FORECASTDT-1’s minimum-age candidate, so earliest became ~6 months after the last dose while recommended (from earliestRecommendedAge, 4 years on both rows) stayed correct.

Oracle: `POL-2013-0632` — expected ED/RD `2030-05-30` (age 4); actual ED was `2027-03-01`.

## Fix (scoped)

1. Load Age / Interval / AllowableInterval effective and cessation dates in `DataModelLoader` (`Date`, not `String`).
2. Add `RelevantSupportingData` (RELEVANT-1/2 defaults and inclusive bounds).
3. Wire **only** step 7.5 forecast age and preferable-interval attribute finds through `selectAge` / `selectIntervals` anchored on the assessment date.

Out of scope for this finding: Evaluate Age (6.4), preferable/allowable interval evaluation (6.5/6.6), Conditional Skip context/effective dates, and allowable-interval selection in 7.5 (helper exists; not wired yet).

## Verification

Unit: `RelevantSupportingDataTest`, `GenerateForecastDatesAndRecommendedVaccinesTest.theMinimumAgeDateUsesTheAgeRowRelevantForTheAssessmentDate` (green).

FITS run `2026-09-15T195518` vs prior `2026-09-15T175517` (COVID open-ended season baseline):

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 3962 | **4046** | **+84** |
| POL | 417 | **504** | **+87** |
| COVID-19 | 141 | 138 | −3 |

- Oracle `POL-2013-0632`: all plan copies **PASS** (ED/RD age 4).
- POL remaining: 134 fails / 27 unique uids (COMPLETE cluster, ~6mo interval-only ED, off-by-1 day) — separate from this fix.
- Collateral: 3 allowlisted copies of `COVID-19-2024-0067` now ED/RD `2024-09-12` vs FITS `2024-10-17` (~8 weeks after last dose). Preferable-interval §3.3 filtering changed which interval candidates 7.5 maxes; COVID remains parked — do not regenerate allowlist until reviewed.

Maven `FitsFixtureTest` red solely from those 3 allowlist assertions; no other allowlist failures.
