# Vaccine Groups

> **Review status:** draft.

## What this covers

What a "vaccine group" is, why it's a separate concept from a [patient series](patient-series.md), and the single- vs. multiple-antigen branch that [Chapter 9](../steps/chapter-09-index.md) resolves for every group. This is the engine's final consolidation step - what it actually hands back as output.

## Explanation

**[SPEC]** "Identify and evaluate vaccine group combines patient series into a vaccine group-based forecast to provide a common and consistent view for a forecast. In the evaluation, forecasting, and select patient series chapters, all logic was specified for antigens. At this point it is important to define how those antigen-based evaluation and forecasting results can be merged into vaccine group forecasts." (page 93, `../steps/chapter-09-index.md`'s Source). **[SPEC]** Most vaccine groups correspond to exactly one antigen (e.g. "Polio" is both the antigen and the vaccine group); a small, explicitly-named minority combine several: "At present, MMR and DTaP/Tdap/Td vaccine groups are comprised of multiple antigens. MMR contains the antigens Measles, Mumps, and Rubella. DTaP/Tdap/Td contains the antigens Diphtheria, Tetanus, and Pertussis."

This is *why* Chapter 9 exists as a distinct final step rather than letting Chapter 8's best-patient-series output stand as the answer directly: a clinician or downstream system asking "is this patient up to date on MMR?" needs one consolidated answer, not three separate answers for Measles, Mumps, and Rubella that might disagree (one complete, two not).

### The single- vs. multiple-antigen branch

**[SPEC]** Table 9-1 states 9.1's second goal is "to classify the vaccine group type (Single Antigen or Multiple Antigen) for subsequent business rule sections (9.2 or 9.3)" - i.e. this is a branch, not a sequence. **[IMPLEMENTATION]** `ApplyGeneralVaccineGroupRules` (9.1) decides this via `VACCINEGROUP-1`/`VACCINEGROUP-2` and routes to [9.2 Single Antigen Vaccine Group](../steps/09-02-single-antigen-vaccine-group/index.md) or [9.3 Multiple Antigen Vaccine Group](../steps/09-03-multiple-antigen-vaccine-group/index.md) accordingly - see `../steps/chapter-09-index.md`'s Process Flow for the verified loop this sits inside (every vaccine group is visited once per engine run, in a loop that terminates in `END`).

For a single-antigen group, consolidation is close to a pass-through (the one antigen's patient series result essentially *is* the group's result). For a multiple-antigen group, [9.3](../steps/09-03-multiple-antigen-vaccine-group/index.md)'s Table 9-4 - a six-condition cascading decision table - decides the group's status from the combination of its member antigens' statuses (e.g. any one `CONTRAINDICATED` member makes the whole group `CONTRAINDICATED`; all members `COMPLETE`/`IMMUNE` makes the group `COMPLETE`), and 9.3's other business rule (`MULTIANTVG-1`, `FORECASTPRIORITY-1`) picks which member antigen's dates/reason to surface as the group's own forecast when more than one member is still due.

### Domain model

**[IMPLEMENTATION]** Three classes carry this, verified directly:

- **`VaccineGroup`** (`core/domain/VaccineGroup.java`) - the Supporting Data definition: a name, a list of member `Vaccine`s, a list of member `Antigen`s, and an `administerFullVaccineGroup` flag (`YesNo`).
- **`VaccineGroupStatus`** (`core/domain/VaccineGroupStatus.java`) - a six-value enum, **value-identical to `PatientSeriesStatus`** (`COMPLETE, CONTRAINDICATED, IMMUNE, NOT_COMPLETE, NOT_RECOMMENDED, AGED_OUT`), with a static `getVaccineGroupStatus(PatientSeriesStatus)` mapping each patient-series status directly across.
- **`VaccineGroupForecast`** (`core/domain/VaccineGroupForecast.java`, extends `Forecast` - see [target-dose.md](target-dose.md) for `Forecast`'s own fields) - the actual output object: adds a `vaccineGroupStatus`, a `patientSeriesStatus` (kept alongside the vaccine-group status, defaulting to `NOT_COMPLETE`), a `vaccineGroup` back-reference, a list of member `antigensNeededList`, and a `forecastList` of the individual per-antigen `Forecast`s that were consolidated. This is the object `cdsi-fits-tests`' `FitsEngineRunner` reads to compare against a FITS fixture's expected outcome (matching by CVX equivalence - see the main project's `cdsi-fits-tests` module) and what `cdsi-web`'s forecast servlet ultimately renders.

## Where it applies

[Chapter 9](../steps/chapter-09-index.md) (all of it) · [9.2](../steps/09-02-single-antigen-vaccine-group/index.md) · [9.3](../steps/09-03-multiple-antigen-vaccine-group/index.md) · [patient-series.md](patient-series.md) (what feeds into a vaccine group) · [statuses.md](statuses.md) (the shared status vocabulary).

## Open questions

- `VaccineGroupForecast` keeps both a `vaccineGroupStatus` and a `patientSeriesStatus` field, independently settable. This documentation pass didn't trace every place both get read to confirm they're always kept in sync (they have a mapping method, `setVaccineGroupStatus(PatientSeriesStatus)`, but nothing prevents a caller from setting one without the other) - worth a closer look if a future FITS discrepancy ever shows the two disagreeing.
- Per [9.3's Review Findings](../steps/09-03-multiple-antigen-vaccine-group/index.md), `MULTIANTVG-1`'s "latest date administered" clause wasn't traced to a specific line of code in this pass - flagged unconfirmed there, not resolved here either.
