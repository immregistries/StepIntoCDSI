# Chapter 8: Select Best Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 84-86 (chapter overview only - see linked step packages for each subsection). Table 8-1 (Select Best Patient Series Process Steps), Figure 8-1 (Select Best Patient Series Process Model, illustrating the whole chapter), Figure 8-2 (Select Prioritized Patient Series Process Model, illustrating the 8.1-8.7 per-series-group flow specifically - it sits on page 86, before 8.1's own heading, so it's chapter-level content rather than belonging to any single numbered subsection, including 8.2 or 8.7 despite their similar titles).

## Overview

**[SPEC]** "Select Best Patient Series involves reviewing all potential patient series which might satisfy the goals of an antigen and determining the one or more relevant patient series which best fits the patient needs based on several important factors." Table 8-1 lists 8.1 through 8.8 as this chapter's activities. **[SPEC]** "Process steps 8.1 through 8.7 are repeated for each series group to identify one prioritized patient series per series group. Process step 8.8 is then used to determine which prioritized patient series are selected as a best patient series."

## Process flow (verified against the actual Java transitions, not assumed from the table order)

Chapter 8 is **not** a simple linear 8.1→8.2→...→8.8 chain - it has two real branch points:

1. **8.1 → 8.2** (always).
2. **8.2 branches**: if Table 8-3 finds one obvious prioritized series, skip straight to **8.8**; otherwise continue to **8.3**.
3. **8.3 branches three ways** based on Table 8-5's classification: **8.4** (2+ complete series), **8.5** (2+ in-process, 0 complete), or **8.6** (0 valid doses everywhere - also the fallback default; see 8.3's Review Findings for a possible gap in that partition).
4. **8.4, 8.5, and 8.6 all converge on 8.7** after scoring.
5. **8.7 → 8.8** (always) - selects the single highest-scoring series.
6. **8.8 → 4.5**, NOT back into Chapter 8 - 4.5 (`SelectBestPatientSeries`, Chapter 4's antigen-loop driver) either loops back to **8.1** for the next antigen, or proceeds to **4.6** (Identify and Evaluate Vaccine Group) once every antigen has been processed. This confirms Chapter 8 runs once per antigen, and within that, 8.1-8.7 runs once per series group for that antigen.

## Subordinate steps

| Section | Title | Status |
| --- | --- | --- |
| [8.1](08-01-pre-filter-patient-series/index.md) | Pre-filter Patient Series | draft |
| [8.2](08-02-identify-one-prioritized-patient-series/index.md) | Identify One Prioritized Patient Series | draft |
| [8.3](08-03-classify-scorable-patient-series/index.md) | Classify Scorable Patient Series | draft |
| [8.4](08-04-complete-patient-series/index.md) | Complete Patient Series | draft |
| [8.5](08-05-in-process-patient-series/index.md) | In-process Patient Series | draft |
| [8.6](08-06-no-valid-doses/index.md) | No Valid Doses | draft |
| [8.7](08-07-select-prioritized-patient-series/index.md) | Select Prioritized Patient Series | draft |
| [8.8](08-08-determine-best-patient-series/index.md) | Determine Best Patient Series | draft |

Note: 8.4, 8.5, and 8.6 form a scoring family - each awards points to a specific patient-series classification using the same "+N alone / tie / -N" pattern rather than a Yes/No decision grid. Table 8-13 and Table 8-14 are misattributed to 8.7 in the master extraction inventory (`logic-spec/versions/4.6/index.md`) and in `extracted/tables/table-8-13.txt` (a caption-matching extraction error); the correct attribution - Table 8-13 to 8.7, Table 8-14 to 8.8 - is used throughout these step packages, verified against both the raw section text and the Java source.

## Real implementation gaps surfaced while documenting this chapter

Recorded here for visibility; each is detailed with code citations in its own step's Review Findings. This chapter's issues are more numerous but individually smaller in scope than Chapter 7's (7.3's missing contraindication logic remains the single most significant finding across the documentation effort so far) - they cluster entirely in the 8.4-8.6 scoring family:

- **8.4 (verified bug)**: the "has the most valid doses" scoring condition breaks out of its loop after crediting the *first* series found at the maximum count, so a genuine tie leaves the other tied series unscored rather than correctly netting to zero. The sibling implementation in 8.5 handles the identical spec pattern correctly, confirming this is a real, isolated gap in 8.4 rather than a framework limitation.
- **8.5 (verified bug)**: the "can finish earliest" condition compares `Date` objects with `==`/`!=` (Java reference equality) instead of `.equals()`, so its tie-detection essentially never fires in practice.
- **8.6 (verified bug)**: the "is completable" condition's if/else both increment the score - there is no code path that decrements, so this condition always contributes +1 regardless of whether the series is actually completable. Also reuses the same Date reference-equality bug as 8.5 for its "can start earliest" condition.
- **8.6 (unconfirmed discrepancy)**: implements two scoring conditions (gender match, exceeded maximum age) that Table 8-11 does not define at all - unclear whether this is an intentional, undocumented refinement or logic that doesn't belong here.
- **8.1 (unconfirmed discrepancy)**: the imperative filtering logic doesn't map cleanly onto SELECTSCORE-2's four literal spec bullets - may be an equivalent restructuring or a real gap; this pass didn't trace far enough to be certain.

None of these have been reviewed by a domain expert yet - draft observations, not confirmed defects, except where explicitly marked "verified bug" above (each of those three is directly evidenced by the cited code, not inferred).
