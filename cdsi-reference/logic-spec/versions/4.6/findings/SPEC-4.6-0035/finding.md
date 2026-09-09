# SPEC-4.6-0035: CALCDTINT-9 blocked twice over - no loader support, no ObservationCode equality, and the rule body never assigned a date

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

CALCDTINT-9 (Table 6-19's fourth reference-date business rule - "measures from the most recent matching patient observation") is the last of unit 6.5's two remaining reds, and the last of the four CALCDTINT rules to be implemented (CALCDTINT-1, 2 and 8 were fixed in earlier rounds - see [SPEC-4.6-0012](../SPEC-4.6-0012/finding.md)/[SPEC-4.6-0013](../SPEC-4.6-0013/finding.md)). It was blocked in three independent places at once:

**1. Never loaded.** `DataModelLoader.readSeriesDose`'s interval-parsing branch handled `fromPrevious`, `fromTargetDose`, `fromMostRecent`, `absMinInt`, `minInt`, `earliestRecInt`, `latestRecInt` and `intervalPriority` - and silently dropped everything else, including `<fromRelevantObs>`. `Interval.getFromRelevantObservation()` was always null on any interval loaded from real Supporting Data, even though `Interval.setFromRelevantObservation(...)` already existed as dead code with no caller.

**2. No equality.** `ObservationCode` had no `equals()`/`hashCode()` at all. Even with data loaded, matching a Supporting-Data-declared observation code against the patient's own recorded observation history compares two separately-constructed instances that can never be reference-equal, so the match could never succeed.

**3. Never assigned.** `Interval.getPatientReferenceDoseDate`'s CALCDTINT-9 branch only logged `"REASONING: Using CALCDTINT-9"` and never assigned `tmpPatientReferenceDoseDate` to anything - so even a successful match would have had no effect.

Materiality, from status.yaml's own prior grep of the bundled Supporting Data: of 490 populated `<interval>` elements, 6 use `<fromRelevantObs>` - one each in Hib, Measles, Mumps, Pertussis, RSV and Rubella, all on Dose 1, against observation codes 171 ("Date of hematopoietic stem cell transplant"), 120 ("Begin Date of antiviral therapy [ART]") and 170 ("Onset of pregnancy" - the same example section 6.5's own spec text uses).

## Fix

- `DataModelLoader` now parses `<fromRelevantObs>` (its `<code>`/`<text>` children) into a new `ObservationCode`, calling the existing `Interval.setFromRelevantObservation(...)` setter.
- `ObservationCode` now has content-based `equals()`/`hashCode()` keyed on its `code` field only - the same CVX-only equality convention `VaccineType` already uses - since `text` is a human-readable label, not part of the code's identity.
- `Interval.getPatientReferenceDoseDate`'s CALCDTINT-9 branch now scans the patient's `MedicalHistory.getPatientObservationList()` for the most recent observation whose `ObservationCode` matches (by the new `equals()`), and assigns its date as the reference date. Unlike CALCDTINT-8, no "strictly before this dose" lower bound is needed - observations aren't administered doses and can't self-reference the interval being evaluated.

## Verification

- Unit 6.5's own suite (`EvaluatePreferableIntervalTest`): 30/30 green (up from 28/30).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 2 tests flipped (118 -> 116 failures) and nothing else.
- Full FITS run: 3627 passed both before and after, case-by-case diffed to 0 changed cases - exactly as status.yaml's own prior note predicted, since the bundled FITS fixture format has no observation field at all and cannot exercise this rule under any circumstances. A genuine correctness fix the current fixture set doesn't happen to exercise, the same category as SPEC-4.6-0026's CALCDT-5 fix, SPEC-4.6-0030's 6.1 NPE guard, and SPEC-4.6-0034's 6.3 fix.

## Known remaining limitation

status.yaml's own prior analysis (unit 6.5, "deliberately not covered" item 4) already flagged a separate, pre-existing gap: `getPatientReferenceDoseDate` returns `null` outright whenever `dataModel.getPreviousTargetDose()` is `null`, which is always true on Dose 1 - and all 6 of the real `<fromRelevantObs>` intervals sit on Dose 1. So this fix, while now provably correct, still cannot take effect against any of the 6 real Supporting Data occurrences until that separate gap is addressed. Left untouched here since it affects every CALCDTINT rule alike on Dose 1, not something specific to CALCDTINT-9, and is outside this round's bounds.

## Affected

- Spec sections: 6.5 (page 50, Table 6-19, CALCDTINT-9)
- Code locations: `DataModelLoader.java` (`readSeriesDose`), `ObservationCode.java`, `Interval.java` (`getPatientReferenceDoseDate`)
- FITS cases: none currently exercise `<fromRelevantObs>` intervals - the fixture format has no observation field
