# 7.4 Determine Forecast Need

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 78-80. Figure 7-6 (Determine Forecast Need Process Model). Table 7-9 (Determine Forecast Need Attributes), Table 7-10 (Should the Patient Receive Another Target Dose? - decision table), Table 7-11 (Determine Forecast Need Business Rules). Business rules CALCDTAGE-1, FORECASTDTCAN-1.

## Purpose

**[SPEC]** "Determine forecast need determines if there is a need to forecast dates. This involves reviewing patient data, antigen administered records, and patient series. This is a prerequisite before a CDS engine can produce forecast dates and reasons." In short: before computing *when* the next dose is due, first decide *whether* one is needed at all - the patient might already be done, immune, contraindicated, out of season, or too old.

## Entry Conditions

**[SPEC]** Runs after 7.3 (contraindication status, however it was determined, is an input here).

## Inputs and Attributes

**[SPEC]** Table 7-9: Immunization history (Vaccine Dose(s) Administered), Relevant Patient series (Target Dose Statuses), Supporting Data Seasonal Recommendation End Date (assumed 12/31/2999), **the outcome of 7.2** (Evidence of Immunity, assumed "no evidence"), **the outcome of 7.3** (Contraindicated Patient Series, assumed "not contraindicated"), Runtime Assessment Date, Maximum Age Date (CALCDTAGE-1), Candidate Earliest Date (FORECASTDTCAN-1).

**[IMPLEMENTATION]** This step explicitly depends on 7.2's and 7.3's outputs as inputs (`caEvidenceOfImmunity`, `caContraindicatedPatientSeries` attributes) - matching the table. Given 7.3 currently produces no real contraindication output (see 07-03's Review Findings), this section's own contraindication condition inherits that gap - see Review Findings below.

## Business Rules

**[SPEC]** Table 7-11: CALCDTAGE-1 (maximum age date, same rule as 6.4); FORECASTDTCAN-1 (candidate earliest date = the latest of: minimum age date, latest minimum interval date, latest conflict-end date, seasonal recommendation start date, latest inadvertent-administration date, most recent administered-dose date).

**[IMPLEMENTATION]** CALCDTAGE-1 computed via the shared `DateRules.CALCDTAGE_1`. FORECASTDTCAN-1 implemented as `computeEarliestDate()`, which combines the minimum age date and the latest minimum interval date (via `findMinimumIntervalDates()`) - the comment `// list.add(caLatestConflictEndIntervalDate.getFinalValue());// CALCDTLIVE-4 is both used and removed?` and a similarly commented-out seasonal-recommendation-start-date line indicate two of the rule's six candidate dates (conflict-end date, seasonal start date) are **not currently included** in this class's earliest-date calculation - flagged in Review Findings rather than assumed harmless.

## Decision Tables

**[SPEC]** Table 7-10 Should the Patient Receive Another Target Dose?

| Condition | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ≥1 target dose 'Not Satisfied'? | Yes | No | No | - | - | - | - | - |
| ≥1 target dose 'Satisfied'? | - | Yes | No | - | - | - | - | - |
| Evidence of immunity? | No | - | - | Yes | - | - | - | - |
| Series is contraindicated? | No | - | - | - | Yes | - | - | - |
| Assessment date ≤ seasonal recommendation end date? | Yes | - | - | - | - | No | - | - |
| Assessment date < maximum age date? | Yes | - | - | - | - | - | No | - |
| Candidate earliest date < maximum age date? | Yes | - | - | - | - | - | - | No |
| **Outcome** | Yes, need dose (Not Complete) | No (Complete) | No (Not Recommended - past history) | No (Immune) | No (Contraindicated) | No (Not Recommended - season ended) | No (Aged Out - assessment past max age) | No (Aged Out - can't finish before max age) |

## State Changes

**[IMPLEMENTATION]** `DetermineForecastNeed$LT` matches this 7-condition, 8-rule table exactly (`setLogicResults` rows use `YES`/`NO`/`ANY` matching the grid above). Each outcome sets `PatientSeriesStatus` (`NOT_COMPLETE`, `COMPLETE`, `NOT_RECOMMENDED` ×2, `IMMUNE`, `CONTRAINDICATED`, `AGED_OUT` ×2) and, for outcomes 1-7, a matching forecast reason string - all verified to match the specification's outcome text verbatim.

## Next Steps

**[SPEC]** Not stated as a transition rule.

**[IMPLEMENTATION]** Verified directly: Rule 1 (dose needed) proceeds to **7.5**; Rules 2 through 8 all explicitly loop back to **4.4** (the per-series evaluate/forecast driver) rather than continuing the forecast-dates sequence - a completed/immune/contraindicated/out-of-season/aged-out series has nothing further to forecast. See `transitions.yaml`.

## Plain-Language Walkthrough

This is the "should we even bother forecasting a date?" gate. Seven independent conditions, each capable of ending the process for this series on their own: dose statuses that already answer the question (all satisfied = complete, one still open = keep going), immunity, contraindication, a missed vaccination season, or simple old age (either the patient is already past the cutoff, or the earliest they could get the dose is past the cutoff, so there's no point forecasting a date that can never be valid). Only when none of those seven "stop" conditions apply does the engine proceed to actually compute a date, in 7.5.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.DetermineForecastNeed` (LogicStepType `DETERMINE_FORECAST_NEED`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Condition 3 ("is the relevant patient series a contraindicated patient series?") reads `dataModel.getPatient().getMedicalHistory().getContraindicationSet()`**, which is empty unless something upstream populates it - and 7.3 (`DetermineContraindications`), the step whose whole job is to determine this, currently implements no contraindication logic at all (see [07-03's Review Findings](../07-03-determine-contraindications/index.md)). Practical effect: Rule 5 (the "Contraindicated" outcome) is very likely unreachable in the current engine, the same way 07-03 already documents. Recorded here rather than re-diagnosed, to avoid two packages disagreeing about the same underlying gap.
- **FORECASTDTCAN-1's implementation (`computeEarliestDate()`) appears to omit two of the six candidate dates the rule specifies** - the latest conflict-end-interval date and the seasonal recommendation start date - based on commented-out lines in the source (one with a self-doubting comment, `// CALCDTLIVE-4 is both used and removed?`). Not confirmed as a defect (the commented code might be genuinely superseded elsewhere), but flagged as unverified rather than assumed complete - would need someone to trace whether those two dates are folded in via a different code path (e.g. within 6.7's conflict-interval computation) before this can be called a real gap.
