# Domain Model

> **Review status:** draft.

## What this covers

A navigational map from `cdsi-engine`'s actual `org.openimmunizationsoftware.cdsi.core.domain` package (and its `datatypes` subpackage) to the specification's own vocabulary. This is not an exhaustive per-field reference - concepts with their own dedicated document ([target-dose.md](target-dose.md), [patient-series.md](patient-series.md), [vaccine-groups.md](vaccine-groups.md), [statuses.md](statuses.md)) are linked, not repeated here.

Class list verified directly (`ls cdsi-engine/src/main/java/org/openimmunizationsoftware/cdsi/core/domain/` and its `datatypes/` subdirectory), not transcribed from memory.

## Explanation

**[IMPLEMENTATION]** The domain package groups into four rough categories, matching the specification's own patient-data / supporting-data / processing-state / output layering (see `docs/15-separation-of-concerns-in-cdsi-architecture.md` in the main project root for that layering's own rationale):

### Patient data (what the caller provides)

`Patient`, `PatientObservation`, `ObservationCode`, `MedicalHistory`, `ClinicalHistory`, `ImmunizationHistory`, `VaccineDoseAdministered`, `AntigenAdministeredRecord`, `AdverseReaction`. This is the raw input a `ForecastInput` (see `cdsi-engine`'s `core.data` package - outside `core.domain` itself, but the thing that populates most of these) gets turned into during [4.1 Gather Necessary Data](../steps/04-01-gather-necessary-data/index.md) and [4.2 Organize Immunization History](../steps/04-02-organize-immunization-history/index.md).

### Supporting Data / schedule definitions (what the CDC schedule data defines, loaded once)

`Antigen`, `AntigenSeries`, `SeriesDose`, `Vaccine`, `VaccineType`, `Schedule`, `VaccineGroup` (see [vaccine-groups.md](vaccine-groups.md)), `Age`, `Interval`, `AllowableInterval`, `IntervalPriority`, `RecurringDose`, `RequiredGender`, `SeasonalRecommendation`, `PreferrableVaccine`, `AllowableVaccine`, `SubstituteDose`, `SkipTargetDose`, `ConditionalSkip`, `ConditionalSkipSet`, `ConditionalSkipCondition`, `ConditionalSkipConditionType`, `Indication`, `Contraindication`, `AntigenContraindication`, `VaccineContraindication`, `Immunity`, `BirthDateImmunity`, `LiveVirusConflict`, `Observation`, `ClinicalGuidelineObservation`, `SelectPatientSeries` (series-group/priority/preference metadata used by [Chapter 8](../steps/chapter-08-index.md)'s selection logic - despite the generic-sounding name, this is a Supporting Data value holder, not a processing-state class), `DoseType`, `SeriesType`. See [selecting-supporting-data.md](selecting-supporting-data.md) for how a specific patient's applicable subset of this data gets resolved.

### Processing state (built and mutated while evaluating/forecasting one patient)

`PatientSeries` (see [patient-series.md](patient-series.md)), `TargetDose` (see [target-dose.md](target-dose.md)), `Evaluation`, `Forecast`, `VaccineGroupForecast` (extends `Forecast` - see [vaccine-groups.md](vaccine-groups.md)). `Evaluation` is the per-target-dose-attempt record [Chapter 6](../steps/chapter-06-index.md) produces (status, reason, which `VaccineDoseAdministered` it was evaluated against) - `TargetDose.getEvaluationList()` can hold more than one, since a target dose can be evaluated against successive administered records until one satisfies it.

### Cross-cutting value types (`datatypes/` subpackage)

`EvaluationStatus`, `EvaluationReason`, `TargetDoseStatus`, `PatientSeriesStatus`, `VaccineStatus`, `DoseCondition`, `YesNo` (a three-valued enum used throughout Supporting Data for Yes/No/Unknown flags - see [decision-tables.md](decision-tables.md) for how this relates to `LogicResult`), `TimePeriod`/`TimePeriodType`/`TimeRange` (the age/interval calculation primitives - see [date-calculations.md](date-calculations.md)), `Stepper` (a generic position-tracking iterator wrapper used by several of the loops [overall-processing-model.md](overall-processing-model.md) describes, e.g. `dataModel.getPatientSeriesStepper()`).

### Naming oddities worth knowing about, not fixing

- **`Contraindication_TO_BE_REMOVED.java`** exists alongside a plain `Contraindication.java`. Per [7.3's Review Findings](../steps/07-03-determine-contraindications/index.md), `DetermineContraindications`'s own code comments confirm this is a real, acknowledged, unfinished migration ("the ContraindicationElements condition attribute cannot be set correctly until 'Contraindication_TO_BE_REMOVED' get[s] replaced with 'Contraindication'") - not a documentation artifact, an actual in-progress rename blocking real logic from being wired up.
- **`Contraindication` vs. `AntigenContraindication`/`VaccineContraindication`**: three distinct classes exist for what the specification describes as two contraindication levels (antigen and vaccine, [7.3](../steps/07-03-determine-contraindications/index.md)) - this pass did not trace how (or whether) the three relate, since 7.3 doesn't yet instantiate any of them into working logic.

## Where it applies

Referenced from every concept document and, indirectly, every step package - this is the shared vocabulary underneath all of them. Most directly: [target-dose.md](target-dose.md), [patient-series.md](patient-series.md), [vaccine-groups.md](vaccine-groups.md), [statuses.md](statuses.md).

## Open questions

- The exact relationship between `Contraindication`, `AntigenContraindication`, and `VaccineContraindication` is unresolved in this pass - see above. Since none of the three are currently wired into working logic ([7.3](../steps/07-03-determine-contraindications/index.md)), this is likely to matter more once that gap is closed than it does today.
- This document groups classes by inference from their names and the step packages that reference them, not by exhaustively reading every field of every class - treat it as a map to start from, not a complete inventory.
