# SPEC-4.6-0026: CALCDT-5's "move to the first of the next month" rule never actually fired - Calendar's own overflow arithmetic silently pre-empted it

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`TimePeriod.getDateFrom()`'s MONTH/YEAR handling used to look like this:

```java
} else if (type == TimePeriodType.MONTH) {
  month = month + amount;
  // ... normalize month/year ...
  endingDate.set(Calendar.MONTH, month - 1);
  endingDate.set(Calendar.YEAR, year);
}
// CALCDT-5
if (endingDate.get(Calendar.DAY_OF_MONTH) > endingDate.getActualMaximum(Calendar.DAY_OF_MONTH)) {
  // ... roll to the 1st of the next month ...
}
```

`Calendar.set(MONTH, ...)` does **not** clamp an out-of-range day-of-month for the new month - it silently rolls the excess days forward. Setting the month to February while `DAY_OF_MONTH` is still 31 (from the original date) doesn't produce an error or a clamped value; it produces March 3rd (31 minus February's 28 days = 3 days past the 1st of March). By the time the "CALCDT-5" check ran, `endingDate.get(Calendar.DAY_OF_MONTH)` already reflected this silent rollover - a legitimate day-of-month for whatever month it landed on - so `dayOfMonth > actualMaximum` could never be true, and the check never fired.

CALCDT-5's own two worked examples, from Table 3-6:

> - "03/31/2000 + 6 months = 10/01/2000 (September 31 does not exist)"
> - "08/31/2000 + 6 months = 03/01/2001 (February 31 does not exist)"

A new test, `TestTimePeriod.calcdtFiveAComputedDateThatIsNotARealDateMovesForwardToTheFirstOfTheNextMonth`, pinning both verbatim, failed on the second: `TimePeriod("6 months").getDateFrom(08/31/2000)` returned **03/03/2001**, not 03/01/2001 - the exact 3-day overflow the mechanism above predicts (31 − 28 = 3). The first example happened to still pass, because a 1-day overflow (September has 30 days, so 31 − 30 = 1) lands exactly on the 1st of the next month regardless of whether CALCDT-5's dedicated logic actually runs - a coincidence that masked the bug for the smaller of the spec's own two examples.

Traced from FITS case `5925aa6aca7e8c78bc364506-HepA-AART-EXTRA-3`: a dose administered 08/29/2026, HepA's `minInt` of "6 months," landing on a nonexistent 02/29/2027 (2027 is not a leap year). This is only a 1-day overflow (29 − 28 = 1), so - like the spec's own first example - it already produced the CALCDT-5-correct answer (03/01/2027) by coincidence, both before and after this fix. See "Not fixed" below.

## Fix

Capture the original day-of-month *before* touching the Calendar's month/year fields at all. Set the day to 1 first (always valid, so the month/year change can never trigger an overflow while in flight), set the target month/year, and only then compare the *original* day-of-month against the target month's actual maximum - applying "move to the 1st of the next month" only when that comparison genuinely requires it. Applied to both the MONTH and YEAR branches (YEAR has the identical latent bug for a leap-day reference date, e.g. adding 1 year to Feb 29 in a leap year).

## Verification

- Two new `TestTimePeriod` tests (the specification's own two worked examples, plus a third pinning the FITS-observed 08/29/2026 case) - all green.
- Full `cdsi-engine` suite: identical failure set before/after via sorted diff.
- Full FITS run, case-by-case diffed against the pre-fix baseline: 0 regressions, 0 improvements. The multi-day-overflow scenario this fix actually corrects doesn't occur anywhere in the current 4896-case fixture set - only in the hand-authored unit test pinning the spec's own example. Still a genuine, worthwhile correctness fix: it protects against a class of date-arithmetic error (any month/year `TimePeriod` addition landing more than one day past the end of a shorter target month) the fixture set doesn't currently happen to exercise, but real patient data eventually will.

## Not fixed: a spec-vs-fixture conflict, not a code defect

HepA's three sibling FITS cases (`...EXTRA-3`, `...EXTRA-5`, `...EXTRA-7`) all expect **02/28/2027** for this same 08/29/2026 + 6 months computation - directly contradicting CALCDT-5's own literal text and worked examples, both of which this fix (and, by coincidence, the pre-fix code for this specific 1-day-overflow case) compute as **03/01/2027**. Correcting the engine's CALCDT-5 implementation further cannot resolve this either way - the specification and these three fixtures disagree about the intended answer, and the specification's text is unambiguous. Recorded as an open question about whether these "Extra Test Cases" fixtures are authoritative, not an engine defect - see this round's progress-ledger entry.

## Two further, unrelated things this same investigation found

- **CVX 82 was missing from `CvxEquivalence`'s HepA group** (`{"85", "52"}` - unspecified and pediatric formulations - but not 82, adult formulation), causing FITS case `...HepA-AART-EXTRA-10` to report no forecast at all even though the engine's own forecast (always reported under the hardcoded CVX 85) was correct. Same class of bug as SPEC-4.6-0022's CVX 122/314 fixes; added `"82"` to the group in both `cdsi-fits-tests`' `CvxEquivalence.java` and `cdsi-web`'s `FitsServlet.java`. Not itself a logic-spec finding - it's a test-harness/servlet data gap outside `cdsi-engine`.
- **CVX 143 (the vaccine administered in that same `EXTRA-10` fixture) is not recognized anywhere in the bundled 4.65 Supporting Data at all** - confirmed absent from every antigen file and the schedule-level CVX map, not just HepA's own file. `GatherNecessaryData` throws `IllegalArgumentException: Unrecognized cvx code '143'` for this case, which is correct, defensive behavior given the input - the gap is in the Supporting Data content itself (either CVX 143 needs a future official CDC/ACIP data release, or this fixture is testing against a different release than what's bundled), not something engine code or a FITS-harness equivalence table can fix without fabricating data. Left as-is; documented for whoever maintains the bundled Supporting Data release.

## Affected

- Spec sections: 3.4 (page 12, Table 3-6, CALCDT-5) - a cross-cutting date-arithmetic rule used throughout Chapters 4-9 wherever a `TimePeriod` is added to a reference date (ages, intervals, conflicts, contraindications, indications, conditional-skip windows)
- Code locations: `TimePeriod.java`
- FITS cases: none currently exercise the multi-day-overflow scenario this fixes; HepA's three cases trace to a separate, unresolved spec-vs-fixture conflict (see above)
