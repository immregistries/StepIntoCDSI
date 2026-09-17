# SPEC-4.6-0033: An extraneous-dose placeholder can get re-evaluated for conditional skip, losing its series' completion status entirely

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Investigated while root-causing SPEC-4.6-0019's still-open PCV regression (part B): an adult patient with a textbook-complete Pneumococcal 50+ sequence (PCV13, then PPSV23, then PCV20 five years later) reported `AGED_OUT` for the antigen overall instead of `COMPLETE`.

Direct instrumentation (a scratch diagnostic capturing every log and alert event across the whole `EvaluateAndForecastAllPatientSeries` → `EvaluateConditionalSkip` cycle, removed before this finding was written) traced the exact sequence for this patient's "Pneumococcal 50+ 2-dose PCV13 series":

1. The series' own dose 4 target dose evaluates to `SKIPPED`.
2. 4.4's `markRestAsExtraneous()` (introduced by SPEC-4.6-0028) creates one placeholder `TargetDose` - status `UNNECESSARY`, evaluation `EXTRANEOUS` - for a leftover administered dose beyond what the 2-dose series needed. That placeholder still tracks dose 4's own `SeriesDose` object, since `markRestAsExtraneous()` has no reason to build a distinct one.
3. 4.4 dispatches into `EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST` (7.1) for that same placeholder, because it's now the "current" target dose and 4.4's forecast-entry dispatch doesn't distinguish a placeholder from a real one.
4. 7.1 evaluates dose 4's real `ConditionalSkip` data - the same data that made dose 4 skip in the first place - decides "skip" is met again, and calls `dataModel.getTargetDose().setTargetDoseStatus(TargetDoseStatus.SKIPPED)`, silently overwriting the placeholder's `UNNECESSARY` status.
5. Control routes back to `EvaluateAndForecastAllPatientSeries` (7.1's own "skip" destination in forecast context), which now sees `SKIPPED`, tries to advance to a next target dose, finds nothing left, and gives up on the series entirely - moving on to the next patient series - without ever reaching `DetermineForecastNeed` (7.4) for this one.
6. `PatientSeriesStatus` is never assigned. The series is left with a `null` status and silently drops out of contention for "best patient series" in favor of whichever other, unrelated Pneumococcal series-group happens to have a properly-set status - even an incorrect `Aged Out` one.

This bug is independent of SPEC-4.6-0019's own "equal to" spelling gap - it was found while investigating that finding, but reproduces with or without that fix, whenever *any* series' own last target dose is `SKIPPED` (for any reason at all) while a leftover administered AAR triggers `markRestAsExtraneous()`. Tested in complete isolation (leaving the "equal to" spelling gap untouched): full FITS run, case-by-case diffed against the true baseline: 3616 → 3627 passed, **0 regressions, 11 improvements** (6 COVID-19, 5 PCV) - confirming this bug was already firing on the current, unmodified codebase for other cases, not only the one this investigation started from.

## Fix

Fixed at the shared `EvaluateConditionalSkip` base class level, so it applies uniformly to all three contexts it serves (6.2, 7.1, 7.6): when the current target dose's status is already `UNNECESSARY`, no conditional skip evaluation is performed at all - the step falls straight through to its `noSkipLogicStep` destination, exactly as if no `ConditionalSkip` data existed for this dose:

```java
if (dataModel.getTargetDose().getTargetDoseStatus() == TargetDoseStatus.UNNECESSARY) {
    log("Target dose is an extraneous-dose placeholder (status UNNECESSARY) - "
            + "no conditional skip evaluation applies.");
} else if (seriesDose.getConditionalSkip() != null) {
    // ... existing table-building logic, unchanged ...
} else {
    log("No conditional skips are defined. ");
}
```

An `UNNECESSARY` target dose is - today - only ever a `markRestAsExtraneous()` placeholder: definitively resolved bookkeeping, not a real dose awaiting a skip/no-skip decision in any context.

## Verification

- Full `cdsi-engine` suite: sorted diff confirms zero change - this fix affects zero currently-tested unit scenarios, since no existing test drives an `UNNECESSARY`-status target dose through `EvaluateConditionalSkip`.
- Full FITS run, case-by-case diffed against the true baseline: **0 regressions, 11 improvements** (6 COVID-19, 5 PCV).

This closes part B of SPEC-4.6-0019 (the PCV `AGED_OUT` defect) as its own, independently-shippable fix. Part A of that finding (the DTaP clinical-correctness question) remains open pending ACIP input via [GitHub Issue #65](https://github.com/immregistries/StepIntoCDSI/issues/65).

## Affected

- Spec sections: 4.4 (page 35, the placeholder mechanism), 6.2 (page 51, the shared base class this fix lives in)
- Code locations: `EvaluateConditionalSkip.java`
- FITS cases: 11 improved, 0 regressed
