# 6.4 Evaluate Age

> **Review status:** draft. Reviewed against the extracted specification text and tables (pages 52-53), Figures 6-5/6-6, and the current `cdsi-engine` source (`EvaluateAge.java`). Not yet reviewed by anyone other than the agent that drafted it.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 52-53. Figure 6-5 (Evaluate Age Timeline), Figure 6-6 (Evaluate Age Process Model), Table 6-14 (Age Attributes), Table 6-15 (Was the Vaccine Dose Administered at a Valid Age?), Table 6-16 (Evaluate Age Business Rules). Business rules CALCDTAGE-1, CALCDTAGE-4, CALCDTAGE-5.

*(As in every step package: `[SPEC]`-tagged text is quoted or closely paraphrased from the source; `[IMPLEMENTATION]`-tagged text is an observation about `cdsi-engine`, never presented as normative specification content.)*

## Purpose

**[SPEC]** "Evaluate age validates the age at administration of a vaccine dose administered against a defined age range of a target dose. In cases where a target dose does not specify age attributes, the age at administration is considered 'valid.'"

## Entry Conditions

**[SPEC]** Not stated explicitly in this section's text. Per the Chapter 6 process model (Figure 6-1) and this step's position in the sequence (6.1 → 6.2 → 6.3 → **6.4** → 6.5 → ...), a vaccine dose administered has already passed the 6.1 (dose-administered condition), 6.2 (conditional skip), and 6.3 (inadvertent-vaccine) checks before age is evaluated.

## Inputs and Attributes

**[SPEC]** Table 6-14 Age Attributes:

| Attribute Type | Attribute Name | Assumed Value if Empty |
| --- | --- | --- |
| Patient | Date of Birth | - |
| Vaccine dose administered | Date Administered | - |
| Calculated date (CALCDTAGE-1) | Maximum Age Date | 12/31/2999 |
| Calculated date (CALCDTAGE-4) | Minimum Age Date | 01/01/1900 |
| Calculated date (CALCDTAGE-5) | Absolute Minimum Age Date | 01/01/1900 |

**[IMPLEMENTATION]** `EvaluateAge`'s constructor creates exactly these five `ConditionAttribute`s (`caDateOfBirth`, `caDateAdministered`, `caMinimumAgeDate`, `caMaximumAgeDate`, `caAbsoluteMinimumAgeDate`), and assigns the same three assumed values via `LogicStep.FUTURE` (12/31/2999) and `LogicStep.PAST` (01/01/1900) constants shared across all step classes. If the target dose's series dose has no `Age` definition at all, none of the three calculated dates get an initial value beyond their assumed default - matching the table's "Assumed Value if Empty" column.

## Business Rules

**[SPEC]** Table 6-16 Evaluate Age Business Rules:

| Business Rule ID | Business Rule |
| --- | --- |
| CALCDTAGE-1 | A patient's maximum age date must be calculated as the patient's date of birth plus the maximum age. |
| CALCDTAGE-4 | A patient's minimum age date must be calculated as the patient's date of birth plus the minimum age. |
| CALCDTAGE-5 | A patient's absolute minimum age date must be calculated as the patient's date of birth plus the absolute minimum age. |

**[IMPLEMENTATION]** Implemented identically in the constructor: `caAbsoluteMinimumAgeDate.setInitialValue(age.getAbsoluteMinimumAge().getDateFrom(dateOfBirth))`, and correspondingly for minimum and maximum age dates, using the antigen series dose's `Age` supporting-data record. There is no separate rule for "CALCDTAGE-2" or "CALCDTAGE-3" - Table 6-16 itself skips those numbers; this is a gap in the specification's own business-rule numbering, not an extraction defect (confirmed by checking the source PDF directly).

## Decision Tables

**[SPEC]** Table 6-15 Was the Vaccine Dose Administered at a Valid Age?

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 |
| --- | --- | --- | --- | --- |
| Is the date administered < absolute minimum age date? | Yes | No | No | No |
| Is the absolute minimum age date ≤ date administered < minimum age date? | No | Yes | No | No |
| Is the minimum age date ≤ date administered < maximum age date? | No | No | Yes | No |
| Is the date administered ≥ maximum age date? | No | No | No | Yes |
| **Outcome** | Not valid ("Too young") | Valid ("Grace period") | Valid | Extraneous ("Too old") |

