# SPEC-4.6-0044: A break statement made every scoring tie resolve as a first-listed win, and masked a second Complete-series-only scoping gap

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Two compounding gaps in unit 8.4 (Complete Patient Series):

**1. A break statement defeated tie-inclusive scoring.** `evaluate_ACandidatePatientSeriesHasTheMostValidDoses()`'s scoring loop awarded `+1` to the first series it found at the maximum valid-dose count and then `break`'d out entirely - every series listed after it was never visited by any scoring branch at all, not incremented, not decremented, not considered for the tie outcome. SELECTB-19 defines "has the most" as `>=` every other scorable series - tie-inclusive by design - so two series genuinely tied at the maximum should both score `0` (Table 8-7's middle column), not `+1` for whichever happened to be listed first while its tied sibling silently kept whatever score it already had.

**2. A Not Complete series was scored anyway.** The same method unconditionally decremented any series whose status wasn't `COMPLETE`, even though Table 8-7's own title scopes it to "a Scorable Patient Series That **Is** a Complete Patient Series." A Not Complete series doesn't compete for "has the most valid doses" at all and shouldn't be touched by this row in either direction. This gap was masked by gap 1 until fixed - with the break in place, a mis-scored Not Complete series was rarely the *only* wrong value in a given test fixture.

## Fix

The method now runs three passes instead of two: find the maximum valid-dose count among scorable Complete series, count how many Complete series share it, then score every series accordingly - a lone series at the maximum gets `+1`; two or more sharing it get `0` (Table 8-7's tie column); anything below the maximum gets `-1`. A Not Complete series is now left untouched rather than decremented, matching Table 8-7's own Complete-series-only scope.

## Verification

- Unit 8.4's own suite (`CompletePatientSeriesTest`): 15/15 green (up from 9/15).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 6 tests flipped and nothing else across all 783 tests.
- Full FITS run: 3637 passed both before and after, case-by-case diffed to 0 changed cases. 8.4 only runs when 8.3 finds two or more Complete patient series in one series group, and within that narrow scenario the corrected tie/exclusion handling doesn't change which series 8.7 ultimately prioritizes for any of the 4896 bundled cases.

## Affected

- Spec sections: 8.4 (pages 89-90, Table 8-7 and Table 8-8)
- Code locations: `CompletePatientSeries.java`
- FITS cases: none currently exercise a scoring differential this fix would change
