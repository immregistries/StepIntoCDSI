# Target Dose

> **Review status:** draft.

## What this covers

The single most-used unit of work in the whole specification: a target dose is one specific, patient-specific dose slot within a series that has to be satisfied (or explicitly skipped) before the patient can move on to the next one. Source: Logic Specification for ACIP Recommendations v4.6, section 3.1 "Target Dose" (page 23), Figure 3-1. Spans Chapters 5 through 8.

## Explanation

**[SPEC]** "A target dose is a patient-specific dose required to satisfy the recommendations of ACIP. Until a target dose is satisfied, the patient is not allowed to move to the next target dose in the patient series. The patient remains on the 'unsatisfied' target dose until the patient has a 'valid' vaccine dose administered that satisfies the target dose." A target dose can also be skipped instead of satisfied - the spec explicitly defers that detail to Chapters 6 and 7 rather than explaining it in section 3.1.

Figure 3-1 illustrates this with a hypothetical series whose three target doses are defined only by minimum age (0 days, 2 months, 6 months): an administered dose given too early evaluates to "not valid" and does not satisfy its target dose; a dose given at an appropriate age evaluates to "valid," satisfies the target dose, and the patient series advances to the next target dose. The spec ties this directly to two other concepts documented separately: an administered dose's [evaluation status](statuses.md) (Valid/Not Valid/Extraneous/Sub-standard) determines whether it *can* satisfy a target dose, and each target dose itself carries its own status (Not Satisfied/Satisfied/Skipped - see [statuses.md](statuses.md)) tracking whether that particular slot has been filled yet. A whole patient series' own status (Not Complete/Complete/etc.) is a third, still separate, level - **[SPEC]** the worked example is explicit that patient series status stays "Not Complete" through the first three administered doses and only becomes "Complete" once the fourth dose satisfies the third (final) target dose.

So there are three nested things being tracked simultaneously, at three different scopes: one administered dose's evaluation outcome, one target dose's satisfaction state, and one whole series' completion state. A target dose is the middle layer, and the one everything else in Chapters 6-8 is organized around.

**[IMPLEMENTATION]** `cdsi-engine` represents this directly: `org.openimmunizationsoftware.cdsi.core.domain.TargetDose` is the runtime object being evaluated, tracking a `TargetDoseStatus` (`NOT_SATISFIED`, `SATISFIED`, `SKIPPED`, and two values not named in Table 3-2 at all - `SUBSTITUTED`, `UNNECESSARY`; see Open Questions) and a list of `Evaluation`s, one per administered dose that was checked against it. `DataModel` steps through a patient's target doses one at a time (`dataModel.getTargetDose()`), which is exactly the "processing model focuses on one target dose and one administered dose at a time" framing the specification itself uses (section 2.10.2, page 20) to justify separating rule logic from iteration/looping.

## Where it applies

- [5.1 Select Relevant Patient Series](../steps/05-01-select-relevant-patient-series/index.md) - creates the `PatientSeries` whose target doses will later be evaluated; doesn't touch target-dose state directly yet.
- The whole 6.1-6.9 evaluation chain - each step evaluates one administered dose against the *current* target dose's attributes, without changing the target dose's own status.
- [6.10 Satisfy Target Dose](../steps/06-10-satisfy-target-dose/index.md) - the step that actually decides whether the target dose's status becomes `SATISFIED` based on the evaluation chain's outcome; this is where the "moves to the next target dose" transition documented in section 3.1 actually happens in code.
- 7.1-7.6 (forecasting) - operates on the *next* unsatisfied target dose in a series to compute when it's due.
- 8.1-8.8 (best-series selection) - scores and compares patient series partly based on how many target doses are satisfied/in-process.

## Open questions

- `TargetDoseStatus` has two values (`SUBSTITUTED`, `UNNECESSARY`) that don't appear in Table 3-2 at all (which lists only Not Satisfied, Satisfied, Skipped). Not resolved here - possibly documented elsewhere in the specification (e.g. around recurring doses or seasonal recommendations) rather than in section 3.2's summary table, but that wasn't confirmed in this pass. Worth checking against Chapters 6-7's full text or Appendix A before treating this as a spec/code mismatch.
