# Chapter 7: Forecast Dates and Reasons

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 71-72 (chapter overview only - see linked step packages for each subsection). Table 7-1 (Forecast Dates and Reasons Process Steps), Figure 7-1 (Forecast Dates and Reason Process Model).

## Overview

**[SPEC]** "A CDS engine uses a patient's medical and vaccine history to forecast immunization due dates for a relevant patient series. It is also possible the patient would not be recommended additional doses. In that case, the outcome of the forecast will be an appropriate status (e.g., complete, immune, contraindicated) and no dates will be generated." Table 7-1 lists the six activities below as the chapter's process steps.

The chapter runs mostly as a linear chain (7.1 → 7.2 → 7.3 → 7.4 → 7.5 → 7.6), with two loop-back points to **4.4** (the evaluate-and-forecast driver): 7.1 (dose can be skipped) and 7.4 (any of seven "no dose needed" outcomes - complete, immune, contraindicated, out of season, aged out). 7.6 always ends at 4.4 too, though not for the reason the specification describes - see below.

## Subordinate steps

| Section | Title | Status |
| --- | --- | --- |
| [7.1](07-01-evaluate-conditional-skip/index.md) | Evaluate Conditional Skip | draft |
| [7.2](07-02-determine-evidence-of-immunity/index.md) | Determine Evidence of Immunity | draft |
| [7.3](07-03-determine-contraindications/index.md) | Determine Contraindications | draft |
| [7.4](07-04-determine-forecast-need/index.md) | Determine Forecast Need | draft |
| [7.5](07-05-generate-forecast-dates-and-recommended-vaccines/index.md) | Generate Forecast Dates and Recommended Vaccines | draft |
| [7.6](07-06-validate-recommendation/index.md) | Validate Recommendation | draft |

Note: 7.1 shares its implementation verbatim with Chapter 6's 6.2 (both titled "Evaluate Conditional Skip") - the specification itself calls this out as "defined once, used twice." 7.6 also reuses the same shared logic for a third context (`ConditionalSkipType.VALIDATING`), though as documented below, that reuse is currently unreachable.

## Real implementation gaps surfaced while documenting this chapter

Recorded here for visibility; each is detailed with code citations in its own step's Review Findings. **This chapter has the two most significant gaps found in the documentation effort so far**, both larger in scope than the single-condition gaps found in Chapters 5-6:

- **7.2**: the clinical-history path to evidence-of-immunity (as opposed to the birth-date path) is hardcoded to never fire - the specification's own worked example (a documented immune clinical finding) is the unreachable path.
- **7.3 (most significant so far)**: `DetermineContraindications` implements **none** of its three decision tables - no antigen contraindication, vaccine contraindication, or combined series-contraindication logic runs at all. The class's own comments confirm this is known, unfinished work. No patient series is ever marked Contraindicated by this step. This also makes 7.4's own contraindication check effectively dead, since it reads a data structure 7.3 never populates.
- **7.4**: FORECASTDTCAN-1's earliest-date calculation appears to omit two of its six specified candidate dates (conflict-end date, seasonal start date) - unverified whether this is compensated elsewhere.
- **7.5**: FORECASTRECVAC-1 (recommended vaccine selection), FORECASTDN-1 (forecast dose number), and FORECASTGUIDANCE-1 (administrative guidance) were not found implemented in this class, and this pass did not exhaustively search elsewhere - unverified, not confirmed missing.
- **7.6**: `ValidateRecommendation` overrides `process()` to skip its inherited conditional-skip validation entirely, always proceeding as if the forecast were valid. The specification's stated re-forecast-if-illogical behavior does not currently run.

None of these have been reviewed by a domain expert yet - they are draft observations, not confirmed defects. Two (7.3, 7.6) look like genuinely unfinished work rather than subtle bugs, based on the source's own comments.