(Full outcome wording is quoted in State Changes below, since it doubles as the evaluation-reason text.)

## State Changes

**[SPEC]** Per rule:

- **Rule 1:** "No. The vaccine dose administered was not administered at a valid age for the target dose. Evaluation reason is 'Too young'."
- **Rule 2:** "Yes. The vaccine dose administered was administered at a valid age for the target dose. Evaluation reason is 'Grace period'."
- **Rule 3:** "Yes. The vaccine dose administered was administered at a valid age for the target dose." (no evaluation reason given for the plain-valid case)
- **Rule 4:** "No. The vaccine dose administered was not administered at a valid age for the target dose. It is extraneous. Evaluation reason is 'Too old'."

**[IMPLEMENTATION]** `EvaluateAge`'s inner `LT` (`LogicTable`) class implements these four rules as four `LogicCondition`/`LogicOutcome` pairs, calling `dataModel.setEvaluationForCurrentTargetDose(status, reason)`:

| Rule | `EvaluationStatus` | `EvaluationReason` |
| --- | --- | --- |
| 1 | `NOT_VALID` | `TOO_YOUNG` |
| 2 | `VALID` | `GRACE_PERIOD` |
| 3 | `VALID` | `null` |
| 4 | `EXTRANEOUS` | `TOO_OLD` |

This matches the specification's four outcomes exactly, including Rule 3 correctly having no reason code (a plain valid dose needs no explanation).

## Next Steps

**[SPEC]** Not stated explicitly as a transition rule in this section (Chapter 6's tables describe *evaluation outcomes*, not control flow); Figure 6-1 (Evaluation Process Model) and the section ordering imply 6.4 is followed by 6.5 Evaluate Preferable Interval regardless of the age-evaluation outcome - age validity is a piece of evaluation state carried forward, not a fork in processing.

**[IMPLEMENTATION]** Confirmed: `EvaluateAge.process()` calls `setNextLogicStepType(LogicStepType.EVALUATE_PREFERABLE_INTERVAL)` **before** evaluating the decision table, i.e. unconditionally. See `transitions.yaml`.

## Plain-Language Walkthrough

Every target dose a series defines can specify an age window: too early and the dose doesn't "count" yet (`NOT_VALID`/too young), a bit early but within a tolerance and it's accepted with a note (`VALID`/grace period), right on time and it's simply valid, or too late and it's `EXTRANEOUS` (the dose happened, but not for this target dose - too old to satisfy it). Figure 6-5's timeline is the clearest way to see this: four zones on a line (Not Valid | Valid | Valid | Extraneous), split by three calculated dates (absolute minimum, minimum, maximum age), with "Date Administered" as the point being placed on that line.

If a target dose doesn't define age attributes at all, the assumed values (year 1900 minimum dates, year 2999 maximum date) make every real-world administered date land inside "Valid" by construction - which is exactly the specification's stated fallback ("age at administration is considered 'valid'").

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateAge` (LogicStepType `EVALUATE_AGE`) - `cdsi-engine`.
- Structured log events: `EvaluateAge` logs at `REASONING`/`TRACE` level for each of the four condition checks (e.g. "Checking: Is date administered < absolute minimum age date?" followed by the actual dates compared) and at `CONTROL`/`STATE` level for the selected outcome (e.g. "DOSE REJECTED: Vaccine dose was administered before the absolute minimum age" / "Setting evaluation status to \"not valid\""). This is exactly the kind of per-decision structured trace the reference module's later phases (engine tracing, diagnostic bundles) are meant to build on.
- Tests: no dedicated unit test (see `step.yaml`'s `tests` entry).

## Review Findings

- **Business-rule numbering gap (not a defect):** Table 6-16 defines CALCDTAGE-1, -4, and -5 only - there is no CALCDTAGE-2 or -3 anywhere in the source document (confirmed directly, not inferred). Noted so a future reviewer doesn't mistake this for a missed extraction.
- **Figure 6-5 references "Decision Table 6-15 Columns 1 & 2 / 3 & 4 / Column 5 / Column 6"**, but Table 6-15 as extracted (and as read directly from the rendered PDF page) has exactly 4 rule columns, not 6. This is a **specification-internal inconsistency** between the figure's column labels and the actual table structure - recorded here as an open question rather than resolved by guessing which one is "right." (Category: would be `SPECIFICATION_AMBIGUITY` if formalized in `findings/`.)
