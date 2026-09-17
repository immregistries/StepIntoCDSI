# SPEC-4.6-0011: EvaluateAllowableInterval's placeholder table name and shared per-interval state

**Status:** confirmed (reviewed and merged by the project owner - see "Fix merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Two independent defects in the same class, both closing unit 6.6's remaining red tests:

1. `setConditionTableName("Table ")` - a placeholder never filled in, where 6.3 and 6.4 name their own attribute tables in full (e.g. "Table 6-14 Age Attributes").

2. `EvaluateAllowableInterval` declared `caDateAdministered`, `caAllowableIntervalElements`, and `caAbsoluteMinimumIntervalDate` as fields on the step class itself, then looped over every `AllowableInterval` on the target dose, calling `caAbsoluteMinimumIntervalDate.setInitialValue(...)` and constructing a new `LT` (`LogicTable`) per iteration - but each `LT`'s condition 0 closure read the *same* shared `caAbsoluteMinimumIntervalDate` field rather than a value captured at that iteration. By the time `process()` evaluated every table in the list, all of them answered against whichever interval's date was set last, not their own.

## Interpretation

Defect 1 is a pure transcription slip with no behavioural effect - the same shape as 6.5's own attribute-table-name defect (`theStepNamesTableSixSeventeenAsItsAttributeTable`), fixed the same way.

Defect 2 is a genuine per-iteration state bug. `EvaluatePreferableInterval` (6.5) avoids this exact mistake by giving each of its per-interval `LT` instances its own `ConditionAttribute` objects, constructed fresh inside the loop rather than shared on the outer step. The fix mirrors that pattern for the two attributes each `LT` actually evaluates against (date administered, absolute minimum interval date), passed into a new `LT(Date, Date)` constructor and held as `LT`-local fields that shadow the outer step's same-named fields.

Deliberately **not** changed: 6.6's own documented convention (confirmed against `EvaluateAllowableIntervalTest`'s class javadoc) of publishing Table 6-20's three attributes once in `getConditionAttributeList()` rather than per interval in `getConditionAttributesAdditionalMap()` the way 6.5 does. That's 6.6's own already-tested design choice, unrelated to this defect - the step-level fields and `conditionAttributesList` registration are untouched; only the per-table *evaluation* logic was given its own state.

Materiality: the bundled Supporting Data release's 484 series doses carry exactly one `allowableInterval` element each (465 bare/self-closing, 19 populated, no series dose with more than one populated interval), so defect 2 had no effect on the bundled release as it stands - a latent defect, not a live one, as unit 6.6's own step-tests notes already recorded before this fix.

## Fix merged

Reviewed and approved by the project owner, merged to `develop`.

```diff
-    setConditionTableName("Table ");
+    setConditionTableName("Table 6-20 Allowable Interval Attributes");
```

```diff
-        caAbsoluteMinimumIntervalDate
-            .setInitialValue(CALCDTINT_3.evaluate(dataModel, this, intervalFromAllowableInterval));
-
-        LT logicTable = new LT();
+        Date absoluteMinimumIntervalDate = CALCDTINT_3.evaluate(dataModel, this, intervalFromAllowableInterval);
+        caAbsoluteMinimumIntervalDate.setInitialValue(absoluteMinimumIntervalDate);
+
+        LT logicTable = new LT(aar.getDateAdministered(), absoluteMinimumIntervalDate);
```

```diff
   private class LT extends LogicTable {
+    private final ConditionAttribute<Date> caDateAdministered;
+    private final ConditionAttribute<Date> caAbsoluteMinimumIntervalDate;
     private YesNo result = null;

-    public LT() {
+    public LT(Date dateAdministered, Date absoluteMinimumIntervalDate) {
       super(1, 2,
           "Table 6 - 21 Did the vaccine dose administered satisfy the defined Allowable interval?");
+
+      caDateAdministered = new ConditionAttribute<Date>("Vaccine dose administered", "Date Administered");
+      caDateAdministered.setInitialValue(dateAdministered);
+      caAbsoluteMinimumIntervalDate = new ConditionAttribute<Date>("Calculated Date",
+          "Absolute Minimum Interval Date");
+      caAbsoluteMinimumIntervalDate.setAssumedValue(PAST);
+      caAbsoluteMinimumIntervalDate.setInitialValue(absoluteMinimumIntervalDate);
```

The outer step-level `caDateAdministered`/`caAbsoluteMinimumIntervalDate` fields, and their registration into `conditionAttributesList`, are unchanged - they still publish Table 6-20's attributes once, per 6.6's own convention. Only the `LT` instances now carry their own copies for evaluation, so each per-interval table checks its own interval's own absolute minimum date.

### Test verification

`EvaluateAllowableIntervalTest` (25 tests): before, 23 green / 2 red; after, **25 green / 0 red** - unit 6.6 is now fully closed. Full `cdsi-engine` suite: 767 tests, 185 failures, 1 error - down from 187 failures before this round, a reduction of exactly 2, matching the two tests above. No other test in the suite changed status in either direction.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (4896 fixtures), verified by `ReferenceSetVerifier`. `mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run so `cdsi-fits-tests` resolved the freshly-built jar.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | 3364 |
| Failed assertions | 1531 | 1531 |
| Execution errors | 1 | 1 |

`changed-cases.json` for the after-run reports `added: []`, `removed: []`, `statusChanged: []` against the immediately preceding run. Strict no-op on this fixture set, consistent with defect 2 being latent against the bundled Supporting Data (no series dose in the release carries more than one populated allowable interval) - the two JUnit tests, not FITS, are this round's evidence of corrected behaviour.

## Affected

- Spec sections: 6.6 (page 60)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.EvaluateAllowableInterval`
- FITS cases: none confirmed - no FITS case changed status or output in either direction (see FITS verification above)
