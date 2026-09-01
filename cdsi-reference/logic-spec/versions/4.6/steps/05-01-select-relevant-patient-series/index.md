# 5.1 Select Relevant Patient Series

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 41-44. Figures 5-2 (Select Relevant Patient Series Process Model), 5-3 (Assess Indications Process Model). Tables 5-2 (Select Relevant Patient Series Attributes), 5-3 (Select Relevant Patient Business Rules), 5-4 (Does the Indication Apply to the Patient? - a decision table), 5-5 (Is An Antigen Series a Relevant Patient Series for a Patient? - a decision table). Business rules CALCDTIND-1, CALCDTIND-2.

**[Extraction note, not a specification issue]** Table 5-5's structured extraction (`extracted/tables/table-5-5.txt`) captured only its three CONDITIONS rows - its OUTCOMES row is on the following printed page (44) behind a repeated "CONDITIONS / RULES / OUTCOMES" header, one page-boundary pattern past what the current extractor's caption/next-page search handles. The outcome text below is taken directly from the raw section text (`extracted/sections/05-01-select-relevant-patient-series.txt`, page 44), which does contain it correctly, and cross-checked against `SelectRelevantPatientSeries.java`'s `LT55` outcomes - not invented.

## Purpose

**[SPEC]** "Select relevant patient series determines which series defined by the Supporting Data are appropriate to evaluate for the patient. Antigen series with a Series Type of 'Standard' or 'Evaluation Only' are relevant for all patients of the appropriate gender. Not all antigen series with a Series Type of 'Risk' will be appropriate for a given patient." When a Risk series' relevance can't be conclusively determined ("some or all indications are inconclusive and none unambiguously apply"), the spec says it "will not be evaluated or forecast, but a notification should be available to a clinician alerting them to the presence of the indication(s) which could not be resolved."

## Entry Conditions

**[SPEC]** Runs once per antigen, driven by 4.3's loop - implies at least one antigen series exists for the current antigen.

## Inputs and Attributes

**[SPEC]** Table 5-2 Select Relevant Patient Series Attributes:

| Attribute Type | Attribute Name | Assumed Value if Empty |
| --- | --- | --- |
| Patient | Gender | Unknown |
| Patient | Date of Birth | - |
| Patient history | Active Patient Observation(s) | - |
| Supporting Data (Gender) | Required Gender | Gender of the patient |
| Supporting Data (Series Type) | Series type | - |
| Supporting Data (Indication) | Observation Code | - |
| Runtime data | Assessment Date | current date |
| Calculated date (CALCDTIND-1) | Indication Begin Age Date | 01/01/1900 |
| Calculated date (CALCDTIND-2) | Indication End Age Date | 12/31/2999 |

**[IMPLEMENTATION]** `SelectRelevantPatientSeries`'s constructor creates these nine `ConditionAttribute`s exactly as named (`caGender`, `caDateOfBirth`, `caActivePatientObservations`, `caRequiredGender`, `caSeriesType`, `caObservationCode`, `caAssessmentDate`, `caIndicationBeginAgeDate`, `caIndicationEndAgeDate`), with the same assumed-value defaults (`"Unknown"`, `PAST`, `FUTURE`).

## Business Rules

**[SPEC]** Table 5-3 Select Relevant Patient Business Rules:

| Business Rule ID | Business Rule |
| --- | --- |
| CALCDTIND-1 | A patient's indication begin age date must be calculated as the patient's date of birth plus the indication begin age of an indication. |
| CALCDTIND-2 | A patient's indication end age date must be calculated as the patient's date of birth plus the indication end age of an indication. |

**[IMPLEMENTATION]** Both computed via `org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTIND_1`/`CALCDTIND_2`, called from the constructor as `CALCDTIND_1.evaluate(dataModel, this, indication)` / `CALCDTIND_2.evaluate(...)` and assigned as the two calculated attributes' initial values - not independently re-derived elsewhere in this class.

## Decision Tables

**[SPEC]** Table 5-4 Does the Indication Apply to the Patient?

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 |
| --- | --- | --- | --- | --- |
| Does the indication describe any active patient observations? | Yes | No | Unknown | - |
| Is the indication begin age date ≤ assessment date < indication end age date? | Yes | Yes | Yes | No |
| **Outcome** | Applies | Does not apply | Does not apply (flag for clinician) | Does not apply |

