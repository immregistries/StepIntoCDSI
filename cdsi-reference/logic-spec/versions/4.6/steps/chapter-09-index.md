# Chapter 9: Identify and Evaluate Vaccine Group

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, page 93 (chapter overview only - see linked step packages for each subsection). Table 9-1 (Identify and Evaluate Vaccine Group Process Steps), Figure 9-1 (Identify and Evaluate Vaccine Group Process Model).

## Overview

**[SPEC]** "Identify and evaluate vaccine group combines patient series into a vaccine group-based forecast to provide a common and consistent view for a forecast. In the evaluation, forecasting, and select patient series chapters, all logic was specified for antigens. At this point it is important to define how those antigen-based evaluation and forecasting results can be merged into vaccine group forecasts." **[SPEC]** "At present, MMR and DTaP/Tdap/Td vaccine groups are comprised of multiple antigens. MMR contains the antigens Measles, Mumps, and Rubella. DTaP/Tdap/Td contains the antigens Diphtheria, Tetanus, and Pertussis." Table 9-1 lists 9.1 through 9.3 as this chapter's activities, and states 9.1's second goal is "to classify the vaccine group type (Single Antigen or Multiple Antigen) for subsequent business rule sections (9.2 or 9.3)" - i.e. 9.2/9.3 are a **branch**, not a sequence.

## Process flow (verified against the actual Java transitions, not assumed from the table order)

This is the **last chapter** in the overall processing model, and it loops - it does not simply run once and end:

1. Chapter 9 is entered from **4.6** (Chapter 4's own vaccine-group step, which hands off to this chapter's real driver).
2. The chapter-overview section itself, **"9"**, is implemented as `IdentifyAndEvaluateVaccineGroup` - a loop driver, not a pass-through. Each time it runs, it advances a vaccine-group position counter; while groups remain, it sets the current vaccine group and proceeds to **9.1**; once exhausted, it transitions to **END**.
3. **9.1 branches**: exactly one antigen → **9.2**; more than one → **9.3** (VACCINEGROUP-1/2).
4. **9.2 and 9.3 both return to "9"** unconditionally (confirmed: every `setNextLogicStepType` call in both classes targets `IDENTIFY_AND_EVALUATE_VACCINE_GROUP`), closing the loop for the next vaccine group.
5. So the full cycle per vaccine group is: **9 → 9.1 → (9.2 or 9.3) → 9 → ...**, until every vaccine group has been processed, at which point **9 → END** - this is where the entire engine run terminates.

## Subordinate steps

| Section | Title | Status |
| --- | --- | --- |
| [9.1](09-01-apply-general-vaccine-group-rules/index.md) | Apply General Vaccine Group Rules | draft |
| [9.2](09-02-single-antigen-vaccine-group/index.md) | Single Antigen Vaccine Group | draft |
| [9.3](09-03-multiple-antigen-vaccine-group/index.md) | Multiple Antigen Vaccine Group | draft |

9.2 and 9.3 are alternatives (see Process flow above), not sequential steps - a given vaccine group runs through exactly one of them per pass through the loop.

## Real implementation gaps surfaced while documenting this chapter

Recorded here for visibility; each is detailed with code citations in its own step's Review Findings. Nothing in this chapter approaches the severity of Chapter 7's missing-contraindication-logic finding (7.3 remains the single most significant finding across the whole documentation effort) - this chapter's issue is a traceability gap, not missing behavior:

- **9.1**: implements only VACCINEGROUP-1/2 (the single-vs-multiple classification) from Table 9-2's twelve business rules. The other ten (FORECASTVG-1 through 9, FORECASTDN-2) describe vaccine-group-forecast date/reason aggregation that genuinely runs, but in `MultipleAntigenVaccineGroup` (9.3) and, trivially, `SingleAntigenVaccineGroup` (9.2) - under different or no rule-ID labels, not in the class this document's mapping assigns to Table 9-2's section. The *behavior* is present and exercised; the *traceability* from rule ID to implementing code is not, for those ten rules.
- **9.3**: MULTIANTVG-1's "latest date administered" clause (part of its priority-forecast branch) wasn't traced to a specific line of code in this pass - flagged unconfirmed, not resolved by guessing.

## Extraction-tooling note

This chapter's own documentation surfaced a real gap in the extraction tooling, worked around by hand rather than fixed in this pass (consistent with how the front-matter-gap limitation from Chapters 6-8 was handled): section 9.3, being the last in-scope section, has no "next section" to bound its extracted text, so `extracted/sections/09-03-*.txt` runs 3711 lines deep into Appendices A/B/C. This also inflated the master inventory's auto-detected business-rule count for 9.3 to 100+ (almost entirely cross-references to other chapters' rules, plus glossary-table entries the regex happened to match). A future pass to `extract.py` should give the last in-scope section of each chapter (and the last chapter overall) an explicit upper page bound.

## Phase 5 milestone

This completes step-package documentation for every executable step in Chapters 4 through 9 (34 step packages: 4.1-4.6, 5.1, 6.1-6.10, 7.1-7.6, 8.1-8.8, 9.1-9.3) plus all six chapter-overview indexes. See `mappings/spec-to-code.yaml` for the complete spec-to-code mapping and each chapter's own index for a summary of what this pass found.
