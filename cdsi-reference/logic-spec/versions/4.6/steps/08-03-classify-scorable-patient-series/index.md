# 8.3 Classify Scorable Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 88-89. No figure of its own. Table 8-5 (decision table), Table 8-6 (business rules). Business rules SELECTB-6, SELECTB-16, SELECTB-21.

**[Extraction note, not a specification issue]** The structured extraction (`extracted/tables/table-8-5.txt`) only captured Table 8-5's first condition row - the table's remaining two conditions and its OUTCOMES row are on the raw page but weren't picked up (the table appears to repeat its "CONDITIONS / RULES" header mid-table on the source page, similar in spirit to the page-break pattern already seen in Chapters 6-7, though here it's a mid-page repeat rather than a page-break). The full table below is transcribed from `extracted/sections/08-03-classify-scorable-patient-series.txt`, which does contain it completely, cross-checked against `ClassifyScorablePatientSeries.java`'s three actual conditions.

## Purpose

**[SPEC]** "Classify scorable patient series is an attempt to reduce the total number of patient series within a Series Group to only those which have a chance to be selected as the prioritized patient series."

## Entry Conditions

**[SPEC]** Runs only when 8.2 did NOT find a single obvious prioritized series (see 8.2's `transitions.yaml`).

## Business Rules

**[SPEC]** Table 8-6 Classify Scorable Patient Series Business Rules: SELECTB-6 (complete = forecast status 'Complete'), SELECTB-16 (in-process = at least one Satisfied target dose AND forecast status 'Not Complete' - same rule as 8.2 reuses), SELECTB-21 (number of valid doses = count of target doses with target dose status 'Satisfied').

**[IMPLEMENTATION]** `calculateCompletePatientSeriesCount()` and `calculateCountOfPatientSeriesWithValidDoses()` implement SELECTB-6/SELECTB-21 directly and correctly.

## Decision Tables

**[SPEC]** Table 8-5 Which Scorable Patient Series Should be Scored?

| Condition | Rule 1 | Rule 2 | Rule 3 |
| --- | --- | --- | --- |
| Are there 2+ complete patient series in the series group? | Yes | No | No |
| Are there 2+ in-process patient series and no complete patient series in the series group? | - | Yes | No |
| Is the number of valid doses = 0 for all scorable patient series in the series group? | - | No | Yes |
| **Outcome** | Score all complete series only | Score all in-process series only | Score all series (0-valid-doses rules) |

**[IMPLEMENTATION]** `ClassifyScorablePatientSeries$LT` is a 3-condition, 3-rule `LogicTable`, a faithful match. Rule 2's second condition method (`calculateCountOfPatientSeriesWithValidDoses`) is named around "in-process" but actually counts any scorable series with at least one Satisfied target dose - consistent with SELECTB-16's definition of in-process, not a separate calculation.

## State Changes

**[SPEC] / [IMPLEMENTATION]** No data is directly mutated here beyond selecting which scoring path runs next - this step is purely a router (see Next Steps).

## Next Steps

**[SPEC]** Table 8-1 lists 8.4/8.5/8.6 as parallel alternatives keyed to this classification, though the spec doesn't phrase it explicitly as a control-flow branch.

**[IMPLEMENTATION]** **Genuine 3-way branch** - see `transitions.yaml`: rule 1 → 8.4 (Complete), rule 2 → 8.5 (In-process), rule 3 → 8.6 (No Valid Doses). Exactly one of the three always fires since the table is a complete partition (every scorable-series group falls into exactly one of "2+ complete," "2+ in-process and 0 complete," or "0 valid doses everywhere" - though see Review Findings for a gap in that partition).

## Plain-Language Walkthrough

Once it's established that scoring is actually needed (8.2 couldn't shortcut it), this step figures out *which kind* of scoring applies: is the competition between multiple already-complete series, multiple partially-done series, or a group where nobody has any valid doses at all? Each of those three situations gets scored by a different rule set (8.4, 8.5, 8.6 respectively), because "what makes one series better than another" means something different in each case.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.ClassifyScorablePatientSeries` (LogicStepType `CLASSIFY_SCORABLE_PATIENT_SERIES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Possible gap in Table 8-5's partition:** a series group with, say, exactly 1 complete series and 1 in-process series (and not "0 valid doses for all") matches none of the three rules as literally written (rule 1 needs 2+ complete, rule 2 needs 2+ in-process AND 0 complete). `setNextLogicStepType(NO_VALID_DOSES)` is set as a pre-evaluation default before `evaluateLogicTables()` runs, so such a case would silently fall through to 8.6 (No Valid Doses) even though it may have valid doses - this looks like an intentional fallback default rather than a bug, but it means 8.6 can be entered for series groups that don't actually match its own name. Recorded as an open question, not confirmed either way by this pass.
- Extraction limitation noted in Source, above - the correct table content came from the raw section text, not `extracted/tables/table-8-5.txt`.
