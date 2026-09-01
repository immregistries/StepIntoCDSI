# Chapter 6: Evaluate Vaccine Dose Administered

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 45-46 (chapter overview only - see linked step packages for each subsection). Table 6-1 (Evaluation Process Steps), Figure 6-1 (Evaluation Process Model).

## Overview

**[SPEC]** "The core of a CDS engine is the process of evaluating a single vaccine dose administered against a defined target dose within a relevant patient series to determine if the vaccine dose administered is 'valid' or 'not valid' for the relevant patient series... This can be accomplished by breaking the evaluation process into simple logical components." Table 6-1 lists the ten activities below as the chapter's process steps, each with its own attributes and decision table(s).

The chapter runs as a linear chain from 6.1 to 6.10 for the common "everything passes" path, but several steps can loop back early to **4.4** (the evaluate-and-forecast driver) when a dose is rejected outright: 6.1 (lot expired / has a condition), 6.2 (conditional skip applies), 6.3 (inadvertent vaccine), and 6.10 always (whether satisfied or not, since 6.10 is the chapter's own loop-closing step). 6.5 through 6.9 never loop back early - a failed interval, conflict, or vaccine-type check is recorded (via the target dose's `statusCause` field) and only acted on once all of them have run, at 6.10.

## Subordinate steps

| Section | Title | Status |
| --- | --- | --- |
| [6.1](06-01-evaluate-dose-administered-condition/index.md) | Evaluate Dose Administered Condition | draft |
| [6.2](06-02-evaluate-conditional-skip/index.md) | Evaluate Conditional Skip | draft |
| [6.3](06-03-evaluate-for-inadvertent-vaccine/index.md) | Evaluate for Inadvertent Vaccine | draft |
| [6.4](06-04-evaluate-age/index.md) | Evaluate Age | reviewed |
| [6.5](06-05-evaluate-preferable-interval/index.md) | Evaluate Preferable Interval | draft |
| [6.6](06-06-evaluate-allowable-interval/index.md) | Evaluate Allowable Interval | draft |
| [6.7](06-07-evaluate-vaccine-conflict/index.md) | Evaluate Vaccine Conflict | draft |
| [6.8](06-08-evaluate-for-preferable-vaccine/index.md) | Evaluate for Preferable Vaccine | draft |
| [6.9](06-09-evaluate-for-allowable-vaccine/index.md) | Evaluate for Allowable Vaccine | draft |
| [6.10](06-10-satisfy-target-dose/index.md) | Satisfy Target Dose | draft |

Note: 6.2's logic (Tables 6-4 through 6-11) is shared verbatim with Chapter 7's 7.1, which has the identical title "Evaluate Conditional Skip" - see [6.2's index.md](06-02-evaluate-conditional-skip/index.md).

## Real implementation gaps surfaced while documenting this chapter

Recorded here for visibility; each is detailed with code citations in its own step's Review Findings:

- **6.1**: CALCDTLOTEXP-1 (lot expiration date) is not actually calculated - the code comment says so.
- **6.2**: the "Completed Series" conditional-skip condition type (Table 6-7) is hardcoded to never be met.
- **6.5**: the "interval not satisfied" outcome sets the wrong `EvaluationReason` (`GRACE_PERIOD` instead of `TOO_SOON`) - confirmed by comparison with 6.6's correct equivalent.
- **6.8**: trade-name matching is hardcoded to always pass (not yet wired up to real data), making the "trade name mismatch" outcome currently unreachable.

None of these have been reviewed by a domain expert yet - they are draft observations, not confirmed defects.