**[SPEC]** Table 5-5 Is An Antigen Series a Relevant Patient Series for a Patient?

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 |
| --- | --- | --- | --- | --- |
| Is the patient gender one of the required genders of the antigen series? | Yes | No | Yes | Yes |
| Is the series type of the antigen series 'Standard' or 'Evaluation Only'? | Yes | - | No | No |
| Does at least one indication that drives the need for the antigen series apply to the patient? | - | - | Yes | No |
| **Outcome** | Relevant | Not relevant | Relevant | Not relevant |

## State Changes

**[SPEC]** Per Table 5-4's outcomes: Rule 1 → "Yes. The Indication applies to the patient." Rule 2 → "No. The Indication does not apply to the patient." Rule 3 → "No. The Indication does not apply to the patient; however, the Indication Text Description should be made available to the clinician for manual determination." Rule 4 → "No. The indication does not apply to the patient."

Per Table 5-5's outcomes: Rule 1 → "Yes. The antigen series is a relevant patient series for the patient." Rule 2 → "No. The antigen series is not a relevant patient series for the patient." Rule 3 → "Yes. The antigen series is a relevant patient series for the patient." Rule 4 → "No. The antigen series is not relevant patient series for the patient." A relevant series is instantiated as a `PatientSeries` and added to the patient's tracked series (Figure 5-2).

**[IMPLEMENTATION]** `SelectRelevantPatientSeries` builds one `LT54` (Table 5-4) decision table per `Indication` on the current antigen series, and one `LT55` (Table 5-5) decision table per antigen series, which reads the aggregate result of all its `LT54` children (`isApplies()` across the inner set) as its third condition - exactly matching Table 5-5's condition 3. `LT55`'s outcomes 0 and 2 (the two "relevant" outcomes) both construct `new PatientSeries(antigenSeries)` and add it via `dataModel.getPatientSeriesStepper().add(...)`; outcomes 1 and 3 do nothing, matching the spec's "excluded from further processing."

## Next Steps

**[SPEC]** Not stated explicitly.

**[IMPLEMENTATION]** Unconditional return to **4.3** (the loop driver), regardless of how many antigen series were found relevant for the current antigen. See `transitions.yaml`.

## Plain-Language Walkthrough

For one antigen, this step asks two nested questions for every antigen series that could apply: first, for each series' *indications* (the conditions that would make a Risk series relevant - e.g. an underlying health condition), does the patient's observation history and age put them within that indication's applicable age window (Table 5-4)? Then, combining gender, series type (Standard/Evaluation-Only series don't need an indication at all), and whether any indication applied, is this whole antigen series relevant for the patient (Table 5-5)? Every series found relevant becomes a `PatientSeries` the rest of the engine will evaluate and forecast against.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.SelectRelevantPatientSeries` (LogicStepType `SELECT_RELEVANT_PATIENT_SERIES`) - `cdsi-engine`.
- Structured logs: default-level messages inside each `LogicOutcome.perform()` echoing the spec's own outcome text (e.g. "Yes. The Indication applies to the patient.").
- Tests: no dedicated unit test.

## Review Findings

- **`IMPLEMENTATION_MISMATCH` (verified in code, not yet classified/reviewed as a formal finding record):** Table 5-4's first condition, "Does the indication describe any active patient observations?", is implemented as:
  ```java
  setLogicCondition(0, new LogicCondition("Does the indication describe any active patient observations?") {
    @Override
    public LogicResult evaluateInternal() {
      // logic condition not yet implemented
      return LogicResult.NO;
    }
  });
  ```
  This unconditionally returns `NO`, regardless of the patient's actual active observations - the comment itself says "not yet implemented." Structurally this means Rules 3 and 4 of Table 5-4 (the "Unknown" and "-" branches, both of which require condition 1 to be Yes or Unknown) can never be selected via this path; only Rules 1/2 are reachable, and Rule 1 requires the age-window condition to be true regardless of what the code would have used the observation check for. The `caActivePatientObservations` attribute IS populated (`dataModel.getPatient().getMedicalHistory()`) but never read by this condition. This looks like a genuine gap between Table 5-4 as specified and as implemented - it needs a domain expert to confirm whether it materially changes any Risk-series relevance determination for indications that describe active patient observations (i.e. whether real Supporting Data indications rely on the "describes active patient observations" branch at all), before treating it as confirmed rather than draft.
