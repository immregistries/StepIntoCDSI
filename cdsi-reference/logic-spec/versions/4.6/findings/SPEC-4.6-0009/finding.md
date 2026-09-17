# SPEC-4.6-0009: EvaluateForAllowableVaccine's vaccine-type reference comparison and two mislabeled calculated-date rows

**Status:** confirmed (reviewed and merged by the project owner - see "Fix merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Two independent defects in the same class, both closing unit 6.9's remaining red tests:

1. Table 6-29's first condition asks whether the vaccine type of the dose administered is "the same as" the vaccine type of an allowable vaccine. `VaccineType.equals()` already defines "the same" as equal CVX codes. `EvaluateForAllowableVaccine`'s condition 0 instead compared with `vt == av.getVaccineType()` - a Java reference comparison - so two distinct `VaccineType` instances carrying the same CVX code were wrongly judged "not the same vaccine type".

2. The condition-attribute labels for the two calculated dates read `"Calculated data (CALCDTALLOW-1)"` and `"Calculated Data (CALCDTALLOW-2)"`; Table 6-28's own rows 4 and 5 name these `"Calculated date (CALCDTALLOW-1)"` / `"Calculated date (CALCDTALLOW-2)"` - "date", not "data", in either capitalization. The calculated values themselves were always correct; only the label text was wrong.

## Interpretation

Defect 1 is latent, not observable, against the bundled Supporting Data release: `DataModelLoader` and `GatherNecessaryData` both take every `VaccineType` out of one shared `cvxMap`, so every allowable vaccine entry and every administered dose already share one instance per CVX code - `==` and CVX-equality agree today. It's still a real defect against the domain model's own contract, and worth fixing before that sharing assumption is ever relaxed.

Defect 2 is a pure transcription slip with no behavioural effect - 6.6 and 6.8 both already use "Calculated date (CALCDT...)" for their own calculated-date labels, so this brings 6.9 in line with the same convention.

Both of unit 6.9's two red JUnit tests exercised exactly these: `sameVaccineTypeMeansTheSameCvxCode` (defect 1) and `tableSixTwentyEightLabelsTheCalculatedDateRowsAsTheSpecificationDoes` (defect 2). Neither defect was expected to move any FITS case, and none did.

## Fix merged

Reviewed and approved by the project owner, merged to `develop`.

Scope: both defects, in the same file, same round - deliberately bundled because they're independent, equally narrow, and don't touch any shared logic or state, unlike a causally-linked cluster that must be fixed together.

```diff
-      logicTable.caAllowableVaccineTypeBeginAgeDate = new ConditionAttribute<Date>(
-          "Calculated data (CALCDTALLOW-1)", "Allowable Vaccine Type Begin Age Date");
-      logicTable.caAllowableVaccineTypeEndAgeDate = new ConditionAttribute<Date>(
-          "Calculated Data (CALCDTALLOW-2)", "Allowable Vaccine Type End Age Date");
+      logicTable.caAllowableVaccineTypeBeginAgeDate = new ConditionAttribute<Date>(
+          "Calculated date (CALCDTALLOW-1)", "Allowable Vaccine Type Begin Age Date");
+      logicTable.caAllowableVaccineTypeEndAgeDate = new ConditionAttribute<Date>(
+          "Calculated date (CALCDTALLOW-2)", "Allowable Vaccine Type End Age Date");
```

```diff
-          if (vt == av.getVaccineType()) {
+          if (vt.equals(av.getVaccineType())) {
```

The second change defers to `VaccineType`'s own `equals()` (already null-safe via an `instanceof` check) rather than re-deriving CVX comparison by hand, so it stays correct against the entirely-empty `<allowableVaccine/>` shape the bundled release carries three of (HepB Dose 1, HepB Dose 2, Pertussis Dose 1), where `getVaccineType()` returns `null` - `anEmptyAllowableVaccineElementNeverMatches` (already green before this round) continues to pass.

### Test verification

`EvaluateForAllowableVaccineTest` (31 tests): before, 29 green / 2 red; after, **31 green / 0 red** - unit 6.9 is now fully closed. Full `cdsi-engine` suite: 767 tests, 188 failures, 1 error - down from 190 failures before this round, a reduction of exactly 2, matching the two tests above. No other test in the suite changed status in either direction.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (4896 fixtures), verified by `ReferenceSetVerifier`. `mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run so `cdsi-fits-tests` resolved the freshly-built jar.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | 3364 |
| Failed assertions | 1531 | 1531 |
| Execution errors | 1 | 1 |

`changed-cases.json` for the after-run reports `added: []`, `removed: []`, `statusChanged: []` against the immediately preceding run. Strict no-op on this fixture set, as expected going in - the two JUnit tests, not FITS, are this round's evidence of corrected behaviour.

## Affected

- Spec sections: 6.9 (page 68)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.EvaluateForAllowableVaccine`
- FITS cases: none confirmed - no FITS case changed status or output in either direction (see FITS verification above)
