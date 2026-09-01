# 9.1 Apply General Vaccine Group Rules

> **Review status:** draft. See Review Findings - most of this section's own named business rules are not implemented in the class mapped to it; the underlying behavior exists, but elsewhere, unlabeled.

## Source

Logic Specification for ACIP Recommendations v4.6, page 94. No figure of its own (Figure 9-1 belongs to the chapter overview). Table 9-2 (General Vaccine Group Business Rules). Business rules FORECASTVG-1 through FORECASTVG-9, FORECASTDN-2, VACCINEGROUP-1, VACCINEGROUP-2 (12 rules).

## Purpose

**[SPEC]** "Apply general vaccine group rules provides the business rules which are applied to both types of vaccine groups (i.e., Single Antigen and Multiple Antigen). Finally, this table provides rules to classify the vaccine group type (Single Antigen or Multiple Antigen) for subsequent business rule sections (9.2 or 9.3)."

So this section has two distinct jobs per its own text: (1) define vaccine-group-forecast aggregation rules shared by both branches (FORECASTVG-1 through 9, FORECASTDN-2), and (2) classify which branch a given vaccine group takes (VACCINEGROUP-1/2).

## Entry Conditions

**[SPEC]** Runs once per vaccine group, entered from the chapter driver (section "9", `IdentifyAndEvaluateVaccineGroup`) after 4.6 has selected a vaccine group's antigens for evaluation.

## Business Rules

**[SPEC]** Table 9-2 General Vaccine Group Business Rules (paraphrased - see the full text in `extracted/sections/09-01-apply-general-vaccine-group-rules.txt` for exact wording):

| Business Rule ID | Business Rule |
| --- | --- |
| FORECASTVG-1 | A patient series forecast is contained in a vaccine group forecast if it's from a best patient series, in the vaccine group's series group, and its antigen series defines a regimen the vaccine group classifies. |
| FORECASTVG-2 | Adjusted recommended date = latest of (earliest adjusted recommended date across contained forecasts; earliest date of the vaccine group forecast). |
| FORECASTVG-3 | Adjusted past due date = latest of (earliest adjusted past due date across contained forecasts; earliest date of the vaccine group forecast). |
| FORECASTVG-4 | Latest date = earliest of the latest dates across contained forecasts. |
| FORECASTVG-5 | Unadjusted recommended date = earliest of the unadjusted recommended dates across contained forecasts. |
| FORECASTVG-6 | Unadjusted past due date = earliest of the unadjusted past due dates across contained forecasts. |
| FORECASTVG-7 | Forecast reasons = the union of the forecast reasons of all contained forecasts. |
| FORECASTVG-8 | An antigen is a recommended antigen if its best patient series is the basis of a contained forecast with status 'Not Complete'. |
| FORECASTVG-9 | A series dose vaccine is a recommended series dose vaccine if it's recommended for any contained patient series forecast. |
| FORECASTDN-2 | Forecast dose number = min of contained dose numbers if the vaccine group's "administer full vaccine group" flag is 'Y', else max. |
| VACCINEGROUP-1 | A vaccine group is a single antigen vaccine group if it classifies exactly one antigen. |
| VACCINEGROUP-2 | A vaccine group is a multiple antigen vaccine group if it classifies more than one antigen. |

**[IMPLEMENTATION]** `ApplyGeneralVaccineGroupRules` implements **only VACCINEGROUP-1/2** - its one `LogicTable` (internally still labeled `"TABLE 7 - 2 WHAT IS THE VACCINE GROUP TYPE?"`, a stale label from an earlier chapter numbering, not "Table 9-2" - a cosmetic leftover, not a functional issue) asks exactly one question, "Does the vaccine group contain exactly 1 antigen?", and branches to 9.2 or 9.3 accordingly. None of FORECASTVG-1 through 9 or FORECASTDN-2 appear anywhere in this class. See Review Findings for where that behavior actually lives.

## Decision Tables

**[IMPLEMENTATION]** The one decision actually made here: "Does the vaccine group contain exactly 1 antigen?" → Yes: single antigen vaccine group (9.2). No: multiple antigen vaccine group (9.3). This directly implements VACCINEGROUP-1/2; the specification doesn't present it as a formal Yes/No grid (Table 9-2 is prose-style business rules, not a decision table), so this is a code-side structuring choice, not a spec table being reproduced.

## Next Steps

See `transitions.yaml`: branches to 9.2 or 9.3 based on antigen count.

## Plain-Language Walkthrough

Before a vaccine group's forecast can be assembled, the engine needs to know which of two fundamentally different assembly procedures to use: a single-antigen group (Hib, HepB, Polio - one series' forecast *is* the group's forecast, essentially a pass-through) versus a multiple-antigen group (MMR, DTaP/Tdap/Td - several antigens' forecasts have to be reconciled into one). This section makes exactly that one determination. The actual "how to combine multiple series' dates and reasons into one" logic that Table 9-2's FORECASTVG rules describe turns out to live in the class for the multiple-antigen branch (9.3), not here - see Review Findings.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.ApplyGeneralVaccineGroupRules` (LogicStepType `APPLY_GENERAL_VACCINE_GROUP_RULES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **`IMPLEMENTATION_MISMATCH` (draft) - traceability gap, not a functional gap:** none of FORECASTVG-1 through FORECASTVG-9 or FORECASTDN-2 are implemented in `ApplyGeneralVaccineGroupRules`, the class this section maps to. Checked directly: `grep -rl "FORECASTVG" cdsi-engine/src/main/java` finds nothing anywhere in the codebase - these rule IDs are never cited in a code comment. However, the underlying *behavior* each rule describes (setting a vaccine group forecast's earliest/recommended/past-due/latest dates and forecast reason from its constituent patient series forecasts) **is** implemented, in `MultipleAntigenVaccineGroup.java` (9.3) - see that package's Review Findings for the detail. For the single-antigen case, `SingleAntigenVaccineGroup.java` (9.2) copies the one constituent forecast's fields directly (a trivial case of the same rules, since there's only one contributing series), citing its own `SINGLEANTVG-1` through `SINGLEANTVG-10` comment labels instead - notably, the *specification's own* Table 9-3 only names `SINGLEANTVG-1` and `SINGLEANTVG-2` explicitly; the higher-numbered labels in the code's comments (3 through 10) appear to be the original developer extending that naming pattern by analogy for the remaining copied fields, not additional rules the specification itself enumerates. So: the functionality described by FORECASTVG-1 through 9 exists and runs, split across the 9.2 and 9.3 classes under different labels (or none) rather than centralized in the class this document's own inventory maps to Table 9-2. This is a documentation/traceability problem worth fixing (e.g. moving the aggregation logic here, or at minimum citing FORECASTVG-N in the classes that actually do the work) - not a missing-behavior problem like 7.3's.
