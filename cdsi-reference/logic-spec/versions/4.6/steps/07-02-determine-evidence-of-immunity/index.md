# 7.2 Determine Evidence of Immunity

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 73. Figure 7-2 (Evidence of Immunity Process Model). Table 7-2 (Immunity Attributes), Table 7-3 (Does the Patient have Evidence of Immunity? - a decision table). No business rules of its own.

## Purpose

**[SPEC]** "Determine evidence of immunity assesses the patient's profile to determine if the patient is already potentially immune to the target disease, negating the need for additional doses. A patient may be considered immune due to their clinical history or if they were born before a defined date for the given target disease." The specification's own example: for measles, a patient is immune if they have a clinical finding of "Measles immune" **or** if they were born before 01/01/1957.

## Entry Conditions

**[SPEC]** Runs after 7.1 (the dose was not skipped).

## Inputs and Attributes

**[SPEC]** Table 7-2 Immunity Attributes: Patient Date of Birth, Patient Country of Birth, Patient Evidence of Immunity (clinical history), Supporting Data Immunity elements.

**[IMPLEMENTATION]** `DetermineEvidenceOfImmunity`'s constructor creates `caDateofBirth`, `caCountryofBirth`, `caEvidenceOfImmunity` (from `dataModel.getPatient().getMedicalHistory()`), `caImmunityElements` (from the current antigen's `getImmunityList()`) - matching the table, though `caImmunityElements` isn't actually added to `conditionAttributesList` (a minor omission from the printed attribute table, not from the logic itself).

## Business Rules

**[SPEC]** None defined in this section.

## Decision Tables

**[SPEC]** Table 7-3 Does the Patient have Evidence of Immunity?

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 |
| --- | --- | --- | --- | --- | --- |
| Does the patient history contain one of the immunity guidelines? | Yes | No | No | No | No |
| Is the patient's date of birth < the immunity birth date? | - | Yes | Yes | Yes | No |
| Does this patient have an immunity exclusion condition? | - | Yes | No | No | - |
| Is the patient's country of birth the same as the immunity country of birth? | - | - | Yes | No | - |
| **Outcome** | Immune | Not immune | Immune | Not immune | Not immune |

## State Changes

**[IMPLEMENTATION]** `DetermineEvidenceOfImmunity$LT` implements all five rules exactly matching the table's Yes/No/ANY grid (`setLogicResults` rows `[YES,NO,NO,NO,NO]`, `[ANY,YES,YES,YES,NO]`, `[ANY,YES,NO,NO,ANY]`, `[ANY,ANY,YES,NO,ANY]`). Outcomes 0 and 2 (the two "immune" rules) both set `PatientSeriesStatus.IMMUNE` and a forecast reason of "Patient has evidence of immunity"; outcomes 1, 3, 4 do nothing further (evidence of immunity is simply absent, evaluation continues).

## Next Steps

**[SPEC]** Not stated as a transition rule - Table 7-1's chapter overview implies 7.2 precedes 7.3.

**[IMPLEMENTATION]** Unconditional to **7.3** regardless of outcome - immunity status is state carried forward (read later by 7.4's own "does the patient have evidence of immunity?" condition), not a branch point here. See `transitions.yaml`.

## Plain-Language Walkthrough

A patient can be considered immune to a disease two ways: a direct clinical finding in their history (e.g. a lab-confirmed prior infection), or an age-based rule (born before a cutoff date when the disease was common enough that most people caught it naturally). Either one, on its own, is enough - hence the decision table's several independent "Yes" paths. If immune, the series is marked `IMMUNE` and forecasting for it stops without ever reaching a due date.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.DetermineEvidenceOfImmunity` (LogicStepType `DETERMINE_EVIDENCE_OF_IMMUNITY`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **`IMPLEMENTATION_MISMATCH` (draft, verified in code):** Condition 0, "Does the patient history contain one of the immunity guidelines?", is a placeholder:
  ```java
  protected LogicResult evaluateInternal() {
    if (caEvidenceOfImmunity != null) {
      // placeholder for now, above logic should determine YES or NO
      // (long comment block sketching the intended traversal of
      //  dataModel -> immunityList -> Immunity -> clinicalHistoryList)
    }
    return NO;
  }
  ```
  This always returns `NO`, so Rule 1 (the clinical-history "Yes" path) can never fire - only the birth-date-based rules (2, 3) can ever mark a patient immune via this step. The specification's own worked example ("a clinical finding of 'Measles immune'") is exactly the path that's unreachable. Needs a domain-expert/engineering read on priority, since this is a currently-inert half of the section's stated purpose, not an edge case.
