# SPEC-4.6-0054: Open-ended seasonal start projection (COVID-19)

**Status:** open  
**Category:** SPECIFICATION_AMBIGUITY (extends SPEC-4.6-0051)

## Why

COVID-19 Supporting Data 4.65 publishes `seasonalRecommendation` as **start `20250827`, empty end** on every dose — an open-ended season, not Influenza’s closed Jul–Jun window.

SPEC-4.6-0051’s `SeasonalRecommendationDates.project` required both start and end. When end was null it returned the literal template unchanged, so every assessment before Aug 2025 used earliest/recommended **2025-08-27**. That produced the dominant COVID FITS cluster: expected **2024-08**, actual **2025-08** (~93 field diffs).

## What we do instead

For start-only templates, roll the start back by whole years until the assessment is on or after that anniversary. Do not roll forward — once the anniversary is in the past the season is treated as still open. Closed two-sided windows keep the SPEC-4.6-0051 behavior.

## Verification

Run `2026-09-15T175517`: combined FITS **3956 → 3962** (net +6, all COVID-19, 0 regressions). Allowlist regenerated.

A follow-on attempt to floor overdue vaccine-group earliest/recommended at the assessment date (FITS “due now”) gained ~67 COVID cases but regressed 170 allowlisted cases across many groups and was **not** kept.

## Remaining COVID clusters

- ~67 COMPLETE vs NOT_COMPLETE (likely Vaccine Count by Date season boundaries still absolute)
- Large date-only cluster expecting assessment-date “due now” while FORECASTDT correctly reports a past earliest

## Affected

- Spec sections: 7.4, 7.5 (same deviation family as 0051)
- Code: `SeasonalRecommendationDates`
- FITS: COVID-19 (e.g. `2023-0101`, `2024-0067`)
