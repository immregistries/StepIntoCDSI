# SPEC-4.6-0008: EvaluateForPreferableVaccine excludes the begin age date itself from its own inclusive window

**Status:** confirmed (reviewed and merged by the project owner on 2026-09-08 - see "Fix merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Table 6-26's second condition reads: "Is the preferable vaccine type begin age date <= date administered < preferable vaccine type end age date?" - an inclusive lower bound, exclusive upper bound.

`EvaluateForPreferableVaccine.java`'s condition index 1 (`evaluateInternal()`) implements this as:

```java
if (caVaccineTypeBeginAgeDate.getFinalValue().before(caDateAdministered.getFinalValue())
    && caDateAdministered.getFinalValue()
        .before(caVaccineTypeEndAgeDate.getFinalValue())) {
```

The first half uses strict `Date.before()`, which is `false` when the two dates are equal - so a dose administered exactly on the begin age date is excluded from a window the condition's own label says it belongs in. The second half (strict `.before()` against the end age date) already matches the label's exclusive upper bound and is correct as written.

## Interpretation

This reads as a copy-paste/off-by-one slip on the lower-bound half of a two-sided range check, not a deliberate design choice - the condition's own inline specification text says "<=" for exactly the comparison implemented as strict "<".

The codebase already has an established, correct idiom for this exact shape of check: `EvaluateAge.java` uses `!absoluteMinimumDate.after(dateAdministered)` and `!minimumDate.after(administeredDate)` at two call sites for the same "date A is on or after date B" comparison.

Two of unit 6.8's five red JUnit tests exercise exactly this boundary:
- `ruleOneIncludesTheBeginAgeDateItself` - a dose administered on the begin age date itself.
- `aDoseAdministeredOnTheDateOfBirthIsInsideAZeroDayBeginAgeWindow` - a "0 days" begin age window, where the begin age date equals the date of birth - the boundary case most likely to occur on real data (a Hep B birth dose).

Both assert `LogicResult.YES` for condition index 1 and, before the fix, got `NO`.

This finding does not claim FITS impact: no FITS case naming this defect was found during investigation, and the full-suite before/after comparison below shows the fix is a no-op against the current 4896-case fixture set. The two affected JUnit tests are this round's evidence of corrected behaviour, independent of FITS.

## Fix merged

Reviewed and approved by the project owner on 2026-09-08, commit `cb1f5ce` on `develop`.

Scope: **only** the begin-date half of condition index 1. The end-date half of the same condition, and condition index 2 (the trade-name check - a separate, already-known, deliberately deferred limitation), were deliberately left untouched.

The change is one line, in `EvaluateForPreferableVaccine.java`'s condition index 1:

```java
-          if (caVaccineTypeBeginAgeDate.getFinalValue().before(caDateAdministered.getFinalValue())
+          if (!caVaccineTypeBeginAgeDate.getFinalValue().after(caDateAdministered.getFinalValue())
```

This makes the condition match its own documented "<=" inclusive lower bound, using the exact idiom (`!x.after(y)`) already used twice elsewhere in the codebase (`EvaluateAge.java`) for the same kind of comparison. Nothing else in the file was changed.

### Test verification

`EvaluateForPreferableVaccineTest` (30 tests): before the fix, 25 green / 5 red; after, 27 green / 3 red. The two tests this fix targets - `ruleOneIncludesTheBeginAgeDateItself` and `aDoseAdministeredOnTheDateOfBirthIsInsideAZeroDayBeginAgeWindow` - both flip from red to green. The remaining 3 red tests (`ruleFiveEvaluatesTheTradeNameOfTheVaccineDoseAdministered`, `ruleFiveReportsNotPreferableWhenTheTradeNameDiffers`, `tableSixTwentyFiveNamesTheCalculatedDatesAsTheSpecificationDoes`) are pre-existing, unrelated defects (the trade-name check and a presentation-only label swap), unaffected by this change.

Full `cdsi-engine` suite: 767 tests, 190 failures, 1 error - down from 192 failures before the fix (767/192/1), a reduction of exactly 2, matching the two tests above. No other test in the suite changed status in either direction.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-222cc1c7` (Logic Specification 4.6, Supporting Data 4.65, 4896 fixtures), verified by `ReferenceSetVerifier` at the start of the run. `mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run, per the lesson recorded in Phase 22's before/after record, so `cdsi-fits-tests` resolved the freshly-built jar rather than a stale one from the local Maven repository.

| | Before (`9ccbf22`) | After (`2f6eb4e`, fix applied uncommitted) |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | 3364 |
| Failed assertions | 1531 | 1531 |
| Execution errors | 1 | 1 |

- Run bundles: `2026-09-07T165702-910277600Z-9ccbf22-...` (before) and `2026-09-08T002144-342928800Z-2f6eb4e-...` (after), under `cdsi-fits-tests/target/fits-runs/`.
- The after-run's own `changed-cases.json` reports `added: []`, `removed: []`, `statusChanged: []`.

So the fix is a strict no-op on this fixture set: it does not flip any FITS case, and it regresses none. The unit test, not FITS, is what demonstrates the corrected behaviour - consistent with this fixture set's coverage gap (per the finding's Materiality note in `cdsi-reference/step-tests/status.yaml` unit 6.8, the "0 days" begin-age boundary is exercised by 53 of the bundled release's 1089 `preferableVaccine` entries, but that does not guarantee any FITS *case* actually administers a dose on that exact date).

## Affected

- Spec sections: 6.8 (page 65)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.EvaluateForPreferableVaccine`
- FITS cases: none confirmed - no FITS case changed status or output in either direction (see FITS verification above)
