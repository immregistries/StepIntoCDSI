# 7.3 Determine Contraindications

> **Review status:** draft. This section surfaces the most significant implementation gap found in this documentation pass so far - see Review Findings before relying on this step for anything.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 74-77. Figures 7-3 (Contraindication Process Model), 7-4 (Antigen Contraindication Process Model), 7-5 (Vaccine Contraindication Process Model). Table 7-4 (Determine Contraindication Attributes), Table 7-5 (Does the Antigen Contraindication Apply to the Patient? - decision table), Table 7-6 (Does the Vaccine Contraindication Apply to the Patient? - decision table), Table 7-7 (Is the Relevant Patient Series a Contraindicated Patient Series? - decision table), Table 7-8 (Determine Contraindication Business Rules). Business rules CALCDTCI-1, CALCDTCI-2.

**[Extraction note, not a specification issue]** Table 7-8 exists in the document body (confirmed directly - it appears immediately after Table 7-7's outcomes, before section 7.4 begins) but is **missing from the specification's own LOFT front matter** (which jumps from Table 7-7 to Table 7-9). Same pattern as Table 6-11 (section 6.2) and Table 6-19 (section 6.5). Its content is transcribed below from the raw section text. Table 7-7's second condition row and Table 7-6's OCR-hyphenated outcome text ("contraindicati on", "vacci ne") also needed hand-correction from the raw extraction - noted so a reviewer isn't surprised the wording here doesn't match `extracted/tables/table-7-7.txt` verbatim.

## Purpose

**[SPEC]** "Determine contraindications assesses if any or all series for an antigen are contraindicated for the patient. Contraindications may be applied at either the antigen or vaccine level." **[SPEC]** "Given the complex nature of contraindications, it may not always be possible to conclusively determine if a contraindication applies to a patient. To minimize missed doses, in the case where a contraindication cannot be definitively determined to be relevant for a patient, the contraindication will not be applied, but a notification should be made to a clinician" - i.e. ambiguous cases default to "not contraindicated, but flag for review," the same conservative default 5.1's indication logic uses.

An **antigen** contraindication blocks all relevant patient series for that antigen from being forecast at all (patient series status becomes Contraindicated). A **vaccine** contraindication only removes one specific vaccine from consideration, not the whole series.

## Entry Conditions

**[SPEC]** Runs after 7.2 (immunity has been checked; a contraindication is evaluated regardless of immunity status).

## Inputs and Attributes

**[SPEC]** Table 7-4 Determine Contraindication Attributes: Patient Active Patient Observations, Patient Adverse Reactions, Supporting Data Contraindication elements, Processing Data Assessment Date (assumed current date), Contraindication Begin Age Date (CALCDTCI-1, assumed 01/01/1900), Contraindication End Age Date (CALCDTCI-2, assumed 12/31/2999).

**[IMPLEMENTATION]** The constructor creates all six matching `ConditionAttribute`s and correctly computes the two calculated dates via `CALCDTCI_1`/`CALCDTCI_2.evaluate(...)`. However, `caContraindicationElements`'s actual initial value is never set - the line that would do it is commented out, with a note that it "cannot be set correctly until 'Contraindication_TO_BE_REMOVED' get[s] replaced with 'Contraindication'" (a migration the codebase hasn't finished). See Review Findings.

## Business Rules

**[SPEC]** Table 7-8 Determine Contraindication Business Rules:

| Business Rule ID | Business Rule |
| --- | --- |
| CALCDTCI-1 | A patient's contraindication begin age date must be calculated as the patient's date of birth plus the contraindication begin age of a contraindication. |
| CALCDTCI-2 | A patient's contraindication end age date must be calculated as the patient's date of birth plus the contraindication end age of a contraindication. |

**[IMPLEMENTATION]** Both correctly computed in the constructor via `org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules.CALCDTCI_1`/`CALCDTCI_2` - these two date calculations are the only part of this section that actually runs.

## Decision Tables

**[SPEC]** Table 7-5 Does the Antigen Contraindication Apply to the Patient?

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 |
| --- | --- | --- | --- | --- | --- |
| Does the antigen contraindication describe any active patient observations? | Yes | No | No | Unknown | - |
| Does the antigen contraindication describe any adverse reactions? | No | Yes | No | Unknown | - |
| Is the contraindication begin age date ≤ assessment date < contraindication end age date? | Yes | Yes | Yes | Yes | No |
| **Outcome** | Applies | Applies | Does not apply | Does not apply (flag for clinician) | Does not apply |

