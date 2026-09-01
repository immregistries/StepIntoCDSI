# 6.9 Evaluate for Allowable Vaccine

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 66-69. Figures 6-20/6-21 (Patient Received / Did Not Receive an Allowable Vaccine), Figure 6-22 (Process Model). Table 6-28 (Attributes), Table 6-29 (decision table), Table 6-30 (Business Rules). Business rules CALCDTALLOW-1, CALCDTALLOW-2.

## Purpose

**[SPEC]** "Evaluate for allowable vaccine validates the vaccine of a vaccine dose administered against the list of allowable vaccines." Structurally a simpler sibling of 6.8: no trade name or volume condition, just vaccine type and age window.

## Entry Conditions

**[SPEC]** Runs only when 6.8 found no matching preferable vaccine (see 6.8's `transitions.yaml`).

## Inputs and Attributes

**[SPEC]** Table 6-28: Date Administered, Vaccine Type, Allowable Vaccine elements (Supporting Data), and two calculated dates - Allowable Vaccine Type Begin/End Age Date (CALCDTALLOW-1/2), assumed `01/01/1900`/`12/31/2999`.

**[IMPLEMENTATION]** Matches exactly; one `LT` per `AllowableVaccine` defined on the series dose.

## Business Rules

**[SPEC]** CALCDTALLOW-1/2: begin/end age date = patient's date of birth + the allowable vaccine's begin/end age.

**[IMPLEMENTATION]** Computed inline, same pattern as 6.8's CALCDTPREF-1/2 (`av.getVaccineTypeBeginAge().getDateFrom(birthDate)` etc.) - notably done **inside the first `LogicCondition`'s `evaluateInternal()`** (only if the vaccine type matches at all), not in the constructor the way every other step in this batch computes its calculated attributes. Functionally equivalent, just a different place in the code to look for it.

## Decision Tables

**[SPEC]** Table 6-29 Was the Vaccine Dose Administered an Allowable Vaccine for the Target Dose?

| Condition | Rule 1 | Rule 2 | Rule 3 |
| --- | --- | --- | --- |
| Same vaccine type as an allowable vaccine? | Yes | No | Yes |
| Within the allowable vaccine's age window? | Yes | - | No |
| **Outcome** | Allowable | Not allowable | Not allowable (out of age range) |

## State Changes

**[IMPLEMENTATION]** `EvaluateForAllowableVaccine$LT`'s three outcomes match the three rules; none of them set an `EvaluationReason` (the spec's outcome text for Rule 3 does describe an age-range reason, but unlike 6.8's equivalent, this class doesn't call `setEvaluationReason(...)` anywhere - it only sets its own local `result` field for the "at least one allowable vaccine matched" rollup).

## Next Steps

See `transitions.yaml` - unconditional to 6.10 either way; a miss is recorded via `statusCause` for 6.10.

## Plain-Language Walkthrough

The fallback check after 6.8: if no preferable vaccine matched, is the administered vaccine at least on the *allowable* list for this target dose (a looser, still clinically acceptable set)? Same type-then-age-window logic as 6.8, without the trade-name/volume nuance. Either way, 6.10 gets the final word on whether the dose is satisfied.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateForAllowableVaccine` (LogicStepType `EVALUATE_FOR_ALLOWABLE_VACCINE`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- Table 6-29's Rule 3 outcome text implies an evaluation reason should be recorded for "out of recommended age range," but the code doesn't set one for this step (unlike 6.8's equivalent case, which does). Minor, low-confidence observation - worth a reviewer's eye rather than a confirmed finding, since the overall Not-Satisfied/Not-Valid outcome is still set correctly at 6.10 regardless.
