# SPEC-4.6-0013: CALCDTINT-8 was dead code, then self-referencing - the full story of fixing "most recent dose of a named vaccine type"

**Status:** confirmed (reviewed and merged by the project owner - see "Fix merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`Interval.getPatientReferenceDoseDate()`'s CALCDTINT-8 block read:

```java
if (!previousVdaEvaluation.getEvaluationReason().equals(EvaluationReason.INADVERTENT_ADMINISTRATION)) {
```

with no null check, where `previousVdaEvaluation` is the evaluation of whichever dose satisfied the *previous target dose in the series* - a variable already in scope for CALCDTINT-1/2's own, unrelated purpose. A plainly valid dose has no evaluation reason (`null`), so this threw `NullPointerException` on essentially every real invocation, was silently swallowed by the method's own broad `catch (NullPointerException np)`, and left the reference date `null` - falling back to Table 6-17's assumed 01/01/1900 every time.

Separately, `DataModelLoader.readSeriesDose`'s interval-parsing branch had no case for `<fromMostRecent>` at all - it was silently dropped - and `Interval.fromMostRecentVaccineType` was typed as a single `VaccineType`, while the real Supporting Data always encodes this element as a semicolon-delimited CVX list (COVID-19's own `<fromMostRecent>208; 210; 211; ...517</fromMostRecent>`, 33 codes; 54 of the release's 55 populated occurrences are multi-code lists, only 1 names a single code).

## Interpretation

This finding's own history is part of its evidence - recorded in full rather than smoothed into only the final diff, because the dead ends are exactly what makes the final result trustworthy.

**First pass.** Added the missing null check, multi-CVX list support (loader parsing, `Interval.fromMostRecentVaccineTypeList`, matching against any code in the list, explicitly tracking the latest date rather than relying on "whichever matches last in list order"). Unit tests for this rule went green. A full FITS run then surfaced a real, reproducible **20-case regression** - confined to Meningococcal and Pneumococcal, exactly the two groups this rule affects - alongside 13 cases that appeared to newly pass.

**Root-causing the regression.** Tracing FITS case `MCV-2023-0106` ("Patient... has been administered an extraneous dose of the Meningococcal vaccine") by direct engine inspection (a temporary diagnostic run dumping every administered dose's evaluation state, deleted before this fix was committed) showed the second, too-soon dose's actual evaluation was `NOT_VALID`/`TOO_SOON` - **not** `EXTRANEOUS`, which is reserved specifically for a dose given after the maximum age. The fixture's plain-English "extraneous" described the clinical concept, not that specific engine enum value. An exclusion check written against `EvaluationStatus.EXTRANEOUS` therefore had **zero effect** - confirmed by re-running the full FITS suite and finding the regression completely unchanged, byte-for-byte.

**The actual defect.** CALCDTINT-8's search over `dataModel.getAntigenAdministeredRecordList()` (the patient's whole history, already fully built before any antigen is evaluated) had no lower bound - nothing prevented a dose from matching **itself** as its own "most recent" reference. For `MCV-2023-0106`, target dose 2 was being evaluated against the second dose (2026-09-01) itself; with no date bound, that same dose was found as "the most recent dose of type 316/328", giving a reference date of 2026-09-01, and CALCDTINT-8's own 6-month `minInt` produced 2027-03-01 - exactly the wrong value observed. Adding an explicit "must be strictly before the dose currently being evaluated" bound fixed this, confirmed again by direct engine inspection (CVX 108's forecast changed from `Not Complete`/2027-03-01 to `Complete`, matching FITS exactly) before re-running the full suite.

**The evaluation-status/reason exclusion was kept**, even though it wasn't the cause of this particular regression - it's still a correct, spec-grounded exclusion (CALCDTINT-1 already establishes the same `EXTRANEOUS`/`INADVERTENT_ADMINISTRATION` exclusion precedent for its own, different reference-date rule). Implementing it required a genuine cross-cutting addition: `TargetDose.satisfiedByVaccineDoseAdministered` / `VaccineDoseAdministered.getTargetDose()` are only ever populated on 6.10 Satisfy Target Dose's `SATISFIED` outcome - `IdentifyOnePrioritizedPatientSeries`, `GenerateForecastDatesAndRecommendedVaccines`, `CONDSKIP_1`, and `BusinessRuleTable` all rely on that null-ness as their own signal for "this dose was not satisfied", so reusing those fields for a general "which target dose was this dose ever evaluated against" lookup would have silently broken them. A new, separate field - `VaccineDoseAdministered.evaluatedAgainstTargetDose` - is set in every one of Satisfy Target Dose's six outcomes instead, leaving the existing fields' semantics completely untouched.

