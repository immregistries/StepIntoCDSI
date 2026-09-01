# SPEC-4.6-0006: CompletePatientSeries mishandles a genuine tie for "most valid doses"

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`CompletePatientSeries.evaluate_ACandidatePatientSeriesHasTheMostValidDoses()` increments the score of only the *first* series it finds at the maximum valid-dose count, then `break`s out of the scoring loop entirely. Any *other* series also at that maximum - a genuine tie - is never reached again by that loop iteration; it gets neither the +1 a lone winner should receive nor whatever explicit tie treatment the spec's Table 8-7/8-8 calls for.

Compare with `InProcessPatientSeries.evaluate_ACandidatePatientSeriesHasTheMostValidDoses()` (documented under [SPEC-4.6-0005](../SPEC-4.6-0005/finding.md)'s affected code), which implements the identical spec pattern **correctly**, using a `greatestElementPosList` that collects every tied series and applies the tie treatment to all of them.

## Interpretation

The existence of a correct reference implementation of the same pattern elsewhere in the codebase (`InProcessPatientSeries`) confirms this is a real, fixable gap specific to `CompletePatientSeries`, not an inherent limitation of the scoring framework. Tracked, not fixed, per standing project direction.

## Affected

- Spec sections: 8.4 (pages 89-90)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.CompletePatientSeries`
- FITS cases: none identified in this pass
