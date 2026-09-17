# SPEC-4.6-0032: Table 6-7's series-group completion check was hardcoded No

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Table 6-7's single condition - "Does the Conditional Skip Series Group identify a Series Group with at least one relevant patient series with a patient series status of 'Complete'?" - was implemented as:

```java
setLogicCondition(0, new LogicCondition(
    "Does the Conditional Skip Series Group identify a Series Group with at least one relevant patient series with a patient series status of 'Complete'?") {
  @Override
  public LogicResult evaluateInternal() {
    return LogicResult.NO;
  }
});
```

Two gaps sat behind this. `ConditionalSkipCondition` had no field to hold which series group(s) a condition names, and `DataModelLoader.readCondition` ignored the Supporting Data's `<seriesGroups>` element entirely - even the "left half" of the question (which group to check) was not representable in the domain model. The bundled release defines **25 such conditions** across HepB (7), Pneumococcal (16), and Polio (2), every one inside an AND set where this always-No condition defeated the whole set.

## Fix

- `ConditionalSkipCondition.getSeriesGroupSet()` - a `Set<String>`, mirroring the existing `vaccineTypeSet` pattern.
- `DataModelLoader.readCondition` now parses `<seriesGroups>` (semicolon-delimited, same convention as `<vaccineTypes>`; the bundled release only ever uses a single value, "1", but the parsing supports more).
- Table 6-7's condition now scans the current antigen's tracked patient series for any with `PatientSeriesStatus.COMPLETE` whose own series group (via the existing `SelectBestPatientSeries.seriesGroupOf` helper) is named by the condition - or any `COMPLETE` series at all when the condition names no group, matching the "empty filter matches everything" convention already used for required-gender filtering in 5.1 (and matching the fixture, which never sets a group on its `ConditionalSkipCondition` but still expects a match).

## A wrong turn worth recording: which list to scan

The first attempt scanned `dataModel.getSelectedPatientSeriesList()`. That looked right at a glance - the same series-group-scoped list Chapter 8 uses - but reading `SelectNextSeriesGroup.java` directly showed it's populated only *after* 4.4 finishes evaluating every series for the current antigen, and only by Chapter 8 (which runs once per antigen, after 4.4 hands off). 6.2 runs *during* that same per-series evaluation loop - before Chapter 8 has run for this antigen at all. At 6.2's own execution time, `getSelectedPatientSeriesList()` still holds whatever the *previous* antigen's Chapter 8 pass left in it - stale, unrelated data.

The list that's actually live and correctly antigen-scoped at 6.2's execution time is `dataModel.getPatientSeriesStepper().getList()` - 4.4's own full per-antigen series list, populated once by 4.3 and iterated one series at a time by 4.4, with each series' `PatientSeriesStatus` already set (by 7.2/7.4's `DetermineForecastNeed`) once that series finishes its own forecast earlier in the same loop. Switched to that list, filtered to the current antigen via a null-safe `Objects.equals` (the unit test's own fixture sets neither a target disease on its `AntigenSeries` nor a current antigen on the `DataModel`, so a plain `.equals()` would NPE).

## Zero materiality today - verified directly, not assumed

A scratch diagnostic (removed before this finding was written) drove every PCV, HepB, and Polio FITS case through the real pipeline and checked how many relevant patient series each one ended up with: **not one of them ever has more than one.** The cross-series scenario Table 6-7 exists to check - "has some *other* series in my own series group already completed?" - can't arise unless a patient has more than one relevant series for the same antigen in the first place, and that never happens anywhere in the current fixture set (5.1's Risk-series relevance determination decides that, not anything in this unit).

## Verification

- Unit 6.2's own suite gains `tableSixSevenIsMetWhenTheSeriesGroupHasACompleteRelevantPatientSeries`.
- Full `cdsi-engine` suite: sorted diff confirms exactly this one test flipped and nothing else.
- Full FITS run: 3616 passed both before and after, 0 changed cases - confirmed directly (see above), not assumed. Same category as SPEC-4.6-0026's CALCDT-5 fix and SPEC-4.6-0030's NPE guard: a genuine correctness fix the current fixture set doesn't happen to exercise.

## Affected

- Spec sections: 6.2 (page 56, Table 6-7)
- Code locations: `EvaluateConditionalSkip.java` (`LT67`), `ConditionalSkipCondition.java`, `DataModelLoader.java` (`readCondition`)
- FITS cases: none currently exercise the multi-relevant-series-per-antigen scenario this fixes
