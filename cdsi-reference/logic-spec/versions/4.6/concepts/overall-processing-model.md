# Overall Processing Model

> **Review status:** draft.

## What this covers

The end-to-end engine flow across all of Chapters 4-9: what runs once, what loops, over which collection, and what condition ends each loop. This is the connective picture Table 4-1/Figure 4-1 sketch at a high level; the six chapter-overview indexes (`../steps/chapter-04-index.md` through `chapter-09-index.md`) already verified every individual transition below against the actual Java `setNextLogicStepType` calls - this document assembles those into one top-to-bottom account rather than re-deriving them.

## Explanation

**[SPEC]** Table 4-1 (page 30, `../logic-spec-acip-rec-4.6.pdf` or `extracted/sections/04-processing-model.txt`) frames the whole model as two parts: "The first part is very mechanical in nature and focuses on gathering and prepping all of the required data. The second part uses the data gathered earlier to generate the evaluation and forecast," and lists six activities, 4.1 through 4.6, each naming the chapter that elaborates it:

1. **4.1 Gather Necessary Data** - "gather all pertinent information which will be used in subsequent steps." See [4.1](../steps/04-01-gather-necessary-data/index.md). Runs once per forecast request.
2. **4.2 Organize Immunization History** - "break apart vaccine doses administered into their antigen parts." See [4.2](../steps/04-02-organize-immunization-history/index.md). Runs once.
3. **4.3 Create Relevant Patient Series** *(Chapter 5)* - "instantiate antigen series into relevant patient series for this patient." See [4.3](../steps/04-03-create-relevant-patient-series/index.md), [Chapter 5](../steps/chapter-05-index.md).
4. **4.4 Evaluate and Forecast All Patient Series** *(Chapters 6-7)* - "evaluate each antigen administered and create a forecast for each relevant patient series." See [4.4](../steps/04-04-evaluate-and-forecast-all-relevant-patient-series/index.md), [Chapter 6](../steps/chapter-06-index.md), [Chapter 7](../steps/chapter-07-index.md).
5. **4.5 Select Best Patient Series** *(Chapter 8)* - "select one or more best patient series for the patient based on their evaluated history and forecast." See [4.5](../steps/04-05-select-best-patient-series/index.md), [Chapter 8](../steps/chapter-08-index.md).
6. **4.6 Identify and Evaluate Vaccine Group** *(Chapter 9)* - "merge together patient series forecasts into a vaccine group forecast." See [4.6](../steps/04-06-identify-and-evaluate-vaccine-group/index.md), [Chapter 9](../steps/chapter-09-index.md).

Figure 4-2 (page 31) frames this as the patient-series population narrowing at each stage: **Antigen Series → Relevant Patient Series → Scorable Patient Series → Prioritized Patient Series → Best Patient Series**, each set selected from the previous one by that stage's criteria.

### The loop structure, assembled top to bottom

**[IMPLEMENTATION]** There are four nested loops. From outermost to innermost:

**Loop 1 - antigens** (drives 4.3 → 5.1 → 4.4 → 4.5 → 8.1, all for one antigen, repeated per antigen):
- **4.3** (`CreateRelevantPatientSeries`) iterates the antigen list: while antigens remain, go to **5.1** for the current antigen; once exhausted, proceed to **4.4**.
- **4.5** (`SelectBestPatientSeries`) similarly loops per antigen: while antigens remain, go to **8.1**; once exhausted, proceed to **4.6**.
- Termination: the antigen list is exhausted (a fixed, finite collection built once in 4.2).

**Loop 2 - target dose × administered record, inside one antigen's evaluation** (4.4, delegating to Chapters 6-7 per iteration):
- [4.4](../steps/04-04-evaluate-and-forecast-all-relevant-patient-series/index.md) walks the current patient series' target-dose list against its administered-record list together: evaluate the current AAR against the current target dose (Chapter 6); if satisfied, advance to the next target dose (inserting a duplicate if it's a recurring dose, e.g. yearly flu); if not, keep the target dose and advance to the next AAR.
- Termination: target doses exhausted (any remaining AARs become `EXTRANEOUS`), or AARs exhausted (evaluation ends and forecasting, Chapter 7, begins on whichever target dose evaluation stopped at).
- **[IMPLEMENTATION - no spec basis]** Two additional exits exist purely as safety nets, not specification requirements: a >1000-cycle total-count guard and a >200-repeated-state guard, both forcing an early exit to 4.5 rather than letting the engine hang. See 4.4's own Review Findings.

