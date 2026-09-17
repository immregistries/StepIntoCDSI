# SPEC-4.6-0023: Seasonal recommendation dates were never parsed out of Supporting Data - permanently inert for every antigen that declares one

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`DataModelLoader.readAntigenSeries`'s `<selectSeries>` child-element parsing had this branch:

```java
} else if (parentNode.getNodeName().equals("seasonalRecommendation")) {
  SeasonalRecommendation seasonalRecommendation = new SeasonalRecommendation();
  seasonalRecommendation.setSeriesDose(seriesDose);
  seriesDose.getSeasonalRecommendationList().add(seasonalRecommendation);
```

It creates the record and links it to its series dose, but never reads `<startDate>`/`<endDate>` out of the `<seasonalRecommendation>` XML node itself - both fields on `SeasonalRecommendation` stayed `null` forever, for every antigen that declares one. Confirmed against `AntigenSupportingData-RSV-508.xml`'s "RSV 1-dose series," whose single series dose declares:

```xml
<seasonalRecommendation>
<startDate>20251001</startDate>
<endDate>20260331</endDate>
</seasonalRecommendation>
```

The two consuming steps were already correct: `GenerateForecastDatesAndRecommendedVaccines.findSeasonalRecommendationStartDate()` (7.5, FORECASTDTCAN-1's fourth bullet) and `DetermineForecastNeed.findSeasonalRecommendationEndDate()` (7.4, Table 7-11's "is the assessment date ≤ the seasonal recommendation end date?" condition) both correctly call `referenceSeriesDose.getSeasonalRecommendationList().get(0).getSeasonalRecommendationStartDate()`/`EndDate()` - the accessor was just never populated with real data. This is a loader defect, not a logic-step defect.

Traced from FITS case `675c41f8e4b089d2bdc0c483-RSV-2023-0028`: a newborn with no doses, expected earliest/recommended date `2024-10-01` (the start of RSV season in that fixture's own timeline) - actual was the birth date, `2024-08-21`, with no seasonal constraint applied at all.

## Fix and verification

Parse `<startDate>`/`<endDate>` in the loader's `seasonalRecommendation` branch, reusing the existing `parseDate(String)` helper (already handles the `yyyyMMdd` format these elements use). Added `DataModelLoaderTest.testSeasonalRecommendationDatesAreParsed`, which loads the real bundled 4.65 release and asserts RSV's "RSV 1-dose series" seasonal recommendation carries non-null, correctly-ordered dates - deliberately not asserting the exact literal dates, matching this test class's own stated intent to avoid breaking on routine CDC Supporting Data updates.

## A known, accepted side effect: old fixtures vs. the current season

Fixing this (combined with SPEC-4.6-0022) produced **+214 FITS improvements** (143 PCV, 37 COVID-19, 32 RSV, 2 FLU) but also **30 regressions**, all in FLU/COVID-19 cases dated 2013-2016, 2020, and 2024. These are old NIST fixtures now correctly measured against the bundled Supporting Data's actual, current (2025-2026) season window - a window their own historical `evalDate` predates. This is a test-data-staleness artifact, not a logic defect: FORECASTDTCAN-1 and Table 7-11 both refer to *a* seasonal recommendation start/end date, a single literal value from Supporting Data, not a recurring annual computation - there is no spec-supported way to make an old fixture and the current season's Supporting Data agree without inventing a "nearest matching season" transformation the specification never describes.

Presented to the project owner as an explicit choice, per the standing "FITS moves forward, not backward" rule (the same discipline applied to SPEC-4.6-0020's regression). Decision: ship, given the fix is textually exact and the net effect is strongly positive (+184 net). Recorded here for CDC/CDSi review alongside this campaign's other similar exceptions.

## Affected

- Spec sections: 7.4 (page 77, Table 7-11), 7.5 (page 79-80, Table 7-9/7-12, FORECASTDTCAN-1)
- Code locations: `DataModelLoader.java` (fix), `SeasonalRecommendation.java` (the fields that were always null)
- FITS cases: RSV/COVID-19/FLU cases whose antigen series declares a seasonal recommendation
