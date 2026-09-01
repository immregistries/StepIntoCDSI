# Statuses

> **Review status:** draft.

## What this covers

The specification's three distinct status vocabularies - evaluation status, target dose status, and patient series status - each tracking a different scope of "how is this doing" and used throughout every step package in Chapters 5-9. Source: Logic Specification for ACIP Recommendations v4.6, section 3.2 "Statuses" (page 24), Tables 3-1, 3-2, 3-3.

## Explanation

**[SPEC]** "The Logic Specification uses different statuses to denote the state of evaluation, target dose, and patient series." These are three separate, non-interchangeable vocabularies at three separate scopes - see [target-dose.md](target-dose.md) for how they nest (one administered dose's evaluation feeds into one target dose's satisfaction, which feeds into one series' completion).

### Evaluation Status (Table 3-1) - one administered dose, evaluated against one target dose

| Status | Meaning |
| --- | --- |
| Valid | Administered according to ACIP recommendations |
| Not Valid | Not administered according to recommendations; must be repeated |
| Extraneous | Not administered according to recommendations, but does not need repeating (e.g. given after the maximum age, or an extra dose) |
| Sub-standard | Has a known dose condition (expired, sub-potent, recalled); needs repeating |

**[IMPLEMENTATION]** `org.openimmunizationsoftware.cdsi.core.domain.datatypes.EvaluationStatus`: `VALID`, `NOT_VALID`, `EXTRANEOUS`, `SUB_STANDARD` - a clean 1:1 match with Table 3-1, both in count and meaning (verified by reading the enum directly).

### Target Dose Status (Table 3-2) - one target dose slot within a series

| Status | Meaning |
| --- | --- |
| Not Satisfied | No administered dose has met the target dose's goals yet |
| Satisfied | An administered dose has met the target dose's goals |
| Skipped | No administered dose met the goals, but none is needed either (age/interval means the target dose doesn't apply) |

**[IMPLEMENTATION]** `org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus` has five values: `NOT_SATISFIED`, `SATISFIED`, `SKIPPED` (matching the three above), plus `SUBSTITUTED` and `UNNECESSARY`, which don't appear in Table 3-2 at all - see [target-dose.md](target-dose.md)'s Open Questions.

### Patient Series Status (Table 3-3) - one whole antigen series for the patient

| Status | Meaning |
| --- | --- |
| Not Complete | Not yet met all ACIP recommendations for the series |
| Complete | Has met all ACIP recommendations for the series |
| Contraindicated | No further vaccines should be given for this series right now |
| Immune | Evidence of immunity means no further vaccines are needed |
| Not Recommended | Immunization history already provides sufficient protection; no action recommended |
| Aged Out | Exceeded the maximum age before completing the series |

**[IMPLEMENTATION]** `org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus`: `COMPLETE`, `CONTRAINDICATED`, `IMMUNE`, `NOT_COMPLETE`, `NOT_RECOMMENDED`, `AGED_OUT` - a clean 1:1 match with Table 3-3 (verified directly). Note: `org.openimmunizationsoftware.cdsi.core.domain.VaccineGroupStatus` (used in Chapter 9, for a *vaccine group's* forecast rather than a single antigen series) is a **separate enum with the identical six values** - the two are not the same type in code even though they mean the same thing at two different aggregation levels (series vs. group). Don't assume they're interchangeable when reading code that uses one or the other.

### Decision-table results are a fourth, related but distinct vocabulary

Not a "status" in the spec's own sense, but easy to confuse with one: `LogicResult` (`YES`, `NO`, `ANY`, `UNKNOWN`, `EXTRANEOUS`, `ZERO`, `ONE`, `MORE_THAN_ONE`) is the generic answer type a single *condition* within a decision table evaluates to - see [decision-tables.md](decision-tables.md). `EXTRANEOUS` appears in both `EvaluationStatus` and `LogicResult` but means something different in each context (a dose's outcome, versus a condition's answer in an 8.x-style "how many doses" counting table).

## Where it applies

Every step package in Chapters 6-9 reads or sets one or more of these. The clearest single examples: [6.4 Evaluate Age](../steps/06-04-evaluate-age/index.md) sets `EvaluationStatus`; [6.10 Satisfy Target Dose](../steps/06-10-satisfy-target-dose/index.md) sets `TargetDoseStatus`; the 8.x scoring family and [9.3 Multiple Antigen Vaccine Group](../steps/09-03-multiple-antigen-vaccine-group/index.md) set `PatientSeriesStatus`/`VaccineGroupStatus`.

## Open questions

- See [target-dose.md](target-dose.md): `TargetDoseStatus.SUBSTITUTED`/`UNNECESSARY` have no corresponding entry in Table 3-2.
- `VaccineGroupStatus` and `PatientSeriesStatus` being separate types with identical values wasn't confirmed to be intentional design (vs. an opportunity to share one enum) - noted as an observation, not a defect, since nothing here suggests it causes incorrect behavior.
