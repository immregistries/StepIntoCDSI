# 8.7 Select Prioritized Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 91. No figure or decision table of its own - Table 8-13 (Select Prioritized Patient Series Business Rules) is a plain two-column business-rule table, not a Yes/No decision grid. Business rules SELECTBEST-1, SELECTBEST-2.

**[Extraction/attribution note, not a specification issue]** The master extraction inventory (`logic-spec/versions/4.6/index.md`) attributes both Table 8-13 and Table 8-14 to section 8.7, and `extracted/tables/table-8-13.txt` actually contains Table 8-14's content (a caption-matching mistake in the extractor - it found the right caption text but attached the wrong table geometry). Reading the raw section text directly (`extracted/sections/08-07-select-prioritized-patient-series.txt` and `08-08-determine-best-patient-series.txt`) shows Table 8-13 is entirely within 8.7's own text and Table 8-14 is entirely within 8.8's - confirmed further by `DetermineBestPatientSeries.java` (8.8), which explicitly instantiates a `LogicTable` labeled `"TABLE 8-14 ..."`, while `SelectPrioritizedPatientSeries.java` (this class) has no `LogicTable` at all. This step's tables/figures below and 8.8's are corrected to match the source, not the master inventory or the mis-extracted table file.

## Purpose

**[SPEC]** "Select prioritized patient series provides the business rules to be applied to the scored patient series which will result in the prioritized patient series for the series group."

## Entry Conditions

**[SPEC]** Runs after whichever of 8.4/8.5/8.6 scored the group's series (or, per 8.2, may be reached without any scoring having happened at all - see 8.2's Review Findings).

## Business Rules

**[SPEC]** Table 8-13 Select Prioritized Patient Series Business Rules:

| Business Rule ID | Business Rule |
| --- | --- |
| SELECTBEST-1 | The scorable patient series score is the sum of all points awarded to it. |
| SELECTBEST-2 | The prioritized patient series is the one with the highest score, or - if tied - the one with the best-ranked series preference. |

**[IMPLEMENTATION]** `selectPrioritizedPatientSeries()` picks the series with the highest `getScorePatientSeries()` (an accumulator already summed by the prior scoring steps' repeated inc/desc calls, matching SELECTBEST-1 implicitly rather than via an explicit sum step), and on a score tie, compares `getSeriesPreference()` (parsed as an integer, lower = better) - matching SELECTBEST-2.

## State Changes

**[IMPLEMENTATION]** Adds the selected series to `dataModel.getPrioritizedPatientSeriesList()`.

## Next Steps

**[IMPLEMENTATION]** Unconditional to **8.8**. See `transitions.yaml`.

## Plain-Language Walkthrough

After scoring, this step just picks the winner: highest total score wins outright, and a tie is broken by the antigen series' own declared preference ranking (a Supporting Data attribute, not something computed here).

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.SelectPrioritizedPatientSeries` (LogicStepType `SELECT_PRIORITIZED_PATIENT_SERIES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Table 8-13/8-14 attribution corrected** - see the note under Source. The master inventory and the extracted table file both point to the wrong section for these two tables; this pass verified the correct attribution against both the raw section text and the actual Java classes before writing this package and 8.8's.
- `currentSeriesPreference != ""` / `newSeriesPreference != ""` in `selectPrioritizedPatientSeries()` use Java string reference comparison (`!=`) rather than `.equals("")`. This is a real code smell, but not confidently a bug - Java commonly interns literal empty strings, so this may work correctly in practice depending on where `getSeriesPreference()`'s value originates; this pass did not trace that far. Noted for awareness, not classified as a confirmed defect.
