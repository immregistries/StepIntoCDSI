# 6.7 Evaluate Vaccine Conflict

> **Review status:** draft.

## Source

Logic Specification for ACIP Recommendations v4.6, pages 61-62. Figure 6-15 (Timeline), Figure 6-16 (Process Model). Table 6-23 (Attributes), Table 6-24 (Business Rules). Business rules CALCDTCONFLICT-1, CALCDTCONFLICT-2, CONFLICT-3. **No decision table exists for this section** - see Purpose.

## Purpose

**[SPEC]** "Evaluate vaccine conflict validates the date administered of a vaccine dose administered against previous administered vaccines to ensure proper spacing between administrations. This covers live virus vaccine conflicts as well as non-live virus vaccine conflicts." If no vaccine-conflict Supporting Data exists for the administered vaccine type, there is no conflict by definition.

**[SPEC, Implementer Note]** "This section of the logic specification has been considerably refined in version 4.4 to be less process driven and more business rule based. The outcome should be functionally equivalent to previous versions." The note also flags a terminology migration still pending as of v4.6: the spec's "Conflicting Vaccine Type" / "Impacted Vaccine Type" correspond to the Supporting Data's older "Previous Vaccine Type" / "Current Vaccine Type" XML terms - not yet renamed there to avoid breaking existing consumers.

## Entry Conditions

**[SPEC]** Runs after 6.6, regardless of interval outcome.

## Inputs and Attributes

**[SPEC]** Table 6-23: Date Administered, Vaccine Type (both from the dose administered), Live Virus Conflicts (Supporting Data), and two calculated dates - Conflict Begin/End Interval Date (CALCDTCONFLICT-1/2), no assumed-value defaults given.

**[IMPLEMENTATION]** `caDateAdministered`, `caCurrentVaccineType` match. The begin/end interval date attributes are declared only in code comments (`caConflictBeginIntervalDate`/`caConflictEndIntervalDate`, both commented out at the class level) and are actually created ad hoc, per candidate previous dose, inside the constructor's loop (`LT422`'s locals) - a slightly different structure than a flat attribute table but covering the same values.

## Business Rules

**[SPEC]** CALCDTCONFLICT-1 (conflict begin interval date = previous dose's date + the conflict's begin interval, when the current dose is an "impacted" type and the previous dose is a "conflicting" type for it); CALCDTCONFLICT-2 (conflict end interval date, similarly, with an extra branch for whether the previous dose's evaluation status was Valid or something else); CONFLICT-3 (a dose is "impacted" if its date falls within the begin/end interval window).

**[IMPLEMENTATION]** These three conditions are exactly what `LT420`/`LT421`/`LT422` check (see Decision Tables) - the business rules were not "removed" by the v4.4 refinement so much as restructured as inline conditions on internally-named tables rather than a separate Yes/No grid the spec now exposes to the reader.

## Decision Tables

**[SPEC]** None (see Purpose/Implementer Note).

**[IMPLEMENTATION]** The code still internally implements this as three nested `LogicTable`s, each checking one of the three business rules above in sequence (does a Live Virus Conflict entry apply to this vaccine type at all? → is there a previous administered dose recorded on or before this one? → for each such previous dose, is the pairing a defined conflict, and does the current date fall in its interval window?). These carry **stale internal labels "Table 4-20," "Table 4-21," "Table 4-22"** - a leftover from a pre-4.6 chapter-numbering revision (consistent with the code's own comment `// change chapter to be more Business rule based to match the 4.5 document`, itself referencing an even earlier version number than the document's current 4.6). Not a functional problem - the conditions checked line up with the current spec's business rules - but worth knowing so a reader isn't confused looking for "Table 4-20" in the current PDF.

## State Changes

**[IMPLEMENTATION]** If any previous dose is found in conflict (`y == YesNo.YES`), `dataModel.getTargetDose().setStatusCause(... + "VirusConflict")` - the same `statusCause`-accumulation pattern 6.5/6.6 use, read later by 6.10.

## Next Steps

See `transitions.yaml` - unconditional to 6.8.

## Plain-Language Walkthrough

This step asks: does giving this vaccine now conflict with a live-virus (or other) vaccine given recently? It works backward through every dose already administered, and for each one asks the same three-part question the spec's business rules define: is this vaccine pairing even a defined conflict, and does the previous dose's date put the current dose inside that conflict's interval window? The specification stopped presenting this as a Yes/No decision grid in v4.4 specifically to make the underlying business rules easier to read directly - the code hasn't been restructured to match that presentation change, but the logic itself lines up.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.EvaluateVaccineConflict` (LogicStepType `EVALUATE_VACCINE_CONFLICT`) - `cdsi-engine`.
- Tests: no dedicated unit test.

## Review Findings

- Internal decision-table labels ("Table 4-20/21/22") are stale relative to the current v4.6 document, which has no numbered decision table for this section at all post-4.4-refinement. Documentation-only; the underlying conditions match the current business rules. Worth a low-priority cleanup pass, not a correctness issue.
