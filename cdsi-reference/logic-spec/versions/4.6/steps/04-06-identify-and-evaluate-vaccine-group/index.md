# 4.6 Identify and Evaluate Vaccine Group

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 39-40. Figure 4-8 (Identify and Evaluate Vaccine Group Process Model). No tables, no business rules in this section (see Chapter 9).

## Purpose

**[SPEC]** "The goal of identify and evaluate vaccine group is to merge together antigen-based forecasts into vaccine group forecasts. This is especially important in MMR and DTaP/Tdap/Td vaccine groups which each contain more than one antigen... it is important to provide a forecast consistent with the vaccine group rather than the individual antigen." For vaccine groups with non-equivalent series groups, "it is important to only blend best patient series of the same series type (e.g., risk with risk and standard with standard)."

## Entry Conditions

**[SPEC]** Follows 4.5 - one or more best patient series have been selected per antigen.

## Inputs and Attributes

**[SPEC]** None in this section (see Chapter 9's step packages).

## Business Rules

**[SPEC]** None in this section.

## Decision Tables

**[SPEC]** None in this section - "loops through each vaccine group and applies the business rules defined in Chapter 9."

## State Changes

**[IMPLEMENTATION]** Advances `dataModel.vaccineGroupPos`; while vaccine groups remain, sets the current vaccine group and delegates to 9.1; once exhausted, ends the entire forecast (`LogicStepType.END`).

## Next Steps

**[SPEC]** Not stated as a loop (see Purpose).

**[IMPLEMENTATION]** Transitions to **9.1 Apply General Vaccine Group Rules** while vaccine groups remain, and to **END** once every vaccine group has been processed - this is the final step of the entire engine run. See `transitions.yaml`.

## Plain-Language Walkthrough

The last loop in the overall model: having selected the best patient series per *antigen* (4.5), this step groups those antigen-level results into *vaccine groups* (e.g. combining Diphtheria/Tetanus/Pertussis best series into one DTaP/Tdap/Td vaccine-group forecast) - delegating the actual merge rules to Chapter 9 once per group, then ending the run.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.IdentifyAndEvaluateVaccineGroup` (LogicStepType `IDENTIFY_AND_EVALUATE_VACCINE_GROUP`) - `cdsi-engine`.
- Structured logs at `CONTROL`/`STATE` level tracing vaccine-group iteration (prefixed `IDANDEVAL_VG:` in the log messages themselves).
- Tests: no dedicated unit test.

## Review Findings

None identified.
