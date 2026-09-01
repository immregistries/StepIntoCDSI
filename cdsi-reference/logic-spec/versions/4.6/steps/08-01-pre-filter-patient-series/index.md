# 8.1 Pre-filter Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 87. No figure or decision table of its own - Table 8-2 (Pre-Filter Patient Series Business Rules) is the section's only table. Business rules SELECTB-24, SELECTSCORE-2.

## Purpose

**[SPEC]** "Pre-filter patient series examines each of the patient series for a given Series Group to determine if any series should be removed from consideration for best patient series. If a Series Group contains relevant patient series of different priorities, only the set of highest priority patient series should be considered when determining the best patient series for the Series Group."

## Entry Conditions

**[SPEC]** Runs once per series group, at the start of Chapter 8's per-antigen processing (driven by 4.5's antigen loop - see `chapter-08-index.md`).

## Inputs and Attributes

**[SPEC]** None in table form - Table 8-2's two business rules (below) define the filtering logic directly rather than through an attribute table.

## Business Rules

**[SPEC]** Table 8-2 Pre-Filter Patient Series Business Rules:

| Business Rule ID | Business Rule |
| --- | --- |
| SELECTB-24 | A relevant patient series that is the basis of a patient series forecast must be considered a candidate scorable patient series if the forecast is not Contraindicated, OR (the forecast is Contraindicated AND every relevant patient series in the same series group is also Contraindicated). |
| SELECTSCORE-2 | A relevant patient series must be considered a scorable patient series if one of four conditions holds: (1) it's a Risk series whose priority is at least as high as any other series in the group and it's a candidate; (2) it's a Standard series with a Valid dose administered before the maximum-age-to-start date and it's a candidate; (3) it's a Standard series with 0 valid doses, no default series exists for the group, and it's a candidate; (4) it's an Evaluation-Only series that is itself complete. |

**[IMPLEMENTATION]** `PreFilterPatientSeries.process()` implements this as plain Java conditionals rather than a formal `LogicTable`, in two passes: first it builds a "candidate" list (non-Contraindicated series, falling back to all-Contraindicated series only if every one is Contraindicated - matching SELECTB-24), then for each candidate applies a `switch` on `SeriesType` - RISK is added if its priority equals the group's highest Risk priority, STANDARD is added if it has at least one Valid dose, EVALUATION_ONLY is added if the series status is already COMPLETE. A final pass adds a default Standard series if nothing else qualified and no valid doses exist anywhere in the group.

## Decision Tables

**[SPEC]** None - this section is rule-based filtering, not a Yes/No decision grid (unlike most other Chapter 6-9 steps).

## State Changes

**[IMPLEMENTATION]** Builds `dataModel`'s `scorablePatientSeriesList` from the series group's relevant patient series, per the rules above.

## Next Steps

**[SPEC]** Not stated as a transition rule - Table 8-1 implies 8.1 precedes 8.2.

**[IMPLEMENTATION]** Unconditional to **8.2**. See `transitions.yaml`.

## Plain-Language Walkthrough

Before scoring can happen, the candidate pool has to be narrowed to series that are actually still in play: series ruled out by a Contraindication drop out (unless literally every series in the group is contraindicated, in which case they're kept so *something* can still be reported), and then each remaining series has to clear a type-specific bar (Risk series need to be among the highest-priority Risk options; Standard series need at least one dose that actually counted; Evaluation-Only series need to already be complete). A safety-net default series gets added back in if nothing survived and the patient has no valid doses at all, so the group never comes up completely empty-handed.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.PreFilterPatientSeries` (LogicStepType `PRE_FILTER_PATIENT_SERIES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Imperative structure doesn't map 1:1 onto SELECTSCORE-2's four spec bullets (draft observation, not a confirmed defect):** the code's `switch` on `SeriesType` (RISK/STANDARD/EVALUATION_ONLY) captures the *gist* of each bullet but not every literal clause - e.g. the Standard-series bullet's "date administered before the maximum age to start date" qualifier isn't checked, only "has at least one Valid dose" is; and the "0 valid doses AND no default series exists" Standard-series bullet isn't handled in this pass at all (it's only reachable via the separate fallback-default-series block at the end, which checks a different, narrower condition). This may be an intentional simplification that produces equivalent results given how the data is structured elsewhere, or it may be a real gap - this pass did not trace far enough to be certain either way, so it's recorded as an open question rather than a confirmed `IMPLEMENTATION_MISMATCH`.
