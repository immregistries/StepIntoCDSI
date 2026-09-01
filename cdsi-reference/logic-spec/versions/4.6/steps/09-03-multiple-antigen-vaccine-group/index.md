# 9.3 Multiple Antigen Vaccine Group

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 95-96. No figure of its own. Table 9-4 (What is the Vaccine Group Status of a Vaccine Group Forecast for a Multiple Antigen Vaccine Group? - a 6-condition/6-outcome decision table), Table 9-5 (Multiple Antigen Vaccine Group Business Rules - see the correction below). Business rules MULTIANTVG-1, FORECASTPRIORITY-1.

**[Extraction note, not a specification issue - fixed during this same documentation pass]**

This package originally had to work around a section-splitting overflow: as the LAST in-scope section, 9.3's extracted text had no "next section" to bound it and ran 3711 lines into Appendices A/B/C, which in turn inflated the master inventory's auto-detected business-rule count for 9.3 to 100+ IDs (nearly all cross-references to rules already documented in other chapters - e.g. CALCDTLOTEXP-1 in 6.1, CALCDTCONFLICT-1 in 6.7, SELECTBEST-2/3 in 8.7, FORECASTDTCAN-1 in 7.4 - plus the same "COVID-19" antigen-name false-positive seen in section 4.4). Both symptoms shared one root cause: the same "last in-scope item has no upper bound" issue already fixed once for figures/tables (`extract.py`'s `_in_scope` filter). Rather than leave it as a documented limitation, the same session fixed it properly: `toc.find_first_appendix_page()` locates the real "APPENDIX A:" body heading (searched from page 10 onward, so it doesn't match the Table of Contents' own dot-leader mention of the same text), and `extract.py` now passes that as `split_sections.extract_all_sections`'s `overall_end_page`. Re-running `logic-spec extract` now produces a 100-line `09-03-multiple-antigen-vaccine-group.txt` bounded correctly at Table 9-5, and the master inventory now lists exactly the two genuine rules (`MULTIANTVG-1, FORECASTPRIORITY-1`) for 9.3 - matching what this package already documents below from manual verification.

## Purpose

**[SPEC]** "The forecasting decisions and rules which need to be applied to a multiple antigen vaccine group are listed below." A multiple antigen vaccine group (MMR, DTaP/Tdap/Td) classifies more than one antigen, so its vaccine group forecast must reconcile several antigens' patient series forecasts into one.

## Entry Conditions

**[SPEC]** Reached from 9.1 when VACCINEGROUP-2 determines the vaccine group classifies more than one antigen.

## Business Rules

**[SPEC]** Table 9-5 Multiple Antigen Vaccine Group Business Rules (the section's actual two rules, not the inflated auto-detected list - see the Extraction note above):

| Business Rule ID | Business Rule |
| --- | --- |
| MULTIANTVG-1 | The earliest date of a vaccine group forecast for a multiple antigen vaccine group must be one of the following: the later of (the earliest date of all contained patient series forecasts; the latest date administered of any vaccine dose administered belonging to the vaccine group) if any contained forecast is a priority patient series forecast; otherwise, the latest earliest date of all contained patient series forecasts. |
| FORECASTPRIORITY-1 | A patient series forecast is a priority patient series forecast if its target dose has at least one preferable interval, and every preferable interval for that target dose has an interval priority flag of 'Y'. |

**[IMPLEMENTATION]** The earliest-date computation (lines ~286-317 of `MultipleAntigenVaccineGroup.java`) branches on whether a contributing forecast's interval has an `IntervalPriority` set: when present, it takes the **earlier** of the running earliest date and the candidate; when absent, it takes the **later**. This matches the spirit of MULTIANTVG-1's priority-vs-non-priority split, but this pass did not fully trace whether the "latest date administered of any vaccine dose administered belonging to the vaccine group" clause (part of the priority branch) is incorporated - flagged as unconfirmed rather than asserted as fully conformant.

## Decision Tables

**[SPEC]** Table 9-4 What is the Vaccine Group Status of a Vaccine Group Forecast for a Multiple Antigen Vaccine Group? A six-condition cascade, each condition checked only if all prior ones are 'No':

| Condition | R1 | R2 | R3 | R4 | R5 | R6 |
| --- | --- | --- | --- | --- | --- | --- |
| Any contained forecast has status 'Contraindicated'? | Yes | No | No | No | No | No |
| Any contained forecast has status 'Aged Out'? | - | Yes | No | No | No | No |
| Any contained forecast has status 'Not Recommended'? | - | - | Yes | No | No | No |
| Any contained forecast has status 'Not Complete'? | - | - | - | Yes | No | No |
| All contained forecasts have status 'Immune'? | - | - | - | - | Yes | No |
| All contained forecasts have status 'Complete' or 'Immune'? | - | - | - | - | - | Yes |
| **Outcome** | Contraindicated | Aged Out | Not Recommended | Not Complete | Immune | Complete |

**[IMPLEMENTATION]** `MultipleAntigenVaccineGroup`'s inner `LT` class implements exactly this cascade as six `LogicCondition`/`LogicOutcome` pairs (verified: six `setLogicCondition` calls, six `setLogicResults` rows with the same `ANY`-cascading pattern as the table, six `setLogicOutcome` calls setting `vaccineGroupStatus`/`patientSeriesStatus` to `CONTRAINDICATED`/`AGED_OUT`/`NOT_RECOMMENDED`/`NOT_COMPLETE`/`IMMUNE`/`COMPLETE` respectively) - this table extracts and implements cleanly, with no discrepancy found between spec and code.

## State Changes

**[IMPLEMENTATION]** Builds one `VaccineGroupForecast`, selecting the constituent `PatientSeries` whose tracked antigen series' target disease is one of the vaccine group's antigens (`selectedList`). Sets `vaccineGroupStatus`/`patientSeriesStatus` per Table 9-4's cascade, `earliestDate` per MULTIANTVG-1 (see above), and (per the class's other setter calls, not individually re-verified rule-by-rule in this pass) `adjustedRecommendedDate`, `adjustedPastDueDate`, `latestDate`, `unadjustedRecommendedDate`, `unadjustedPastDueDate`, and `forecastReason` - this is where the behavior described by 9.1's FORECASTVG-2 through FORECASTVG-9 actually runs (see 9.1's Review Findings).