This is the one part of this round's fix that reaches into a different unit's class (6.10 `SatisfyTargetDose`). It was proposed and **explicitly approved by the project owner** before being made, per the standing rule that a unit's own Role B round does not touch another unit's code unilaterally.

**Net result.** With both fixes in place, the full FITS suite is a strict no-op against the current 4896-case fixture set: 0 regressions, and - worth stating plainly rather than glossing over - the 13 cases that appeared to improve during the intermediate (buggy, self-referencing) state **no longer do**. That improvement was itself an artifact of the self-reference bug coincidentally producing a value FITS's expectation happened to match, not a genuine fix. The corrected CALCDTINT-8 is now provably right on real Supporting Data (55 populated intervals across COVID-19, Pneumococcal, Pertussis, Zoster and Meningococcal, previously always silently ignored) without moving any FITS case in either direction.

## Fix merged

Reviewed and approved by the project owner, merged to `develop`. Five files:

- `DataModelLoader.java` - parses `<fromMostRecent>` as a semicolon-delimited CVX list, resolving each code via the existing `supportingDataModel.getCvx(code)` lookup, into a `List<VaccineType>`.
- `Interval.java` - `fromMostRecentVaccineType` (singular) → `fromMostRecentVaccineTypeList`; CALCDTINT-8 matches against any code in the list, bounds candidates to strictly before the dose currently being evaluated, tracks the latest date explicitly, and excludes a candidate whose `evaluatedAgainstTargetDose` evaluation is `EXTRANEOUS` or `INADVERTENT_ADMINISTRATION`.
- `VaccineDoseAdministered.java` - new `evaluatedAgainstTargetDose` field/accessors, deliberately separate from the existing `targetDose` field.
- `SatisfyTargetDose.java` - all six outcomes now set `evaluatedAgainstTargetDose`, not just the `SATISFIED` one; no other behavior changed.
- `EvaluatePreferableIntervalTest.java` - the two existing CALCDTINT-8 tests updated for the list-typed field; three new tests added (multi-code match, inadvertent-administration exclusion, extraneous-evaluation exclusion - the last one a direct regression test for `MCV-2023-0106`).

### Test verification

`EvaluatePreferableIntervalTest` (30 tests, up from 27 - three new tests added this round): 28 green / 2 red - the 2 red are the still-deferred CALCDTINT-9 tests, untouched by this round. Full `cdsi-engine` suite: 770 tests, 180 failures, 1 error - down from 182 failures before this round, a reduction of exactly 2, matching the two CALCDTINT-8 tests (`calcdtintEightMeasuresFromTheMostRecentDoseOfANamedVaccineType`, `theSupportingDatasFromMostRecentVaccineTypeReachesTheInterval`) flipping from red to green; the three newly-added tests are all green, adding to the total without adding failures. No other test in the suite changed status in either direction; `SatisfyTargetDoseTest` (25/3) is byte-for-byte unchanged, confirming the additive `evaluatedAgainstTargetDose` wiring didn't disturb 6.10's own behavior.

### FITS verification

All runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (4896 fixtures), verified by `ReferenceSetVerifier`. `mvn -pl cdsi-engine install -DskipTests` was run before every FITS run in this investigation so `cdsi-fits-tests` always resolved the freshly-built jar.

| | Before this round | After (final, corrected) |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | 3364 |
| Failed assertions | 1531 | 1531 |
| Execution errors | 1 | 1 |

Direct case-by-case comparison (`results.jsonl`, not just the aggregate counts) confirms **0 cases changed status in either direction** between the true pre-round baseline and the final, corrected fix - a strict no-op, verified after the intermediate buggy state's 20 regressions / 13 spurious improvements were fully resolved.

## Affected

- Spec sections: 6.5 (page 57)
- Code locations: `Interval`, `DataModelLoader`, `SatisfyTargetDose`, `VaccineDoseAdministered`
- FITS cases: none - verified strict no-op on the current fixture set (see FITS verification above)
