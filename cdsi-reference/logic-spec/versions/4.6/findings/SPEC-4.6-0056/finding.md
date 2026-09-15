# SPEC-4.6-0056: Evaluation and 7.4 Age/Interval rows ignored Effective/Cessation Dates (§3.3)

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH (extends SPEC-4.6-0055)

## Why

SPEC-4.6-0055 selected Age and Preferable Interval rows in 7.5 only. Section 3.3 / Table 3-5 also applies to evaluation (RELEVANT-1, date administered) and to 7.4’s CALCDTAGE-1 / FORECASTDTCAN-1 candidates (RELEVANT-2, assessment date).

Those steps still used `getAgeList().get(0)` and the full interval list:

- **6.4 Evaluate Age** — Polio Dose 4’s ceased 18-week absMin evaluated modern administrations that should use 4 years − 4 days.
- **6.5 Evaluate Preferable Interval** — both the ceased 4-week and current 6-month Dose 4 interval rows were required to pass.
- **7.4 Determine Forecast Need** — FORECASTDTCAN-1’s minimum-age candidate still came from index 0 (18 weeks).

Oracle for the leftover forecast-need piece: same dual Age rows as `POL-2013-0632`. Oracle for evaluation-path interval selection: `POL-2013-0640` (dose at 4y but &lt;6 months after prior; FITS wants NOT_COMPLETE).

## Fix (scoped)

Reuse `RelevantSupportingData` already added in SPEC-4.6-0055:

- 6.4: `selectAge` anchored on the date administered
- 6.5: `selectIntervals` anchored on the date administered
- 7.4: `selectAge` / `selectIntervals` anchored on the assessment date

Out of scope: 6.6 Allowable Interval, Conditional Skip effective dates / context.

## Verification

Unit: `EvaluateAgeTest.ageAttributesUseTheRowRelevantForTheDateAdministered`, `EvaluatePreferableIntervalTest.onlyPreferableIntervalsRelevantForTheDateAdministeredAreEvaluated`, `DetermineForecastNeedTest.forecastdtcanOneUsesTheAgeRowRelevantForTheAssessmentDate` (green). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-15T215530` vs SPEC-4.6-0055 run `2026-09-15T195518`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4046 | **4094** | **+48** |
| POL | 504 | **554** | **+50** |
| MenB | (in OTHER) | | **+8** (`2024-0075`/`0076`) |
| COVID-19 | 138 | 128 | −10 |

- `POL-2013-0632` still PASS.
- Oracle `POL-2013-0640` did **not** move (still COMPLETE). 6.5 fails the 6-month interval in isolation, but CALCDTINT-1 after skipped Dose 3 treated the interval as assumed PAST so 6.6 never ran. Follow-on: SPEC-4.6-0057.
- `POL-2013-0630` still due-now vs expected +6 months (catch-up target, not this selection bug).
- POL remaining: 84 fails / 17 unique uids.
- Collateral allowlist red: 11 additional COVID-19 copies (`2023-0101`, `2024-0001`, `2024-0002`, `2025-0097`, `2025-0098`) on top of the three `2024-0067` from SPEC-4.6-0055. COVID parked — do not regenerate allowlist until reviewed.

