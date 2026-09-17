# SPEC-4.6-0015: EvaluateVaccineConflict's live-virus conflict window was dead code - the first fix in this pass to actually move FITS

**Status:** confirmed (reviewed and merged by the project owner - see "Fix merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

CALCDTCONFLICT-1 and CALCDTCONFLICT-2 both anchor on "the date administered of the **previous** vaccine dose administered." CONFLICT-3 requires the current dose's date to be "on or after the conflict begin interval date" (inclusive) "and before the conflict end interval date" (exclusive).

Four defects, all in `EvaluateVaccineConflict.java`, all confirmed by Role A's own detailed test writeup before this round began:

1. **Wrong loop direction.** The constructor's per-previous-dose loop read `for (int i = dataModel.getSelectedAntigenAdministeredRecordPos() + 1; i < ...size(); i++)` - over a list 4.2 sorts ascending by date administered, this scans doses administered *after* the current one, not before. With a spec-shaped (chronologically ascending) history, the loop body essentially never runs against anything CALCDTCONFLICT-1/2 actually name.

2. **Copy-paste on the end interval.** `LT422.setIntervalDate()` computed both a "begin" and an "end" amount from `liveVirusConflict.getConflictBeginInterval()` - the same field, twice - never reading `getMinimalConflictEndInterval()` or `getConflictEndInterval()`, the two fields CALCDTCONFLICT-2 actually names. Begin and end dates always came out equal, so CONFLICT-3's window was always zero-width even on the rare occasion the loop reached it.

3. **Exclusive instead of inclusive begin boundary.** `LT422`'s condition read `caDateAdministered.getFinalValue().after(caConflictBeginIntervalDate...)` - strict `>`, excluding the begin date itself, where the spec says "on or after."

4. **Matching ignored the conflicting vaccine type.** `setIntervalDate()` matched a `LiveVirusConflict` entry on `getCurrentVaccineType()` alone - not also `getPreviousVaccineType()`, unlike `LT421`'s own condition two lines above it in the same file, which already matches on both. An impacted vaccine type with more than one distinct conflicting type/interval could silently pick the wrong one (the last matching entry in the Supporting Data's own list order).

## Interpretation

Four defects in the same small area of one class - none required new investigation to find, only to fix. The loop-direction and end-interval defects are the pair Role A's own materiality note already flagged as load-bearing ("Reds (2) through (9) all trace to two things in the same class"); the begin-boundary and multi-conflict-matching defects were smaller, found while rewriting the same methods to fix the first two, and fixed alongside since they're the same causal cluster.

The rewrite also replaces a manual amount/type switch-statement duplicate of `TimePeriod.getDateFrom()` with a direct call to it, removing (as a side effect) the already-documented "compound conflict intervals lose their second term" gap - though no case in the current fixture set exercises a compound interval, so this is confirmed correct by construction, not by a new failing test.

