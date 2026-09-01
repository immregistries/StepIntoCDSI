# 6.8 Evaluate for Preferable Vaccine

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 63-66. Figures 6-17/6-18 (Patient Received / Did Not Receive a Preferable Vaccine), Figure 6-19 (Process Model). Table 6-25 (Attributes), Table 6-26 (decision table), Table 6-27 (Business Rules). Business rules CALCDTPREF-1, CALCDTPREF-2.

## Purpose

**[SPEC]** "Evaluate for preferable vaccine validates the vaccine of a vaccine dose administered against the list of preferable vaccines." **[SPEC]** Volume is "sparsely populated and tracked differently in most systems," so it is never used to invalidate a dose - only to attach an evaluation reason noting under-volume administration.

## Entry Conditions

**[SPEC]** Runs after 6.7, regardless of conflict outcome.

## Inputs and Attributes

**[SPEC]** Table 6-25: Date Administered, Volume, Trade Name (all from the dose administered), Preferable Vaccine elements (Supporting Data), and two calculated dates - Preferable Vaccine Type Begin/End Age Date (CALCDTPREF-1/2), assumed `01/01/1900`/`12/31/2999`.

**[IMPLEMENTATION]** One `LT` per `PreferrableVaccine` defined on the series dose - matching all attributes directly.

## Business Rules

**[SPEC]** CALCDTPREF-1/2: begin/end age date = patient's date of birth + the preferable vaccine's begin/end age.

**[IMPLEMENTATION]** Computed directly in the constructor via `pi.getVaccineTypeBeginAge().getDateFrom(birthDate)` / `getVaccineTypeEndAge().getDateFrom(birthDate)` - not routed through a shared `DateRules` class the way 6.5/6.6's CALCDTINT rules are, but the calculation matches.

## Decision Tables

**[SPEC]** Table 6-26 Was the Vaccine Dose Administered a Preferable Vaccine for the Target Dose? (4 conditions, 5 outcomes):

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 |
| --- | --- | --- | --- | --- | --- |
| Same vaccine type as a preferable vaccine? | Yes | Yes | No | Yes | Yes |
| Within the preferable vaccine's age window? | Yes | Yes | - | No | Yes |
| Same trade name? | Yes | Yes | - | - | No |
| Volume ≥ preferable volume? | Yes | No | - | - | - |
| **Outcome** | Preferable | Preferable (low volume note) | Not preferable | Not preferable (out of age range) | Not preferable (trade name mismatch) |

## State Changes

**[IMPLEMENTATION]** `EvaluateForPreferableVaccine$LT`'s five outcomes match the spec's five rules exactly, including Rule 2's `EvaluationReason.LESS_THAN_RECOMMENDED_VOLUME`. **Condition 2 (trade name check) is hardcoded to always return `LogicResult.YES`** - the code comment says why: `"trade name is not set to the correct value, and trade name is not passed into the forecaster"`. Consequence: outcome 4 (Rule 5, "trade name mismatch") can never be selected via this condition in practice, since it always evaluates as matching. The engine still evaluates "was this vaccine preferable" correctly on type/age/volume; it just never actually distinguishes by trade name today, despite the specification defining that as a real condition.

## Next Steps

See `transitions.yaml`. Note that `process()` overrides its own default based on whether *any* preferable vaccine matched across all of them (not per-rule) - unlike 6.1-6.3's per-outcome overrides.

## Plain-Language Walkthrough

If a target dose lists one or more "preferable" vaccine formulations (a specific brand/type combination CDC recommends over merely-allowable alternatives), this step checks whether the administered dose matches one of them on type, age-appropriateness, trade name, and volume. A match (even an under-volume one, which just gets a note rather than a rejection) means the dose satisfies the "preferable vaccine" requirement directly; no match at all sends the dose on to 6.9 to check the more permissive "allowable vaccine" list instead.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateForPreferableVaccine` (LogicStepType `EVALUATE_FOR_PREFERABLE_VACCINE`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Trade-name matching is hardcoded to always pass** (code comment confirms this is deliberate, pending trade name actually being passed into the forecaster) - Rule 5's "trade name mismatch" outcome is currently unreachable. Draft `IMPLEMENTATION_MISMATCH` in the sense that Table 6-26 defines a condition the engine doesn't actually evaluate yet; recorded as a known, intentional-looking limitation rather than an accidental bug, pending confirmation.
