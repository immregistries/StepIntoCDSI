# SPEC-4.6-0017: Table 6-6's stale chapter label, and Table 6-8's exclusive interval lower bound

**Status:** confirmed (merged)
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

**Table 6-6's label.** LT66's constructor:

```java
super(1, 2, "Table 4-6 CONDITIONAL Type of Age - Is the Condition Met?");
```

The extracted spec text names this table "Table 6-6" throughout; "4-6" is a stale chapter number left from a prior renumbering. (LT68 next to it already said "Table 6 - 8" correctly, so this was not a global miss.)

**Table 6-8's interval condition.** The spec's second condition text: "Is the Conditional Skip Reference Date >= Conditional Skip Interval Date?" The code:

```java
if (intervalDate.before(referenceDate)) {
    return LogicResult.YES;
}
```

`intervalDate.before(referenceDate)` is strictly `referenceDate > intervalDate`, silently excluding the `>=` boundary the spec's own text requires.

## Interpretation

Both are the same shape as SPEC-4.6-0016: a genuine, narrow spec-vs-code mismatch, fixed, and verified to be a strict no-op against the current FITS fixture set.

**Fix:**

```java
super(1, 2, "Table 6-6 CONDITIONAL Type of Age - Is the Condition Met?");
```

```java
if (!intervalDate.after(referenceDate)) {
    return LogicResult.YES;
}
```

### Test verification

`EvaluateConditionalSkipForEvaluationTest` (44 tests): two reds close - `theAgeTableNamesItselfWithItsCurrentSpecificationNumber` and `theIntervalConditionIsMetWhenTheReferenceDateEqualsTheIntervalDate`. Full `cdsi-engine` suite: 771 tests, failures 169 -> 167 (down exactly 2), 1 error unchanged, no other test changed status in either direction.

### FITS verification

`mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run. Reference set `acip-4.6-sd-4.65-fits-222cc1c7`, 4896 fixtures.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3378 | 3378 |
| Failed assertions | 1517 | 1517 |
| Execution errors | 1 | 1 |

Case-by-case comparison (caseId + status + expectedHash + actualHash, sorted, strict diff): **0 lines differ**. Neither fix is exercised by the bundled fixture set in a way that changes any reported forecast.

## Why this finding stopped here

This round's investigation started from four of unit 6.2's eight red tests (see `cdsi-reference/step-tests/status.yaml`, unit `6.2`, reds (3), (4), (5), (8)). Reds (5) and (8) are this finding. Reds (3) and (4) - Table 6-9's "equal to" comparator and Table 6-6's age-window *lower* bound - are real mismatches of the identical shape, but each fix, tested alone, has a real effect on the FITS suite that turned out to trace back to a separate, already-documented, deliberately out-of-scope defect rather than being safely mergeable on its own. They are recorded as SPEC-4.6-0018 and SPEC-4.6-0019 instead of being bundled in here, so that this finding remains what it claims to be: a fully verified, zero-risk no-op.

## Affected

- Spec sections: 6.2 (pages 55-56)
- Code locations: `EvaluateConditionalSkip` (shared base class for 6.2, 7.1, 7.6)
- FITS cases: none - verified strict no-op (see FITS verification above)