Determining whether a given previous dose counts as "no evaluation status or Valid" versus "an evaluation status that is not Valid" (CALCDTCONFLICT-2's branch condition) reuses `VaccineDoseAdministered.evaluatedAgainstTargetDose`, the link introduced in SPEC-4.6-0013 - set by every one of 6.10 Satisfy Target Dose's six outcomes, not only the satisfied one. Using `TargetDose.satisfiedByVaccineDoseAdministered` instead (which stays null for anything but a genuinely satisfied dose) would have left this branch just as unreachable as the original defect did. No new cross-step change was needed this round; SPEC-4.6-0013's own addition already covers it.

Fixed live by 8 of unit 6.7's 11 red tests. The remaining 3 (a presentation-only attribute-type label, and a separate, already-documented-as-latent `LT420` constructor side effect that only reaches a live code path when the antigen-administered-record list is empty - never true in a real engine run) are a different, unrelated defect in the same class, deliberately left for a later round.

## Fix merged

Reviewed and approved by the project owner, merged to `develop`.

```diff
-      for (int i = dataModel.getSelectedAntigenAdministeredRecordPos() + 1; i < dataModel
-          .getSelectedAntigenAdministeredRecordList().size(); i++) {
+      for (int i = 0; i < dataModel.getSelectedAntigenAdministeredRecordPos(); i++) {
```

```diff
-        if (liveVirusConflict.getCurrentVaccineType()
-            .equals(caCurrentVaccineType.getFinalValue())) {
-          Date dob = vaccineAdministered.getDateAdministered();
-          int beginAgeAmount = liveVirusConflict.getConflictBeginInterval().getAmount();
-          int endAgeAmount = liveVirusConflict.getConflictBeginInterval().getAmount();
-          ... (manual DAY/WEEK/MONTH/YEAR switch on both) ...
-          caConflictBeginIntervalDate.setInitialValue(beginIntervalDate);
-          caConflictEndIntervalDate.setInitialValue(endIntervalDate);
-        }
+        if (liveVirusConflict.getCurrentVaccineType().equals(caCurrentVaccineType.getFinalValue())
+            && liveVirusConflict.getPreviousVaccineType().equals(vaccineAdministered.getVaccineType())) {
+          Date previousDateAdministered = vaccineAdministered.getDateAdministered();
+          Date beginIntervalDate = liveVirusConflict.getConflictBeginInterval()
+              .getDateFrom(previousDateAdministered);
+          Date endIntervalDate = isPreviousDoseValidOrUnevaluated(vaccineAdministered)
+              ? liveVirusConflict.getMinimalConflictEndInterval().getDateFrom(previousDateAdministered)
+              : liveVirusConflict.getConflictEndInterval().getDateFrom(previousDateAdministered);
+          caConflictBeginIntervalDate.setInitialValue(beginIntervalDate);
+          caConflictEndIntervalDate.setInitialValue(endIntervalDate);
+        }
```

```diff
-          if (caDateAdministered.getFinalValue()
-              .after(caConflictBeginIntervalDate.getFinalValue())
+          if (!caConflictBeginIntervalDate.getFinalValue()
+              .after(caDateAdministered.getFinalValue())
               && caDateAdministered.getFinalValue()
                   .before(caConflictEndIntervalDate.getFinalValue())) {
```

### Test verification

`EvaluateVaccineConflictTest` (25 tests): before, 14 green / 11 red; after, **22 green / 3 red** - 8 tests flip to green. Full `cdsi-engine` suite: 770 tests, 171 failures, 1 error - down from 179 failures before this round, a reduction of exactly 8. No other test in the suite changed status in either direction; `EvaluateAllowableIntervalTest`, `EvaluatePreferableIntervalTest`, and `SatisfyTargetDoseTest` (all touched indirectly, via `evaluatedAgainstTargetDose` or shared conflict/interval infrastructure) are byte-for-byte unchanged.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (4896 fixtures), verified by `ReferenceSetVerifier`. `mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run so `cdsi-fits-tests` resolved the freshly-built jar.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | **3378** |
| Failed assertions | 1531 | **1517** |
| Execution errors | 1 | 1 |

Case-by-case comparison (`results.jsonl`, not just aggregate counts): **0 regressions, 14 cases flip from FAIL to PASS** - 3 distinct scenarios (`MMR-2013-0554`, `MMR-2013-0556`, `VAR-2013-0831`), each appearing across 4-5 test-plan snapshots.

`MMR-2013-0554` ("Dose 1 to 2 MMRV interval 28-4 days", from the "HISTORICAL: CDSi Test Cases: Age-Based Routine Childhood, Adolescent, and Adult Recommendations" plan - a standard, non-risk scenario) is representative: two CVX 94 (MMRV) doses 24 days apart.

| | Before | After | Expected |
| --- | --- | --- | --- |
| `status` | COMPLETE | NOT_COMPLETE | NOT_COMPLETE |
| `earliestDate` | null | 2026-09-29 | 2026-09-29 |
| `recommendedDate` | null | 2029-07-08 | 2029-07-08 |

Before the fix, the too-soon second dose was wrongly treated as a valid, series-completing dose. After, the conflict is correctly detected, the dose doesn't count, and the forecast matches FITS's expectation exactly.

## Affected

- Spec sections: 6.7 (page 62)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.EvaluateVaccineConflict`
- FITS cases: `MMR-2013-0554`, `MMR-2013-0556`, `VAR-2013-0831` (14 total across test-plan snapshots) - all flip FAIL to PASS