**Loop 3 - the evaluation chain within one target-dose/AAR pairing** (Chapter 6, 6.1 → 6.10):
- A linear chain for the common path, with four early-exit points back to 4.4 when a dose is rejected outright (6.1 lot-expired/conditioned, 6.2 conditional skip, 6.3 inadvertent vaccine, and 6.10 always, since it's the chapter's own loop-closing step). See [Chapter 6](../steps/chapter-06-index.md) for the exact per-step conditions.
- Termination: reaching 6.10 (satisfied or not) always returns control to 4.4's target-dose/AAR loop (Loop 2).

**Loop 4 - the forecasting chain, once evaluation stops on an unsatisfied target dose** (Chapter 7, 7.1 → 7.6):
- Mostly linear, with two early-exit points back to 4.4 (7.1 skip applies; 7.4 any of seven "no dose needed" outcomes - complete, immune, contraindicated, out of season, aged out) and one unconditional exit at 7.6 (see [Chapter 7](../steps/chapter-07-index.md) - 7.6's exit isn't for the reason the specification describes; its own inherited validation logic never actually runs, see its Review Findings).
- Termination: reaching 7.6 (or an earlier exit) returns control to 4.4.

**After Loop 1 closes** (all antigens processed): 4.6 hands off to **Chapter 9**, which is itself a fifth loop, per vaccine group:
- **[SPEC]** "identify and evaluate vaccine group combines patient series into a vaccine group-based forecast" (page 93). **[IMPLEMENTATION]** The chapter-overview section "9" (`IdentifyAndEvaluateVaccineGroup`) is the loop driver: while vaccine groups remain, set the current one and go to 9.1; 9.1 branches to 9.2 (single antigen) or 9.3 (multiple antigen) per `VACCINEGROUP-1`/`-2`; both return unconditionally to "9". Once every vaccine group is processed, "9" transitions to **END** - this is where the whole engine run terminates. See [Chapter 9](../steps/chapter-09-index.md) for the full cycle diagram.

### Why this matters for debugging

Consistent with the main project's own prior analysis in `docs/01-overview-subchapter-loops.md` and `docs/11-processing-model-orchestration.md` (both read while writing this document, and still accurate against the step packages built during this documentation effort): most CDSi implementation defects are orchestration defects - the wrong iterator advancing, a loop exiting one step too early or late - not incorrect rule math within a single decision table. A rule defect (like the `EvaluatePreferableInterval` bug documented in [6.5](../steps/06-05-evaluate-preferable-interval/index.md)) typically affects one scenario; a loop-control defect affects nearly every scenario that passes through it. When debugging, check which loop and which position within it (which antigen, which target dose, which AAR, which vaccine group) before suspecting the decision table itself.

## Where it applies

Every step package in Chapters 4-9 is a participant in one of these four+one loops - see each chapter's own index for that chapter's specific entry/exit conditions, already verified against code:
[Chapter 4](../steps/chapter-04-index.md) (outer orchestration) · [Chapter 5](../steps/chapter-05-index.md) (5.1, inside Loop 1) · [Chapter 6](../steps/chapter-06-index.md) (Loop 3) · [Chapter 7](../steps/chapter-07-index.md) (Loop 4) · [Chapter 8](../steps/chapter-08-index.md) (has its own internal loop structure, described there) · [Chapter 9](../steps/chapter-09-index.md) (Loop 5).

## Open questions

- The two loop-guard escapes in 4.4 (cycle-count and repeated-state limits) have no specification basis - they're pure implementation safety nets. Worth confirming with a domain expert whether their thresholds (1000 cycles, 200 repeated states) are principled or arbitrary, though this isn't a behavioral concern for well-formed input - only a question of whether a pathological input could still exceed them before triggering.
- The specification does not name Chapter 8's per-antigen, per-series-group loop nesting explicitly the way it numbers Chapter 4's overall model - Chapter 8's own index (`../steps/chapter-08-index.md`) documents this from code, not from an explicit spec statement to that effect.
