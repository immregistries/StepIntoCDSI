# 6.5 Evaluate Preferable Interval

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 54-57. Figures 6-7 through 6-10 (four timelines, one per way an interval can be measured - "From Immediate Previous Dose Administered Flag," "From Target Dose Number in Series," "From Most Recent Vaccine Type," "From Relevant Observation Code"), Figure 6-11 (Process Model). Table 6-17 (Attributes), Table 6-18 (decision table), Table 6-19 (Business Rules). Business rules CALCDTINT-1, CALCDTINT-2, CALCDTINT-3, CALCDTINT-4, CALCDTINT-8, CALCDTINT-9.

**[Extraction note, not a specification issue]** Table 6-19 exists in the document body (confirmed directly) but, like Table 6-11 in section 6.2, is **missing from the specification's own LOFT front matter** (which jumps from 6-18 to 6-20). `extracted/tables/table-6-19.txt` does not exist for the same reason; its content below is transcribed from `extracted/sections/06-05-evaluate-preferable-interval.txt`.

## Purpose

**[SPEC]** "Evaluate preferable interval validates the date administered of a vaccine dose administered against defined preferable interval(s) from previous vaccine dose(s) administered or other events." A dose can have multiple preferable intervals defined at once (e.g. HepB dose 3, HPV dose 3) - **[SPEC]** "if multiple intervals are specified, then all intervals must be satisfied." Four measurement methods exist: from the immediate previous dose, from a specific target dose number in the series, from the most recent dose of a listed vaccine type, or from a patient observation date.

## Entry Conditions

**[SPEC]** Runs after 6.4 (age is always evaluated first, independent of interval outcome).

## Inputs and Attributes

**[SPEC]** Table 6-17: Date Administered, Preferable Interval elements (Supporting Data), and two calculated dates - Absolute Minimum Interval Date (CALCDTINT-3) and Minimum Interval Date (CALCDTINT-4), both assumed `01/01/1900` if empty.

**[IMPLEMENTATION]** One `LT` decision table is built **per** `Interval` defined on the series dose (not just one) - matching the spec's "all intervals must be satisfied" requirement structurally, not just by convention. Each carries its own `caDateAdministered`, `caPreferableIntervalElements`, `caAbsoluteMinimumIntervalDate` (via `CALCDTINT_3.evaluate(...)`), `caMinimumIntervalDate` (via `CALCDTINT_4.evaluate(...)`).

## Business Rules

**[SPEC]** Table 6-19: CALCDTINT-1 (reference dose date = immediate previous dose, when flagged 'Y' and that dose is Valid/Not Valid and non-inadvertent); CALCDTINT-2 (reference dose date = the dose satisfying a named target-dose-number, when the "from target dose number" method applies); CALCDTINT-3/4 (absolute minimum / minimum interval date = reference date + the respective interval); CALCDTINT-8 (reference date = most recent dose of a named vaccine type); CALCDTINT-9 (reference date = most recent matching patient observation date).

**[IMPLEMENTATION]** This class only directly calls `CALCDTINT_3`/`CALCDTINT_4` (the date-window calculations). The reference-date rules (CALCDTINT-1/2/8/9, which determine *which* prior event the interval is measured from) are not invoked here - they must be resolved earlier, when the `Interval`/`SeriesDose` data is assembled, which this pass did not trace further. Recorded as unverified rather than guessed.

## Decision Tables

**[SPEC]** Table 6-18 Did the Vaccine Dose Administered Satisfy the Preferable Interval for the Target Dose?

| Condition | Rule 1 | Rule 2 | Rule 3 |
| --- | --- | --- | --- |
| Is date administered < absolute minimum interval date? | Yes | No | No |
| Is absolute minimum ≤ date administered < minimum interval date? | - | Yes | No |
| Is minimum interval date ≤ date administered? | - | - | Yes |
| **Outcome** | Not satisfied ("Too soon") | Satisfied ("Grace period") | Satisfied |

## State Changes

**[IMPLEMENTATION]** `EvaluatePreferableInterval$LT`'s three outcomes correspond 1:1 to the three rules. **Outcome 0 (Rule 1, "not satisfied") calls `evaluation.setEvaluationReason(EvaluationReason.GRACE_PERIOD)`** - but its own log line says `"...Evaluation reason is 'Too Soon'."` and the specification's Rule 1 outcome is explicitly "Too soon," not "Grace period." Outcome 1 (Rule 2, the actual grace-period case) correctly sets `GRACE_PERIOD`. This looks like a copy/paste error in outcome 0 (see Review Findings) - the sibling class `EvaluateAllowableInterval` gets the equivalent case right, using `EvaluationReason.TOO_SOON`, which confirms that enum value exists and is the intended one here too.

## Next Steps

See `transitions.yaml` - unconditional to 6.6 either way; a failed preferable interval doesn't reject the dose outright, it falls through to allowable-interval evaluation.

## Plain-Language Walkthrough

Some vaccines have a "preferred" spacing that's a little more forgiving than a hard minimum - a dose given slightly early still counts, just with a "Grace period" note attached, matching how real-world scheduling works when a patient can't hit the exact preferred interval. If a dose was given too soon even for the preferable interval's tolerance, that alone doesn't invalidate it: the engine falls through to 6.6 to check the stricter, less forgiving allowable interval instead.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluatePreferableInterval` (LogicStepType `EVALUATE_PREFERABLE_INTERVAL`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Outcome 0's `EvaluationReason` is wrong: sets `GRACE_PERIOD` where the spec (and the code's own log message) call for "Too Soon."** Verified by direct comparison with `EvaluateAllowableInterval`'s equivalent, correctly-implemented case. Draft `IMPLEMENTATION_MISMATCH` - this could produce a misleading evaluation reason wherever a dose fails the preferable-interval check, which is exactly the kind of transparency defect this reference module exists to surface.
- CALCDTINT-1/2/8/9 (which reference date an interval measures from) are not verified as implemented anywhere by this pass - flagged as unresolved rather than assumed correct.
- Table 6-19 missing from the document's own LOFT (see Source, above) - same pattern as 6.2's Table 6-11.
