# SPEC-4.6-0003: EvaluateGender's rejection outcome is dead code

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`EvaluateGender.evaluateInternal()`:

```java
for (RequiredGender requiredGender : caRequiredGender.getFinalValue()) {
  if (requiredGender.getValue().contains(caGender.getFinalValue())) {
    return LogicResult.YES;
  }
}
return LogicResult.YES;  // unconditional, even when nothing in the loop matched
```

The final `return LogicResult.YES;` executes even when the loop found no match. The class's own inner `LT` class documents a "No, patient's gender is not one of the required genders... Evaluation Reason is 'incorrect gender'" outcome, which this makes permanently unreachable.

Two smaller issues in the same class: it cites `"Table 4-31"` in `setConditionTableName`/the `LT` constructor, but Chapter 4 of the Logic Specification only has Tables 4-1 through 4-3; and its log message reads "Setting next step: 4.10 Satisfy Target Dose" where the real next step, per `LogicStepType`, is 6.10.

## Interpretation

`EvaluateGender`'s `LogicStepType` (`EVALUATE_GENDER`) is declared with placeholder section label `"xx"`, not a real specification subsection number - it has no corresponding text in the Logic Specification and so was never given a step package under `logic-spec/versions/4.6/steps/`. It surfaced instead via Phase 8's mapping validator, which checks every class `LogicStepFactory` can instantiate against `mappings/spec-to-code.yaml` (see that file's `unmapped_classes` section).

The bug itself is unambiguous and independent of the missing-section issue: the gender-mismatch rejection path can never execute today. Tracked, not fixed, per standing project direction (demonstration system; dedicated fix pass planned later).

## Affected

- Spec sections: none - `EvaluateGender` has no corresponding specification subsection
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.EvaluateGender`
- FITS cases: none identified in this pass
