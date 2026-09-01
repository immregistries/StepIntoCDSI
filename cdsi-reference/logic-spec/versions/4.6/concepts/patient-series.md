# Patient Series

> **Review status:** draft.

## What this covers

What a "patient series" is, and how one moves through its full lifecycle - created, evaluated, forecast, scored, and selected - across Chapters 5, 6, 7, and 8. This is one of the two central objects the whole engine revolves around (the other being [vaccine groups](vaccine-groups.md), which patient series ultimately consolidate into).

## Explanation

**[SPEC]** A patient series is a Supporting Data-defined antigen series ("`AntigenSeries`") that has been judged appropriate for a specific patient - see [5.1 Select Relevant Patient Series](../steps/05-01-select-relevant-patient-series/index.md) for exactly how that appropriateness is decided (gender, series type, and applicable indications). Not every antigen series a schedule defines becomes a patient series for a given patient; only the ones 5.1 selects do.

**[IMPLEMENTATION]** `org.openimmunizationsoftware.cdsi.core.domain.PatientSeries` is the corresponding class: it wraps a `trackedAntigenSeries` (the Supporting Data definition), a `List<TargetDose>` (the doses this patient specifically needs to satisfy this series), a `Forecast`, an integer `scorePatientSeries`, and a `PatientSeriesStatus`.

### Lifecycle

1. **Created** - [5.1](../steps/05-01-select-relevant-patient-series/index.md) instantiates a `PatientSeries` for each antigen series Table 5-5's decision logic finds relevant, and adds it to the data model's tracked series (`dataModel.getPatientSeriesStepper().add(...)`).
2. **Evaluated** - for each `TargetDose` in the series, [Chapter 6](../steps/chapter-06-index.md)'s ten-step chain evaluates administered records against it (dose-administered condition, conditional skip, inadvertent vaccine, age, preferable/allowable interval, vaccine conflict, preferable/allowable vaccine, then 6.10 records whether the target dose was satisfied). See [overall-processing-model.md](overall-processing-model.md) for how this nests inside 4.4's target-dose/AAR loop.
3. **Forecast** - once evaluation stops on an unsatisfied target dose, [Chapter 7](../steps/chapter-07-index.md) determines whether a dose is still needed at all (evidence of immunity, contraindications - though see the Open Questions below - forecast need) and, if so, generates the actual forecast dates ([7.5](../steps/07-05-generate-forecast-dates-and-recommended-vaccines/index.md)) that populate the series' `Forecast` object.
4. **Classified for scoring** - back in [Chapter 8](../steps/chapter-08-index.md), once every relevant series for an antigen has been evaluated and forecast, [8.3 Classify Scorable Patient Series](../steps/08-03-classify-scorable-patient-series/index.md) sorts the scorable ones into exactly one of three buckets per series group: **Complete** ([8.4](../steps/08-04-complete-patient-series/index.md)), **In-Process** ([8.5](../steps/08-05-in-process-patient-series/index.md)), or **No Valid Doses** ([8.6](../steps/08-06-no-valid-doses/index.md)) - each bucket has its own scoring table that sets `scorePatientSeries` via `incPatientScoreSeries()`/`descPatientScoreSeries()`/`addScore(...)`.
5. **Selected** - [8.7](../steps/08-07-select-prioritized-patient-series/index.md) picks the single highest-scoring series per series group as that group's prioritized series; [8.8](../steps/08-08-determine-best-patient-series/index.md) then decides whether each series group's prioritized series is also a *best* patient series for the antigen (Table 8-14).
6. **Consolidated** - the resulting best patient series feed into [Chapter 9](../steps/chapter-09-index.md)'s vaccine-group evaluation - see [vaccine-groups.md](vaccine-groups.md) for what happens next.

### Status

**[SPEC]** Table 3-3 Patient Series Statuses (page 25 - see [statuses.md](statuses.md) for the full three-tier status explanation, this section only covers how a *series'* status specifically gets set and read). **[IMPLEMENTATION]** `PatientSeriesStatus` (`core/domain/datatypes/PatientSeriesStatus.java`) is a six-value enum: `COMPLETE, CONTRAINDICATED, IMMUNE, NOT_COMPLETE, NOT_RECOMMENDED, AGED_OUT`. A `PatientSeries`'s status is set at multiple points in the lifecycle above (e.g. a Chapter 7 outcome like "patient has evidence of immunity" sets `IMMUNE`; a Chapter 8 classification outcome can set `COMPLETE`), and this status - not the score - is what [Chapter 9](../steps/chapter-09-index.md) ultimately reads to determine `VaccineGroupStatus` (verified: `VaccineGroupStatus` is a value-identical enum to `PatientSeriesStatus`, and `VaccineGroupForecast.setVaccineGroupStatus(PatientSeriesStatus)` maps one to the other 1:1).

## Where it applies

[5.1](../steps/05-01-select-relevant-patient-series/index.md) (creation) · [Chapter 6](../steps/chapter-06-index.md) (evaluation) · [Chapter 7](../steps/chapter-07-index.md) (forecasting, status-setting) · [8.3](../steps/08-03-classify-scorable-patient-series/index.md)-[8.8](../steps/08-08-determine-best-patient-series/index.md) (classification, scoring, selection) · [vaccine-groups.md](vaccine-groups.md) (consolidation) · [target-dose.md](target-dose.md) (the per-dose unit a series tracks).

## Open questions

- Per [7.3's Review Findings](../steps/07-03-determine-contraindications/index.md), the contraindication-driven path to `PatientSeriesStatus.CONTRAINDICATED` currently never fires (no decision-table logic populates the data `DetermineContraindications` is supposed to evaluate) - a series can theoretically reach `CONTRAINDICATED` status per the enum's existence, but not via that intended path, as of this documentation pass.
- Several of the Chapter 8 scoring bugs documented in [8.4](../steps/08-04-complete-patient-series/index.md), [8.5](../steps/08-05-in-process-patient-series/index.md), and [8.6](../steps/08-06-no-valid-doses/index.md) affect `scorePatientSeries`, which in turn affects which series 8.7 selects as prioritized - this document doesn't re-derive the downstream selection impact of those bugs; see each step's own Review Findings.
