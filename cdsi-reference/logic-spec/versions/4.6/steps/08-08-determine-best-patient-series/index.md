# 8.8 Determine Best Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 92. No figure. Table 8-14 (decision table) - see the attribution note in `08-07-select-prioritized-patient-series/index.md`: the master inventory and the extractor's table file misattribute this to 8.7; it is actually 8.8's own table, confirmed against both the raw section text and `DetermineBestPatientSeries.java`. No business rules of its own (Table 8-14's conditions reference concepts - complete/equivalent series group/series type - already defined by rules documented in earlier sections).

## Purpose

**[SPEC]** "Determine best patient series provides the decision table to be applied to the set of prioritized patient series, one per Series Group, determined above. This step only happens after one prioritized patient series has been selected for each Series Group for the antigen. After this process, one or more non-redundant best patient series will remain."

## Entry Conditions

**[SPEC]** Runs once all series groups for the current antigen have produced a prioritized patient series (via 8.2's shortcut or the full 8.3-8.7 pipeline).

## Decision Tables

**[SPEC]** Table 8-14 Is the Prioritized Patient Series the Best Patient Series for the Series Group?

| Condition | Rule 1 | Rule 2 | Rule 3 |
| --- | --- | --- | --- |
| Is the prioritized patient series a complete patient series? | Yes | No | No |
| Is there a prioritized patient series that is complete in an equivalent series group? | - | No | No |
| Is the series type 'Evaluation Only'? | - | No | No |
| Is the series type 'Risk'? | - | Yes | No |
| Is there a prioritized patient series with type 'Risk' in an equivalent series group? | - | - | No |
| **Outcome** | Yes, it's a best patient series | Yes, it's a best patient series | Yes, it's a best patient series |
| **Default (no rule matches)** | No, there is no best patient series for the series group | | |

**[IMPLEMENTATION]** `DetermineBestPatientSeries` builds one `LT` (a 5-condition, 3-rule `LogicTable` literally labeled `"TABLE 8-14 ..."`) **per prioritized series**, filtered to only those matching the current antigen (`ps.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())`). All five conditions and all three rules match the spec table exactly, including the "equivalent series group" conditions, which scan every OTHER patient series in `dataModel.getPatientSeriesStepper().getList()` (not just prioritized ones) for a matching complete/Risk series elsewhere.

## State Changes

**[SPEC] / [IMPLEMENTATION]** Rules 1-3 add the prioritized series to `dataModel.getBestPatientSeriesList()`. The default outcome (no rule matches) does nothing - the series is excluded from the best-series list.

## Next Steps

**[SPEC]** Not stated in this section, but this is the last of Chapter 8's activities per Table 8-1.

**[IMPLEMENTATION]** Unconditional to **4.5** (`SELECT_BEST_PATIENT_SERIES` - the antigen-loop driver, NOT another Chapter 8 section). 4.5 then either advances to the next antigen (looping back into 8.1) or, once all antigens are done, proceeds to 4.6 (Identify and Evaluate Vaccine Group). See `transitions.yaml` and `chapter-08-index.md`.

## Plain-Language Walkthrough

By this point, each series group for the antigen has exactly one "winner" (its prioritized series). This final check decides whether that winner actually deserves to be forecast at all: a complete series always makes the cut; an incomplete series only makes the cut if there isn't already a complete series doing the same job in an equivalent group, and (for Evaluation-Only or non-Risk series) if there isn't a competing Risk-type series elsewhere that would make this one redundant. Anything that fails all three checks is quietly dropped - not every prioritized series becomes a "best" series.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.DetermineBestPatientSeries` (LogicStepType `DETERMINE_BEST_PATIENT_SERIES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- No implementation gap found in this class itself - Table 8-14 is faithfully and completely implemented. The significant findings for this chapter are all in 8.4/8.5/8.6 (the scoring family) - see those packages' Review Findings, and `chapter-08-index.md` for the chapter-level summary.
- Table 8-13/8-14 attribution correction - see Source, above, and `08-07-select-prioritized-patient-series/index.md`.