## Next Steps

**[IMPLEMENTATION]** Unconditional return to section "9" (the chapter's vaccine-group loop driver) - every one of the six outcomes, plus the class's own entry point, sets next step to `IDENTIFY_AND_EVALUATE_VACCINE_GROUP`. See `transitions.yaml`.

## Plain-Language Walkthrough

For a group like MMR, three antigens' worth of patient-series forecasts have to become one MMR-group forecast. Table 9-4 answers "what's the group's overall status" with a strict priority order - if even one constituent is Contraindicated, the whole group is; only if none are Contraindicated does Aged-Out matter, and so on down to the "everyone's Complete or Immune" case. Separately, MULTIANTVG-1 answers "when's the earliest date" - the tricky part being that some target doses are flagged as higher-priority (e.g. because of an interval that should be accelerated), which flips whether the calculation looks for the earliest or latest candidate date among the contributing series.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.MultipleAntigenVaccineGroup` (LogicStepType `MULTIPLE_ANTIGEN_VACCINE_GROUP`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- **Extraction limitation, fixed in this same pass** - see the Extraction note under Source: this section's raw extracted text originally overflowed into Appendices A/B/C (the last-in-scope-section upper-bound issue), which inflated the master inventory's auto-detected business-rule list to 100+ IDs. `extract.py`/`toc.py` were fixed to bound the last in-scope section at the real "Appendix A:" heading; re-extraction confirms both symptoms are gone.
- **MULTIANTVG-1 not fully re-derived** (unconfirmed, not resolved by guessing): see Business Rules, above - the priority/non-priority branch structure matches the spec, but the "latest date administered" clause of the priority branch wasn't traced to a specific line of code in this pass.
