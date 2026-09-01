# 4.1 Gather Necessary Data

> **Review status:** draft. Reviewed against the extracted specification text (page 31) and the current `cdsi-engine` source. Not yet reviewed by anyone other than the agent that drafted it.

## Source

Logic Specification for ACIP Recommendations v4.6, page 31. No figures or tables are associated with this section. No business-rule identifiers appear in it.

*(Everything in this "Source" block is a direct citation. Everything below is either quoted/paraphrased spec text, clearly labeled as such, or a StepIntoCDSi implementation observation - never presented as if it were normative spec text.)*

## Purpose

**[SPEC]** Gathering all the necessary data is described as "a generic step which could technically be performed in several different ways." The specification states this step is "outside of the purview of this document and is only noted as a generic step in the process" - i.e., Chapter 4 onward assumes this data already exists; it does not define *how* to obtain it.

The specification lists the data an implementation needs before evaluation/forecasting can begin, in two categories:

- **Patient-related data:** Patient, Vaccine Dose Administered, Vaccine, Immunization History, Adverse Reaction, Patient Observations.
- **Evaluation and forecasting data:** Schedule, Antigen Series, Series Dose, Vaccine Group, Antigen, Vaccine.

**[SPEC]** The document is explicit that "gather" does not imply a fetch/get/retrieve operation - some data may be passed in by an external caller, some may already be known, and some may arrive later in the process on an as-needed basis. Section 4.1 is an acknowledgement of the *minimum data needed*, not a data-access algorithm.

## Entry Conditions

**[SPEC]** None stated - this is the first step of the overall processing model (Table 4-1 / Figure 4-1, Section 4).

## Inputs and Attributes

**[SPEC]** No attribute table is defined for this section (unlike most subsequent steps). The two data-category lists above are the section's only structured content.

**[IMPLEMENTATION]** `cdsi-engine` represents these as a single input object, `org.openimmunizationsoftware.cdsi.core.data.ForecastInput`: patient date of birth, patient sex, assessment date, a list of administered vaccinations (date, CVX, MVX, optional dose condition), and a list of observations (code, date). This is a project-defined structure, not something the specification names - see Review Findings.

## Business Rules

**[SPEC]** None defined in this section.

## Decision Tables

**[SPEC]** None defined in this section.

## State Changes

**[IMPLEMENTATION]** `GatherNecessaryData.process()` reads the `ForecastInput` already attached to the data model and populates:

- a `Patient` (date of birth, gender) on the data model,
- an `ImmunizationHistory` containing one `VaccineDoseAdministered` per input vaccination (resolving each CVX code against the loaded supporting data's CVX map; throws `IllegalArgumentException` for an unrecognized code),
- zero or more `PatientObservation`s (resolving each observation code against the loaded supporting data's observation map; same failure behavior for an unrecognized code).

It does not itself load Schedule/Antigen Series/Series Dose/Vaccine Group/Antigen data - those are loaded earlier, when the `DataModel` is constructed (`DataModelLoader.createDataModel`), not as part of this step.

## Next Steps

**[SPEC]** Not defined - see Purpose. Section 4 (the chapter overview) simply lists 4.1 as the first numbered activity in Table 4-1 without describing branching.

**[IMPLEMENTATION]** Unconditional transition to **4.2 Organize Immunization History** (`LogicStepType.ORGANIZE_IMMUNIZATION_HISTORY`). See `transitions.yaml`.

## Plain-Language Walkthrough

Before any CDSi evaluation or forecasting logic can run, something has to hand the engine a patient's basic facts and immunization history. The specification deliberately leaves that "something" undefined - it's treated as a black box so the rest of the document can talk about evaluation and forecasting without getting tangled up in how a particular system (an IIS, an EHR, a sandbox) happens to source its data.

In `cdsi-engine`, that black box is `ForecastInput`: whatever the caller is - a web form, a FITS test-case JSON fixture, a FHIR `$immds-forecast` operation - adapts its own input shape into a `ForecastInput` before the engine ever runs. `GatherNecessaryData` is the one step that reads it and turns it into the domain objects (`Patient`, `ImmunizationHistory`, `VaccineDoseAdministered`, `PatientObservation`) that every subsequent step operates on. It doesn't know or care where the `ForecastInput` came from.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.GatherNecessaryData` (LogicStepType `GATHER_NECESSARY_DATA`) - `cdsi-engine`.
- `org.openimmunizationsoftware.cdsi.core.data.ForecastInput` - the input adapter type this step consumes.
- Structured log events: none emitted by this step currently (no `log(...)`/`alert(...)` calls in `GatherNecessaryData`).
- Tests: no dedicated unit test (see `step.yaml`'s `tests` entry). Exercised indirectly by every `cdsi-fits-tests` fixture, since it is the first step of every run.

## Review Findings

- **Spec vs. implementation scope mismatch (informational, not a defect):** the specification explicitly declines to define this step's behavior ("outside of the purview of this document"), so `cdsi-engine`'s concrete `ForecastInput` shape and its two-branch-turned-one-branch input handling are entirely project decisions with no specification text to verify them against. There is nothing here to classify as `IMPLEMENTATION_MISMATCH` - there is no specification requirement to mismatch.
- **Unresolved question:** the specification's "Evaluation and forecasting data" list (Schedule, Antigen Series, Series Dose, Vaccine Group, Antigen, Vaccine) is *supporting data*, loaded once per `DataModel`, not per-request input in the way patient data is. Whether the specification intends this list to be read as "supporting data must exist" (satisfied) versus "gathered per request" (not applicable to how CDSi supporting data actually works) is not stated. Recorded here rather than resolved silently.
