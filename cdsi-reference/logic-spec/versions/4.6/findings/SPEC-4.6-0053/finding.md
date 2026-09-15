# SPEC-4.6-0053: Conditional-skip vaccineTypes CVX tokens not trimmed on load

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Influenza Dose 2 season-completion skips list CVX codes as `"15; 16; 88; …; 333"` (space after each semicolon). `DataModelLoader` split on `;` and called `getCvx` without trimming, so only `"15"` resolved; `" 88"`, `" 333"`, etc. were null and never matched administered doses.

`CONDSKIP_1` therefore counted 0 for Vaccine Count by Date/Age sets that should recognize a current-season flu dose. The recurring Dose 2 clone was forecast (~4-week interval) instead of SKIPPED, so FITS cases expecting `COMPLETE` stayed `NOT_COMPLETE`.

`fromMostRecent` interval parsing in the same loader already trimmed CVX tokens — this path did not.

## Fix

Trim each `vaccineTypes` token, skip empties, and add only non-null `VaccineType` lookups (same pattern as `fromMostRecent`). Also trim `seriesGroups` tokens for consistency.

## Related

SPEC-4.6-0052 still required so that once 7.1 does SKIP the last forecast target, Table 7-10 can assign `COMPLETE`.
