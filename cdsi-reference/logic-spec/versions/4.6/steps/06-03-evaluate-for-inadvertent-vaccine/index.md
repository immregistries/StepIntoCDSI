# 6.3 Evaluate for Inadvertent Vaccine

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 52. Figure 6-4 (Evaluate for an Inadvertent Vaccine Process Model), Table 6-12 (Inadvertent Vaccine Attributes), Table 6-13 (decision table). No business rules.

## Purpose

**[SPEC]** "Evaluate for inadvertent vaccine determines if the vaccine type of a vaccine dose administered was an inadvertent administration due to the vaccine type that was administered."

## Entry Conditions

**[SPEC]** Runs after 6.2 determines the dose is not skipped.

## Inputs and Attributes

**[SPEC]** Table 6-12: the administered dose's Vaccine Type, and the target dose's Supporting-Data-defined list of inadvertent vaccine types.

**[IMPLEMENTATION]** `caVaccineDoseAdministered` and `caInadvertentVaccine` match directly; the actual check reads `dataModel.getTargetDose().getTrackedSeriesDose().getInadvertentVaccineList()`.

## Business Rules

None (spec or implementation).

## Decision Tables

**[SPEC]** Table 6-13 Was the Vaccine Dose Administered an Inadvertent Administration for the Target Dose?

| Condition | Rule 1 | Rule 2 |
| --- | --- | --- |
| Is the vaccine type of the dose administered one of the target dose's inadvertent vaccine types? | Yes | No |
| **Outcome** | Inadvertent - Target Dose Status 'Not Satisfied', Evaluation Status 'Not Valid', Reason 'Inadvertent Administration' | Not inadvertent |

## State Changes

**[IMPLEMENTATION]** Outcome 0 (Rule 1) sets exactly the three spec-stated values: `TargetDoseStatus.NOT_SATISFIED`, `EvaluationStatus.NOT_VALID`, `EvaluationReason.INADVERTENT_ADMINISTRATION`. It also sets `VaccineDoseAdministered.inadvertentAdministration` (FORECASTDTCAN-1) and `evaluatedAgainstTargetDose` (CALCDTINT-1; 6.10 never runs on this path). Outcome 1 makes no state change.

## Next Steps

See `transitions.yaml`. Inadvertent → 4.4 (loop back, dose rejected); not inadvertent → 6.4.

## Plain-Language Walkthrough

A short, single-condition check: was the vaccine given the wrong type for this specific target dose (e.g. an antigen series expects DTaP but the patient got Tdap for that slot)? If so, the dose is rejected outright for this target dose and processing moves to the next one; otherwise it proceeds to age validation (6.4).

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateForInadvertentVaccine` (LogicStepType `EVALUATE_FOR_INADVERTENT_VACCINE`) - `cdsi-engine`.
- Tests: `EvaluateForInadvertentVaccineTest`.

## Review Findings

- **Documented fix (2026-09-15, SPEC-4.6-0060): write `evaluatedAgainstTargetDose` on an inadvertent VDA.** Figure 6-4 returns to 4.4 without 6.10, so CALCDTINT-1 could not see this VDA's own inadvertent evaluation and borrowed the previous target's Valid evaluation with this shot's date. Rule 1 now writes the same `evaluatedAgainstTargetDose` link 6.10 writes for every other outcome.
