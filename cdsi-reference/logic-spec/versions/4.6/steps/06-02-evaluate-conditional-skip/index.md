# 6.2 Evaluate Conditional Skip

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 47-51. Figure 6-3 (Conditional Skip Process Model). Table 6-4 (Conditional Skip Attributes), Table 6-5 (Conditional Skip Business Rules), Table 6-6/6-7/6-8/6-9 (one decision table per condition type: Age / Completed Series / Interval / Vaccine Count), Table 6-10 (Is the Conditional Skip Set Met?), Table 6-11 (Can the Target Dose Be Skipped?). Business rules CALCDTSKIP-3, CALCDTSKIP-4, CALCDTSKIP-5, CONDSKIP-1, CONDSKIP-2.

**[Extraction note, not a specification issue]** Table 6-11 genuinely exists in the document body (confirmed by reading the raw page text directly) but is **missing from the specification's own List of Figures and Tables front matter** - the LOFT jumps from Table 6-10 straight to Table 6-12. Because this project's extractor builds its table inventory from that front matter, `extracted/tables/table-6-11.txt` does not exist; the table content quoted below is taken directly from `extracted/sections/06-02-evaluate-conditional-skip.txt` and cross-checked against `EvaluateConditionalSkip$LT611`. (The same pattern recurs for Table 6-19 in section 6.5 - see that step's index.md.)

## Purpose

**[SPEC]** "Evaluate Conditional Skip addresses times when a target dose can be skipped. A dose should be considered necessary unless it is determined that it can be skipped." Common reasons: catch-up doses that are no longer needed, a reduced total-dose count once behind-schedule, or a prior dose that negates the need for this one. **[SPEC]** "this section of logic is defined here once, but used in both Evaluation and Forecasting" - Chapter 7 (7.1) refers back to this exact section rather than repeating it.

## Entry Conditions

**[SPEC]** Runs after 6.1 accepts the dose for evaluation. Only Conditional Skip instances with a context of "Evaluation" or "Both" apply here (a "Forecast"-only instance would only apply under 7.1).

## Inputs and Attributes

**[SPEC]** Table 6-4 Conditional Skip Attributes: Date Administered, Administered Dose Count, Conditional Skip elements (Supporting Data), Assessment Date, Earliest Date, Conditional Skip Start/End Date, and three calculated dates (CALCDTSKIP-3 Begin Age Date, CALCDTSKIP-4 End Age Date, CALCDTSKIP-5 Interval Date).

**[IMPLEMENTATION]** The shared base class `EvaluateConditionalSkip` creates `caDateAdministered`, `caAssessmentDate`, `caAdministeredDoseCount` at the top level, then per Conditional Skip Condition (there can be several, grouped into sets): `caConditionalSkipElements`, `caStartDate`, `caEndDate`, `caConditionalSkipBeginAgeDate`/`EndAgeDate`/`IntervalDate`, plus two attributes not in Table 6-4 itself but used by the decision tables (the code says so explicitly): `caNumberofConditionalDosesAdministered` (CONDSKIP-1) and `caConditionalSkipReferenceDate` (CONDSKIP-2).

## Business Rules

**[SPEC]** Table 6-5: CALCDTSKIP-3/4/5 (begin/end age date, interval date calculations); CONDSKIP-1 (count of conditional doses administered meeting vaccine-type/date/evaluation-status criteria); CONDSKIP-2 (the reference date - date administered when evaluating, assessment date when forecasting, earliest date when validating).

**[IMPLEMENTATION]** CALCDTSKIP-3/4/5 computed via `org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules` (`CALCDTSKIP_3/4/5.evaluate(...)`). CONDSKIP-1 via a dedicated `org.openimmunizationsoftware.cdsi.core.logic.businessRules.CONDSKIP_1` class. CONDSKIP-2 is implemented as a `switch` on `conditionalSkipType` (`EVALUATE`/`FORECAST`/`VALIDATING`) directly in the constructor, matching the spec's three cases exactly.

## Decision Tables

**[SPEC]** Four parallel per-condition-type tables, each answering "is this one condition met?":
- Table 6-6 (Age): begin age date ≤ reference date < end age date → Yes/No.
- Table 6-7 (Completed Series): does the named Series Group have a Complete relevant patient series? → Yes/No.
- Table 6-8 (Interval): requires at least one dose administered AND reference date ≥ interval date → Yes; otherwise No (3 rules, including "no dose administered at all" as its own No case).
- Table 6-9 (Vaccine Count by Age/Date): a 3×3 grid comparing "greater than / equal to / less than" the conditional skip dose count against the actual count logic.

Then two combining tables:
- Table 6-10 (Is the Conditional Skip Set Met?): AND requires all conditions in the set met; OR requires at least one.
- Table 6-11 (Can the Target Dose Be Skipped?): AND requires all sets met; OR requires at least one set met.

## State Changes

**[IMPLEMENTATION]** Each condition type maps to its own inner `LTInnerSet` subclass - `LT66` (Age), `LT67` (Completed Series), `LT68` (Interval), `LT69` (Vaccine Count) - matching Tables 6-6/6-7/6-8/6-9 one-for-one. `LT67`'s condition is **hardcoded** `return LogicResult.NO;` with no logic reading the "Series Group has a Complete series" state at all - a verified, real gap (see Review Findings). `LT610` implements Table 6-10's AND/OR combining, `LT611` implements Table 6-11's AND/OR combining and sets `TargetDoseStatus.SKIPPED` plus the next step on a "Yes."

## Next Steps

See `transitions.yaml`. Skip → 4.4 (loop back to the per-series evaluate/forecast driver); no skip → 6.3.

## Plain-Language Walkthrough

This is CDSi's most structurally nested step: a target dose's Conditional Skip definition is a tree - one or more **sets**, each containing one or more **conditions** of four possible types (age window, prior series completion, interval since a reference point, or a dose-count comparison). Each condition is evaluated independently (Tables 6-6-6-9), rolled up per-set by that set's own AND/OR logic (Table 6-10), then rolled up again across all sets by the dose's overall AND/OR set-logic (Table 6-11). If the final answer is "yes, skip," the target dose is marked Skipped and the engine moves on to the next target dose (4.4) without ever running 6.3 onward for this one.

The exact same code (`EvaluateConditionalSkip`) runs this whole tree again in Chapter 7 (7.1) during forecasting - only the "reference date" (CONDSKIP-2) and the two step destinations differ, which is exactly the "defined once, used twice" design the specification calls out explicitly.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateConditionalSkipForEvaluation` (LogicStepType `EVALUATE_CONDITIONAL_SKIP_FOR_EVALUATION`) - a near-empty subclass that only sets the `ConditionalSkipType.EVALUATE` context and its two destination steps.
- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateConditionalSkip` - the actual shared implementation (also used by 7.1's `EvaluateConditionalSkipForForecast`, a sibling subclass this pass didn't need to open since the base class is identical either way).
- Code comments in the base class label some inner tables with a stale "Table 4-6" / "Table 4-7" chapter number (`LT66`, `LT68`) rather than the current spec's "Table 6-6" / "Table 6-8" - a documentation-only leftover from an earlier chapter-numbering revision, not a functional issue (the constructor-level `setConditionTableName` calls correctly say "Table 6.4").
- Tests: no dedicated unit test.

## Review Findings

- **`LT67` (Table 6-7, "Completed Series" condition type) is hardcoded to always return `LogicResult.NO`** - it never reads whether the named Series Group actually has a Complete relevant patient series. This means any Conditional Skip condition of type COMPLETED_SERIES can never be satisfied via this path, the same shape of gap as 05-01's Table 5-4 finding. Draft `IMPLEMENTATION_MISMATCH`; needs a domain-expert read on whether real Supporting Data actually defines COMPLETED_SERIES conditional-skip conditions.
- Table 6-11 missing from the document's own LOFT front matter (see Source, above) - worth fixing upstream (CDC's next revision) more than it's worth working around here, since the raw section text already carries the content faithfully.
