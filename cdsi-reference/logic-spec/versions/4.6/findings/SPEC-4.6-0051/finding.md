# SPEC-4.6-0051: Assessment-relative seasonal recommendation dates (documented deviation)

**Status:** confirmed  
**Category:** SPECIFICATION_AMBIGUITY (intentional product deviation, not a silent reinterpretation)

## Why

CDC Supporting Data publishes one absolute seasonal window per release (e.g. Influenza `20250701`–`20260630`). The Logic Specification reads those dates literally. That breaks down when:

- FITS fixtures are assessed in another flu year than the bundled release, or
- the calendar has moved past the release’s end date before CDC ships the next season file.

## What we do instead

`SeasonalRecommendationDates` keeps the loaded start/end as a **template** and shifts them by whole years around the **assessment date**. 7.4 uses the effective end date (Table 7-11) and effective start (FORECASTDTCAN-1); 7.5 uses the effective start (Table 7-12 / FORECASTDT-1).

If the assessment falls in a **gap** between seasons (e.g. Sep–Mar windows with a summer assessment), the helper keeps the **upcoming** season so FORECASTDTCAN-1 can use the next start date. Table 7-11 Rule 6 ("past end date") is effectively unreachable for normal annual templates under this projection — a deliberate tradeoff.

## Affected

- Spec sections: 7.4, 7.5  
- Code: `SeasonalRecommendationDates`, `DetermineForecastNeed`, `GenerateForecastDatesAndRecommendedVaccines`
