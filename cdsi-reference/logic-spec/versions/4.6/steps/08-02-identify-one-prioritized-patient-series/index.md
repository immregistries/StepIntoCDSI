# 8.2 Identify One Prioritized Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 87-88. No figure of its own - Figure 8-2 ("Select Prioritized Patient Series Process Model", page 86) sits between Table 8-1 and 8.1's heading and illustrates the whole 8.1-8.7 per-series-group flow, not 8.2 specifically; it's documented in `chapter-08-index.md` instead. Table 8-3 (decision table), Table 8-4 (business rules). Business rules SELECTB-6, SELECTB-7, SELECTB-16.

## Purpose

**[SPEC]** "Identify one prioritized patient series examines all of the patient series for a given Series Group to determine if one of the patient series is superior to all other patient series and can be considered the prioritized patient series." This is a shortcut check: if the answer is a clean "yes," the whole scoring machinery in 8.3-8.7 can be skipped for this series group.

## Entry Conditions

**[SPEC]** Runs after 8.1 has built the scorable-series list for the current series group.

## Inputs and Attributes

**[SPEC]** None in table form - Table 8-3's conditions (counts of scorable/default/complete/in-process series) are computed directly rather than through a named attribute table.

## Business Rules

**[SPEC]** Table 8-4 Identify One Prioritized Patient Series Business Rules:

| Business Rule ID | Business Rule |
| --- | --- |
| SELECTB-6 | A scorable patient series is a complete patient series if its forecast has patient series status 'Complete'. |
| SELECTB-7 | A relevant patient series is a default patient series if the default series flag is 'Y' for the antigen series. |
| SELECTB-16 | A scorable patient series is an in-process patient series if it has at least one target dose with status 'Satisfied' AND its forecast has patient series status 'Not Complete'. |

**[IMPLEMENTATION]** All three counted directly in `IdentifyOnePrioritizedPatientSeries$LT`'s condition methods, matching the rule text exactly (including SELECTB-16's `getSatisfiedByVaccineDoseAdministered() != null` check as the "at least one target dose Satisfied" test).

## Decision Tables

**[SPEC]** Table 8-3 Is There a Single Prioritized Patient Series in a Series Group?

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 |
| --- | --- | --- | --- | --- | --- |
| How many scorable patient series are in the series group? | 0 | 1 | >1 | >1 | >1 |
| How many default patient series are in the series group? | 1 | - | - | - | 1 |
| How many complete patient series are in the series group? | - | - | 1 | 0 | 0 |
| How many in-process patient series are in the series group? | - | - | - | 1 | 0 |
| **Outcome** | Yes (default series) | Yes (the single scorable series) | Yes (the single complete series) | Yes (the single in-process series) | Yes (the default series) |
| **Default (no rule matches)** | No - more than one scorable series has potential; proceed to scoring | | | | |

**[IMPLEMENTATION]** `IdentifyOnePrioritizedPatientSeries$LT` implements this as a 4-condition, 5-rule `LogicTable` using `ZERO`/`ONE`/`MORE_THAN_ONE`/`ANY` results - a faithful, verified match to the table above, condition for condition.

## State Changes

**[IMPLEMENTATION]** When a rule fires (0-4), the matching series is added to `dataModel.getPrioritizedPatientSeriesList()`. When the default outcome fires (no single series), nothing is added here - 8.3 onward is responsible for populating it after scoring.

## Next Steps

**[SPEC]** Not stated as a transition rule, but the spec's own structure (8.2 is a shortcut *around* 8.3-8.7) implies exactly the branch below.

**[IMPLEMENTATION]** **This step branches** (see `transitions.yaml`): the default outcome (no single series found) continues to **8.3** to begin scoring; any of rules 1-5 (a single series was identified) skips directly to **8.8**, bypassing the entire 8.3-8.7 scoring family for this series group.

## Plain-Language Walkthrough

Most of the time, scoring across five possible patient-series shapes (complete, in-process, no-valid-doses, etc.) is overkill - if there's exactly one scorable series, or exactly one default series and nothing else competing, or exactly one complete/in-process series and nothing else that could tie with it, the answer is already obvious. This step is that fast path: it counts a handful of things about the series group and, if any of five specific patterns match, declares the winner immediately and jumps straight to 8.8. Only when none of those patterns match - genuinely ambiguous cases with real competition - does the full 8.3-8.7 scoring pipeline actually run.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.IdentifyOnePrioritizedPatientSeries` (LogicStepType `IDENTIFY_ONE_PRIORITIZED_PATIENT_SERIES`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **The 8.3-8.7 scoring family is conditionally skipped, not always executed** - worth calling out explicitly for anyone reasoning about test coverage or debugging a scoring-related FITS failure: if the failing case's series group happens to match one of Table 8-3's five shortcut rules, the bug (if any) is somewhere in 8.2 or 8.8, not 8.3-8.7, regardless of how the case "should" have been scored. No defect found in this step itself.
