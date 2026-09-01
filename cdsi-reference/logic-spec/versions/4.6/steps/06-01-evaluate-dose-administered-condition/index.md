# 6.1 Evaluate Dose Administered Condition

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 46-47. Figure 6-2 (Vaccine Dose Administered Condition Process Model), Table 6-2 (Dose Administered Condition Attributes), Table 6-3 (Can the Vaccine Dose Administered be Evaluated? - decision table). Business rule CALCDTLOTEXP-1.

## Purpose

**[SPEC]** "Evaluate Dose Administered Condition checks the dose administered to see if the target dose must be repeated regardless of the other evaluation rules." Doses administered after their lot expiration date, or flagged with a "condition" (misadministration, recall, cold chain break), don't need further evaluation.

## Entry Conditions

**[SPEC]** First evaluation step for a vaccine dose administered against a target dose (Table 6-1, Figure 6-1) - runs before any of 6.2-6.10.

## Inputs and Attributes

**[SPEC]** Table 6-2 Dose Administered Condition Attributes: Date Administered, Dose Condition Flag (both from the vaccine dose administered), and a calculated Lot Expiration Date (CALCDTLOTEXP-1, assumed `12/31/2999` if empty).

**[IMPLEMENTATION]** Matches: `caDateAdministered`, `caDoseCondition`, `caLotExpirationDate` with the same assumed value (`LogicStep.FUTURE`).

## Business Rules

**[SPEC]** CALCDTLOTEXP-1 is referenced as producing the Lot Expiration Date, but the specification text for this rule's calculation isn't included in this section (no Table 6-11-style rule-text table appears for it here).

**[IMPLEMENTATION]** Not independently calculated in this class - the code comment says outright: `// use CALCDTLOTEXP-1 business rules now (not implemented yet), change attribute type to "Calculated date"`. The value is read directly from `AntigenAdministeredRecord.getLotExpirationDate()`, which must already be resolved elsewhere (upstream, when the record is built) rather than computed here. **This is a real, code-comment-confirmed gap**, not an inference.

## Decision Tables

**[SPEC]** Table 6-3 Can the Vaccine Dose Administered be Evaluated?

| Condition | Rule 1 | Rule 2 | Rule 3 |
| --- | --- | --- | --- |
| Date administered > lot expiration date? | Yes | No | No |
| Is the dose condition flag 'Y'? | - | Yes | No |
| **Outcome** | Cannot be evaluated (sub-standard) | Cannot be evaluated (sub-standard) | Can be evaluated |

## State Changes

**[IMPLEMENTATION]** `EvaluateDoseAdministeredCondition$LT`'s three outcomes: Rules 1/2 both set `TargetDoseStatus.NOT_SATISFIED` and log "sub-standard" (the spec's evaluation status for this case isn't a formal `EvaluationStatus` enum value in the code - it's expressed only via `TargetDoseStatus`, not mirrored into `Evaluation.setEvaluationStatus()`, worth a reviewer's attention since Table 6-3 explicitly names an "Evaluation status is 'sub-standard'" outcome the code doesn't appear to set on the `Evaluation` object itself). Rule 3 makes no state change - it just proceeds.

## Next Steps

**[IMPLEMENTATION]** See `transitions.yaml`. One code-clarity note: `process()` sets a default next step of `EVALUATE_CONDITIONAL_SKIP_FOR_FORECAST` (7.1) before evaluating the table - this looks like it should default toward 6.2, but it's dead code: Table 6-3's two conditions are logically exhaustive and all three outcomes explicitly set their own next step, so the pre-evaluation default is never actually observed.

## Plain-Language Walkthrough

This is a fast, early exit: if the dose was given after its lot expired, or is flagged with an administration condition (recall, misadministration, etc.), there's no point running it through age/interval/vaccine-type checks - it's rejected outright and the engine jumps straight to re-evaluating/forecasting the rest of the series (4.4). Otherwise it proceeds into the main evaluation chain starting at 6.2.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateDoseAdministeredCondition` (LogicStepType `EVALUATE_DOSE_ADMINISTERED_CONDITION`) - `cdsi-engine`.
- Tests: `org.openimmunizationsoftware.cdsi.core.logic.EvaluateDoseAdministeredConditionTest` - one of the very few Chapter 4-9 steps with a dedicated unit test.

## Review Findings

- **CALCDTLOTEXP-1 is not implemented** (code comment: "not yet implemented") - the Lot Expiration Date is read as a pre-existing field rather than calculated by this step. Draft `IMPLEMENTATION_MISMATCH` candidate; needs confirmation of where (if anywhere) this value is actually populated upstream.
- Table 6-3's "Evaluation status is 'sub-standard'" outcome text doesn't obviously correspond to a `setEvaluationStatus(...)` call in `EvaluateDoseAdministeredCondition` - only `TargetDoseStatus.NOT_SATISFIED` is set. Flagged for review rather than assumed to be an oversight (a "sub-standard" status may be represented elsewhere in the domain model that this pass didn't trace).
