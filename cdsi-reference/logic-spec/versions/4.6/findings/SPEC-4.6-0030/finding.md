# SPEC-4.6-0030: 6.1's rejecting outcomes NPE on a target dose's first evaluation

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Table 6-3's Rule 1 ("date administered > lot expiration date") and Rule 2 ("dose condition flag is 'Y'") both require the outcome "Evaluation status is 'sub-standard'". The code wrote this as:

```java
dataModel.getTargetDose().getEvaluation().setEvaluationStatus(EvaluationStatus.SUB_STANDARD);
```

`TargetDose.getEvaluation()` returns the last element of an internal `evaluationList`, or `null` if that list is empty. 4.4 (`EvaluateAndForecastAllPatientSeries.setupCurrentPatientSeries()`) builds every `TargetDose` fresh via `new TargetDose(seriesDose)`, which starts with an empty evaluation list - and 6.1 is the *first* evaluation step run against a newly administered dose. Nothing between 4.4 and 6.1 attaches an `Evaluation`; 6.4 `EvaluateAge` is the first step in the pipeline that does, and even it only asserts non-null *after* its own table has already run. So on the very first vaccine dose administered against a fresh target dose, `getEvaluation()` returns `null` and both rejecting outcomes throw `NullPointerException`.

This was already precisely diagnosed in `status.yaml`'s pre-existing notes, alongside a from-scratch rewrite of this unit's test class (the original `@RunWith(Parameterized.class)` harness asserted nothing at all, due to an unrelated field-shadowing bug - see the rewritten `EvaluateDoseAdministeredConditionTest`'s own javadoc for that history). The two new tests pinning this NPE both start from a target dose with `getEvaluation() == null`, expect no exception, and expect `EvaluationStatus.SUB_STANDARD` readable afterward. The existing tests that pre-attach an `Evaluation` before calling `run()` continued to pass throughout - confirming the `SUB_STANDARD` write itself was always correct; only the object it wrote to was sometimes missing.

Two independent reasons this has never actually crashed in the FITS suite or via cdsi-web: nothing in `cdsi-engine` or `cdsi-web` ever calls `Vaccine.setLotExpirationDate`, and `ForecastInput.VaccinationInput` has no lot-expiration field at all, so the assumed far-future date always applies and Rule 1 can never fire; and every one of the bundled FITS fixtures records `"doseCondition": null`, so Rule 2 never fires either.

## Fix

Lazily create an `Evaluation` and attach it to the target dose (`targetDose.setEvaluation(new Evaluation())`) immediately before setting its status, only when `getEvaluation()` is currently `null`. When one is already attached, the existing mutate-in-place behavior is unchanged - both outcomes still write to whatever `getEvaluation()` returns, so Rule 3's "adds no evaluation" guarantee (asserted by its own, already-passing test) and every other already-passing test are unaffected.

## Verification

- Unit 6.1's own suite (`EvaluateDoseAdministeredConditionTest`): 15/15 green (up from 13/15).
- Full `cdsi-engine` suite: 125 failures/1 error (down from 127/1) - sorted diff confirms exactly the 2 targeted tests flipped and nothing else.
- Full FITS run: 3511 passed both before and after, 0 changed cases - exactly as predicted, since neither rejecting rule is reachable through any current production or FITS input path (see evidence above). A genuine crash-prevention fix for a currently-unreachable path, not a behavior change for any case the current fixture set or cdsi-web's input plumbing can produce - the same category as SPEC-4.6-0026's CALCDT-5 fix.

## Affected

- Spec sections: 6.1 (pages 46-47, Table 6-3)
- Code locations: `EvaluateDoseAdministeredCondition.java`
- FITS cases: none currently exercise either rejecting rule
