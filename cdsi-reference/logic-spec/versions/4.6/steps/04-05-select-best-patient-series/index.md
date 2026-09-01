# 4.5 Select Best Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 39. Figure 4-7 (Select Best Series Process Model). No tables, no business rules in this section (see Chapter 8 for the actual decision logic).

## Purpose

**[SPEC]** "The goal of select patient series is to determine the best path(s) to immunity for the patient based on the evaluated immunization history, forecast, and any patient observations. A best patient series will be selected for each Series Group... it is possible to select a single best patient series across the entire antigen... In other cases, multiple best patient series may be selected for a patient" (e.g. completing a risk series short-term while still needing a standard series later).

## Entry Conditions

**[SPEC]** Follows 4.4 - every relevant patient series has an evaluated history and a forecast.

## Inputs and Attributes

**[SPEC]** None in this section (see Chapter 8's step packages).

## Business Rules

**[SPEC]** None in this section.

## Decision Tables

**[SPEC]** None in this section - "loops through each antigen and applies the business rules found in Chapter 8."

## State Changes

**[SPEC]** "A simple iterative process which loops through each antigen and applies the business rules found in Chapter 8 to each antigen."

**[IMPLEMENTATION]** Like 4.3, this class is a loop driver rather than a decision step: each call advances `dataModel.antigenPos`; while antigens remain, it sets up `antigenSeriesSelectedList`/`selectedPatientSeriesList` for the current antigen and delegates to 8.1; once exhausted, it clears per-antigen state and delegates to 4.6.

## Next Steps

**[SPEC]** Not stated as a loop (see Purpose).

**[IMPLEMENTATION]** Transitions to **8.1 Pre-Filter Patient Series** while antigens remain, and to **4.6 Identify and Evaluate Vaccine Group** once all antigens are processed. See `transitions.yaml`.

## Plain-Language Walkthrough

Same shape as 4.3: the class named for the chapter-4 summary is really the outer loop, and the real decision-making (Chapter 8's series-scoring/prioritization business rules) happens once per antigen in the steps it delegates to, starting with 8.1.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.SelectBestPatientSeries` (LogicStepType `SELECT_BEST_PATIENT_SERIES`) - `cdsi-engine`.
- No structured `log(...)`/`alert(...)` calls in this class.
- Tests: no dedicated unit test.

## Review Findings

None identified.
