# SPEC-4.6-0018: Table 6-6's begin-age-date boundary is exclusive when the spec requires it inclusive - fix confirmed correct, but not safe to merge yet

**Status:** open (not merged - see "Why this is not merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Table 6-6's condition text: "Is the Conditional Skip End Age Date > Conditional Skip Reference Date >= Conditional Skip Begin Age Date?" - the lower bound is `>=`, inclusive.

LT66's code:

```java
if (caConditionalSkipEndAgeDate.getFinalValue()
        .after(caConditionalSkipReferenceDate.getFinalValue())
        && caConditionalSkipReferenceDate.getFinalValue()
                .after(caConditionalSkipBeginAgeDate.getFinalValue())) {
    return LogicResult.YES;
}
```

Both comparisons are strict `Date.after`. A reference date exactly equal to the begin age date evaluates NO, not YES.

`EvaluateConditionalSkipForEvaluationTest.theAgeWindowIsInclusiveOfItsBeginAgeDateAndExclusiveOfItsEndAgeDate` already exists and pins the correct (inclusive-begin) behavior - it is currently red.

## Interpretation

The fix, mirroring SPEC-4.6-0017's identical-shape Table 6-8 fix:

```java
if (caConditionalSkipEndAgeDate.getFinalValue()
        .after(caConditionalSkipReferenceDate.getFinalValue())
        && !caConditionalSkipBeginAgeDate.getFinalValue()
                .after(caConditionalSkipReferenceDate.getFinalValue())) {
    return LogicResult.YES;
}
```

closes the red test and is spec-correct in isolation. But applied alone against the full FITS suite (isolated via one-at-a-time reversion of this round's other candidate changes):

| | Baseline | This fix alone |
| --- | --- | --- |
| Passed | 3378 | 3479 (+101) |
| Failed assertions | 1517 | 1416 |
| Execution errors | 1 | 1 |

Net +101, but not a clean improvement: case-by-case comparison shows **115 cases newly passing and 14 cases newly failing**, all in Hib, Pneumococcal and Polio.

### Root cause of the 14-case regression

Traced one case, `675c41f8e4b089d2bdc0c483-HIB-2013-0308`, via direct engine instrumentation: a temporary scratch JUnit test driving the real `DataModelLoader` -> `LogicStepFactory` pipeline for this exact fixture, plus temporary `println` tracing added to `EvaluateConditionalSkip` and `EvaluateAndForecastAllPatientSeries` (both removed before this finding was written; no diagnostic code was committed).

The fix behaves exactly as intended for a single series in isolation: Hib target dose 3 is correctly `SKIPPED` because the patient's reference date exactly equals its conditional-skip begin age date (the 12-month boundary this fix is supposed to make inclusive). The pipeline correctly advances to target dose 4, and the orchestrator's normal 6.1-6.10 dispatch marks it `SATISFIED` against the same administered dose target dose 3 would otherwise have consumed.

None of that is wrong. The real problem: the bundled Supporting Data defines **8 distinct, competing Hib series-group definitions** (confirmed by grepping `<seriesName>` in `AntigenSupportingData- Hib-508.xml`):

- Hib start at 2 months 4-dose series
- Hib start at 7 months 3-dose series
- Hib start at 12 months 2-dose series
- Hib start at 15 months 1-dose series
- Hib PRP-OMP 3-dose series
- Hib risk child 2-dose series
- Hib risk 1-dose series
- Hib risk 3-dose series

and the engine has no series-group loop at all - already documented in `cdsi-reference/step-tests/cross-cutting-notes.md` under **"2026-09-05 - Chapter 8 has no series group, and its eight steps read three different 'series in scope' lists"**. That entry confirms Chapter 8's 8.1-8.7 run once per *antigen* rather than once per *series group* across 15 of the bundled release's 30 antigens - Hib among them - so which of Hib's 8 competing series definitions is even the right one to be evaluating this patient against is itself an open, unresolved question, independent of anything in this unit.

This fix correctly changes the skip/advance outcome *within whichever series definition is in scope*; it cannot, by itself, fix which series definition Chapter 8 should have put in scope in the first place. The 14 regressed cases are exactly the ones where that upstream scoping question changes the answer; the 115 improved cases are exactly the ones where it doesn't.

## Why this is not merged

The fix is correct and its regression is real, but the regression's cause is a separate, already-known, deliberately out-of-scope defect - the cross-cutting-notes.md entry explicitly says fixing it "requires a project-owner sequencing decision across all eight [Chapter 8] steps at once, not a bounded round." Merging this fix now would violate the standing rule that FITS must move forward, not backward, and bundling in a fix for Chapter 8's series-group scoping is far outside this unit's bounded round.

**Recommended path:** re-attempt this exact fix once Chapter 8's series-group loop is addressed (a separate, larger, already-flagged initiative). The 115-case improvement observed here suggests the fix will then land clean, with the 14-case regression resolving alongside the series-group fix rather than needing separate handling.

## Affected

- Spec sections: 6.2 (page 55)
- Code locations: `EvaluateConditionalSkip` (shared base class for 6.2, 7.1, 7.6)
- FITS cases: 14 cases regress and 115 improve if merged today (net +101, but not a clean move); see `cdsi-reference/step-tests/cross-cutting-notes.md`'s 2026-09-05 entry for the blocking defect
