# SPEC-4.6-0012: EvaluatePreferableInterval's misassigned evaluation reason, placeholder table name, and misspelled attribute name

**Status:** confirmed (reviewed and merged by the project owner - see "Fix merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Three independent transcription-level defects in the same class, closing three of unit 6.5's seven red tests:

1. Table 6-18 Rule 1's outcome text: "No. The vaccine dose administered did not satisfy the preferable interval for the target dose. Evaluation reason is 'Too Soon'." `EvaluatePreferableInterval`'s outcome 0 `perform()` logs that exact line, then calls `evaluation.setEvaluationReason(EvaluationReason.GRACE_PERIOD)` - the reason belonging to outcome 1 (Rule 2), not outcome 0 (Rule 1).

2. `setConditionTableName("Table ")` - a placeholder never filled in, where 6.3 and 6.4 name their own attribute tables in full.

3. Table 6-17 row 4 is named "Minimum Interval Date" in the specification; the step registers it as "Mimium Interval Date" - the attribute name only; the attribute type and the calculated value itself are correct.

## Interpretation

All three are transcription-level slips with no shared root cause, bundled into one round because each is independently narrow and none touches the other's code path.

Defect 1 is a copy-paste artifact: outcome 0's own log line already states the correct reason ('Too Soon'), only the enum value passed to `setEvaluationReason` was wrong - the same shape 6.6's own single-condition case gets right (`EvaluationReason.TOO_SOON`).

Defects 2 and 3 mirror the identical defects already fixed in 6.6 (SPEC-4.6-0011) and 6.9 (SPEC-4.6-0009) respectively - the same category of oversight recurring across independently-written step classes, fixed the same way each time.

Three of unit 6.5's seven red JUnit tests exercised exactly these: `ruleOneRecordsEvaluationReasonTooSoon` (defect 1), `theStepNamesTableSixSeventeenAsItsAttributeTable` (defect 2), and `tableSixSeventeenNamesItsAttributesAsTheSpecificationDoes` (defect 3).

**The other four red tests in this unit are a separate, materially significant cluster, deliberately not touched in this round:** `calcdtintEightMeasuresFromTheMostRecentDoseOfANamedVaccineType`, `calcdtintNineMeasuresFromTheMostRecentMatchingPatientObservation`, `theSupportingDatasFromMostRecentVaccineTypeReachesTheInterval`, and `theSupportingDatasFromRelevantObservationCodeReachesTheInterval`. CALCDTINT-8 and CALCDTINT-9 are each blocked twice over - once in the Supporting Data loader (which never parses `<fromMostRecent>`/`<fromRelevantObs>` at all), once in the reference-date rule body (a missing null check for CALCDTINT-8, and a branch that never assigns a date at all for CALCDTINT-9) - and together they affect 61 real populated intervals in the bundled release, spanning COVID-19, Pneumococcal, Pertussis, Zoster, Meningococcal, Hib, Measles, Mumps, RSV and Rubella. That's a bigger, multi-file fix with real clinical materiality, so it's being scoped and reviewed as its own round rather than folded in here.

This finding does not claim FITS impact - all three defects here are pure presentation/enum-selection slips - and the full-suite before/after comparison below confirms the fix is a no-op against the current 4896-case fixture set.

## Fix merged

Reviewed and approved by the project owner, merged to `develop`.

```diff
-          log("No. The vaccine dose administered did not satisfy the preferable interval for the target dose. Evaluation reason is 'Too Soon'.");
-          Evaluation evaluation = dataModel.getTargetDose().getEvaluation();
-          evaluation.setEvaluationReason(EvaluationReason.GRACE_PERIOD);
+          log("No. The vaccine dose administered did not satisfy the preferable interval for the target dose. Evaluation reason is 'Too Soon'.");
+          Evaluation evaluation = dataModel.getTargetDose().getEvaluation();
+          evaluation.setEvaluationReason(EvaluationReason.TOO_SOON);
```

```diff
-    setConditionTableName("Table ");
+    setConditionTableName("Table 6-17 Preferable Interval Attributes");
```

```diff
-      logicTable.caMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Mimium Interval Date");
+      logicTable.caMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date", "Minimum Interval Date");
```

### Test verification

`EvaluatePreferableIntervalTest` (27 tests): before, 20 green / 7 red; after, **23 green / 4 red** - the three targeted tests flip to green; the remaining 4 red tests are exactly the CALCDTINT-8/9 cluster described above, untouched by this round. Full `cdsi-engine` suite: 767 tests, 182 failures, 1 error - down from 185 failures before this round, a reduction of exactly 3. No other test in the suite changed status in either direction.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (4896 fixtures), verified by `ReferenceSetVerifier`. `mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run so `cdsi-fits-tests` resolved the freshly-built jar.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | 3364 |
| Failed assertions | 1531 | 1531 |
| Execution errors | 1 | 1 |

`changed-cases.json` for the after-run reports `added: []`, `removed: []`, `statusChanged: []` against the immediately preceding run. Strict no-op on this fixture set, as expected for three transcription-level fixes with no clinical effect.

## Affected

- Spec sections: 6.5 (page 57)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.EvaluatePreferableInterval`
- FITS cases: none confirmed - no FITS case changed status or output in either direction (see FITS verification above)