**[SPEC]** Table 7-6 Does the Vaccine Contraindication Apply to the Patient?

| Condition | Rule 1 | Rule 2 | Rule 3 | Rule 4 | Rule 5 | Rule 6 | Rule 7 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Describes any active patient observations? | Yes | Yes | No | No | No | - | Unknown |
| Describes any adverse reactions? | No | No | Yes | Yes | No | - | Unknown |
| Begin age date ≤ assessment date < end age date? | Yes | Yes | Yes | Yes | - | No | Yes |
| Is the preferable vaccine's type one of the contraindicated vaccine types? | Yes | No | Yes | No | - | - | Yes |
| **Outcome** | Applies | Does not apply | Applies | Does not apply | Does not apply | Does not apply | Does not apply (flag for clinician) |

**[SPEC]** Table 7-7 Is the Relevant Patient Series a Contraindicated Patient Series?

| Condition | Rule 1 | Rule 2 | Rule 3 |
| --- | --- | --- | --- |
| Are there any antigen contraindications that apply to the patient? | Yes | No | No |
| Do all preferable vaccines for the series have at least one applying vaccine contraindication? | - | Yes | No |
| **Outcome** | Contraindicated | Contraindicated | Not contraindicated |

## State Changes

**[SPEC]** A contraindicated patient series should end up with patient series status Contraindicated (see 7.4's attribute table, which reads this section's outcome).

**[IMPLEMENTATION]** **None of this runs.** `logicTableList` is never populated in `DetermineContraindications` - no `LogicTable`/`LogicCondition`/`LogicOutcome` for Tables 7-5, 7-6, or 7-7 is instantiated anywhere in the class. The file contains a fully commented-out sketch of what an eventual `LT75` class might look like, and a top-of-file comment block: *"Adjust logic as follows, finish creating entire file... Write the logic for logic tables 7-5 to 7-7, correct logicOutcomes and everything. Afterwards make sure to point to 7.4 next - is this true?"* `process()` calls `evaluateLogicTables()` on an empty list (a no-op) and moves straight to 7.4.

## Next Steps

**[SPEC]** Not stated as a transition rule - Table 7-1 implies 7.3 precedes 7.4.

**[IMPLEMENTATION]** Unconditional to **7.4**, regardless of any contraindication (since none is ever evaluated). See `transitions.yaml`.

## Plain-Language Walkthrough

As specified, this step should look at a patient's active observations and adverse-reaction history against the Supporting Data's contraindication definitions, twice (once at the antigen level, once at the vaccine level), then combine both into a single "is this series contraindicated" answer feeding 7.4. As implemented, none of that happens - the step is currently a pass-through that only computes two date attributes nobody downstream reads (since no contraindication outcome is ever produced), then moves on. A patient with a genuine contraindication currently receives no different treatment than one without, as far as this step is concerned.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.DetermineContraindications` (LogicStepType `DETERMINE_CONTRAINDICATIONS`) - `cdsi-engine`. File header comment: "Created by Nicole on 10/25/24, or somewhere around that time" - a recent, evidently unfinished addition.
- Tests: no dedicated unit test.

## Review Findings

- **`IMPLEMENTATION_MISMATCH` (draft, verified in code) - the most significant gap found in this pass:** `DetermineContraindications` implements none of Tables 7-5, 7-6, or 7-7's decision logic. No `LogicTable` is ever added to `logicTableList`; the class's own in-code comments confirm this is known, unfinished work ("finish creating entire file", "Write the logic for logic tables 7-5 to 7-7"). Practical effect: **no patient series is ever marked Contraindicated by this step**, regardless of the patient's actual contraindication data. This connects directly to 7.4's own "is the relevant patient series a contraindicated patient series?" condition (`DetermineForecastNeed`'s condition 3), which checks `dataModel.getPatient().getMedicalHistory().getContraindicationSet()` - a set nothing in this step (or, as far as this pass traced, anywhere else) ever populates, so that condition is also effectively dead. This is a materially different situation from the smaller "one condition returns NO" gaps found in 6.2/6.5/6.8/7.2 - here, an entire section's worth of clinical safety logic is unimplemented, not just one path through it. Needs prompt engineering/domain-expert attention, not just a documentation note.
- `caContraindicationElements`'s initial value is never set (commented out, blocked on a `Contraindication_TO_BE_REMOVED` → `Contraindication` migration) - consistent with the above; there would be nothing to evaluate against even if the decision tables existed.
- Table 7-8 missing from the document's own LOFT front matter (see Source, above) - same pattern as 6.2's Table 6-11 and 6.5's Table 6-19.
