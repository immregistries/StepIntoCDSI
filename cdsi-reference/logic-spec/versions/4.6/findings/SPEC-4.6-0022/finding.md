# SPEC-4.6-0022: SingleAntigenVaccineGroup mutated one shared VaccineGroupForecast per match, silently discarding all but the last when an antigen has more than one best patient series

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`SingleAntigenVaccineGroupTest` (Section 9.2, Table 9-3) already had two red tests pinning this exact defect before this round touched anything:

- `oneRunProducesAtMostOneVaccineGroupForecastForTheVaccineGroup` - two best patient series for one antigen (HepB Standard + Increased Risk), expected 1 vaccine group forecast, got 2.
- `singleantvgTwoTheEarliestDateIsTheEarliestOfAllContainedPatientSeriesForecasts` - same two series with different earliest dates (01/01/2024 listed first, 06/01/2024 second, deliberately ordered so "last wins" is distinguishable from a true minimum), expected the minimum (01/01/2024), got 06/01/2024.

Both point at the same root cause: `process()` built one `VaccineGroupForecast vgf = new VaccineGroupForecast()` *before* its `for` loop over `dataModel.getBestPatientSeriesList()`, then for every matching series mutated that same object's fields and called `dataModel.getVaccineGroupForecastList().add(vgf)` - adding the same reference again. Whichever match was evaluated last silently overwrote every earlier match's status and dates, and the forecast list ended up holding N references to one object rather than N distinct forecasts.

Direct instrumentation of the real FITS suite for `675c41f8e4b089d2bdc0c483-RSV-2023-0020` (a 60-year-old with one RSV dose administered, expected `COMPLETE`) confirmed `dataModel.getBestPatientSeriesList()` correctly contained two entries for the RSV antigen - "RSV 1-dose series" (`Aged Out`) and "RSV 75 years+ 1-dose series" (`Complete`) - and that the pre-fix code reported whichever was evaluated last (`Aged Out`), discarding the genuinely complete result.

## Interpretation

This bug is not new. It has always been possible to construct it in principle, but it was invisible until SPEC-4.6-0020 corrected Chapter 8 to loop per series group: before that fix, an antigen's Chapter 8 processing produced at most one best patient series, so `SingleAntigenVaccineGroup`'s loop only ever ran once per antigen, and "last match wins" and "the true one match" were indistinguishable. SPEC-4.6-0020 exposed this exactly the way it exposed SPEC-4.6-0021 - both were dormant defects a correct Chapter 8 loop makes reachable.

**The fix.** Collect every matching patient series first, then:

- **SINGLEANTVG-1** (status): pick the most favorable match - `Complete`/`Immune` beats an actionable `Not Complete` beats a dead-end `Contraindicated`/`Aged Out`/`Not Recommended`. Ties keep whichever series Chapter 8's own series-group processing order reached first (the antigen's default, unrestricted group before an age/risk-restricted alternate).
- **SINGLEANTVG-2** (earliest date): literally "the earliest date of *all* the patient series forecasts contained" - an aggregate minimum across every match, independent of which one was chosen for status.
- SINGLEANTVG-3 through 8 (the remaining field copies, not themselves named in Table 9-3): copied from whichever series SINGLEANTVG-1 chose.

**Why not reuse `MultipleAntigenVaccineGroup`'s own precedence?** That class's Table 9-3 rule answers "did the patient satisfy *every* antigen in this bundle" - appropriate when combining genuinely different antigens (Diphtheria + Tetanus + Pertussis for DTaP), where any one antigen being `Aged Out` legitimately means the bundle isn't fully satisfied. `SingleAntigenVaccineGroup`'s question for multiple matches is different: "did the patient satisfy *this one* antigen through *any* of its alternative pathways." Preferring Complete/Immune over the rest is the more defensible reading, but Table 9-3 itself never anticipated more than one contained forecast, so this precedence is a judgment call, not literal spec text - flagged here for CDC/CDSi review alongside this campaign's other similar calls (SPEC-4.6-0018/0019/0021).

## A second, unrelated bug this investigation also found

Fixing this alone did **not** change `675c41f8e4b089d2bdc0c483-RSV-2023-0020`'s FITS result at first. Direct instrumentation of `dataModel.getVaccineGroupForecastList()` immediately before `FitsEngineRunner.compare()` showed the fix working correctly - a single RSV entry, status `Complete` - yet the case still reported `AGED_OUT`. The actual cause: `cdsi-fits-tests`'s `CvxEquivalence.java` (and `cdsi-web`'s `FitsServlet.java`, which the class's own Javadoc says it was copied from) declares CVX **122** - Rotavirus's own code, confirmed against the bundled Supporting Data's `AntigenSupportingData- Rotavirus-508.xml` - as equivalent to RSV's CVX 304 in its `EQUIVALENT_CVX` table. Since `compare()` returns the *first* vaccine-group forecast whose CVX is "the same vaccine" as expected, and Rotavirus's forecast (`Aged Out`, unrelated to this patient's RSV question) happened to sit earlier in `vaccineGroupForecastList` than RSV's own (correct, `Complete`) entry, the comparison silently matched the wrong antigen's forecast. Fixed by removing `"122"` from the RSV equivalence group in both files (also removed a harmless duplicate `"112"` in the neighboring DTaP group while there). A follow-on round also found CVX **314** ("Respiratory syncytial virus (RSV) vaccine, unspecified" - confirmed against the bundled Supporting Data's own `inadvertentVaccine`/`allowableVaccine` entries) was *missing* from the same RSV group, causing FITS case `675c41f8e4b089d2bdc0c483-RSV-2024-0012` to report "No forecasted vaccine group matched CVX 314" even though the engine's forecast was otherwise correct; added it alongside the 122 removal. Neither is a Logic Specification conformance issue - both are test-harness/servlet data bugs outside `cdsi-engine` - so neither is itself a `SPEC-4.6-00NN` finding, but both directly affected this finding's own FITS-visible verification and are documented in the relevant round's progress-ledger entry.

## Affected

- Spec sections: 9.2 (page 95, Table 9-3)
- Code locations: `SingleAntigenVaccineGroup.java`
- FITS cases: RSV cases with more than one best patient series for the antigen; likely also affects any other antigen with a similar non-equivalent multi-series-group shape (Pneumococcal, Meningococcal B, Zoster, COVID-19, HepA, HPV, Hib all declare `equivalentSeriesGroups` data per SPEC-4.6-0020's materiality notes)
