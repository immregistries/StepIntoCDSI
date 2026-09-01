# 4.3 Create Relevant Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 34. Figure 4-4 (Create Relevant Patient Series Process Model). No tables, no business rules in this section.

**[SPEC] Naming note:** Section 4.3 and Chapter 5 (the standalone chapter) are both literally titled "Create Relevant Patient Series." Section 4.3 is a short summary paragraph inside Chapter 4's processing-model overview; the actual attributes, business rules, and decision tables for this activity live in Chapter 5 (specifically 5.1 Select Relevant Patient Series - see that step package). This index intentionally does not duplicate Chapter 5's content; it documents only what 4.3 itself says and what the *code that implements the 4.3 step type* actually does.

## Purpose

**[SPEC]** "An antigen series is one way to reach perceived immunity against a disease... The important aspect of this step is to instantiate each antigen series as a relevant patient series provided it meets necessary requirements as defined by the logic in Chapter 5." The spec explicitly compares this to 4.1: "Similar to gathering necessary data (section 4.1), create relevant patient series will likely vary from system to system based on design details and technologies used" - i.e. 4.3, like 4.1, is deliberately left to be an orchestration concern rather than a fully specified algorithm here (the real decision logic is Chapter 5's).

## Entry Conditions

**[SPEC]** Follows 4.2 - antigen administered records already exist, sorted by antigen then date.

## Inputs and Attributes

**[SPEC]** None defined in this section (see Chapter 5's Table 5-2 for the actual attributes used to decide relevance).

## Business Rules

**[SPEC]** None in this section (see Chapter 5's CALCDTIND-1/CALCDTIND-2).

## Decision Tables

**[SPEC]** None in this section (see Chapter 5's Tables 5-4/5-5).

## State Changes

**[SPEC]** "At the end of this step, each antigen series relevant for the patient is turned into a relevant patient series... Those not relevant for the patient are excluded from further processing."

**[IMPLEMENTATION]** This is where spec and code diverge in an interesting, verified way: `CreateRelevantPatientSeries` is not itself a decision step - it is the **loop driver** around Chapter 5's actual decision step (`SelectRelevantPatientSeries`, 5.1). On its first call it builds `dataModel.antigenSelectedList` (all antigens the patient has data for, or a caller-supplied filtered subset - see `dataModel.getAntigenLabelFilterList()`) and starts iterating; on each subsequent call it just advances the position counter. It performs no antigen-series relevance decisions itself - those all happen in 5.1, once per antigen, each time this step delegates to it.

## Next Steps

**[SPEC]** Not stated (see Purpose - orchestration is left unspecified, same as 4.1).

**[IMPLEMENTATION]** Not a single fixed next step - `CreateRelevantPatientSeries.process()` transitions to **5.1 Select Relevant Patient Series** while antigens remain to process, and to **4.4 Evaluate and Forecast all Relevant Patient Series** once every antigen has been iterated. This is the loop Figure 4-2's "Refinement of Patient Series" and Table 4-1's note "(Chapter 5)" refer to. See `transitions.yaml`.

## Plain-Language Walkthrough

Don't be misled by the class name: `CreateRelevantPatientSeries.java` doesn't decide which antigen series are relevant - it's the outer loop that walks through every antigen the patient could plausibly need, calling into `SelectRelevantPatientSeries` (5.1) once per antigen to make that actual determination. Once every antigen has had its turn, the loop hands off to 4.4 to begin evaluation and forecasting.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.CreateRelevantPatientSeries` (LogicStepType `CREATE_RELEVANT_PATIENT_SERIES`) - `cdsi-engine`.
- Structured logs at default level: e.g. "Antigen selected list is null, creating", "Selecting antigen series for this antigen: X", "Done, now evaluating and forecasting all patient series".
- Tests: no dedicated unit test.

## Review Findings

None identified beyond the naming/scope note above (recorded as a `[SPEC]` observation, not a discrepancy needing resolution).
