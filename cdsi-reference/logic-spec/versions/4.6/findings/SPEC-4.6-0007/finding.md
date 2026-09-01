# SPEC-4.6-0007: NoValidDoses's "is completable" score never decrements, and two undocumented conditions run

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`NoValidDoses.evaluate_ACandidatePatientSeriesIsCompletable()`'s `if`/`else` both call `patientSeries.incPatientScoreSeries()` - there is no code path that ever decrements for this condition, contradicting Table 8-11's documented +1/-1 split. This looks like a copy-paste slip: the `else` branch should very likely call `descPatientScoreSeries()`, matching the pattern every other condition in this scoring family uses.

Separately: two scoring conditions run in this class that aren't present in Table 8-11 at all - a gender-match bonus, and an exceeded-maximum-age penalty.

(A related, separate bug in this same class - a `==`/`!=` Date reference-equality error breaking tie-detection in `evaluate_AScorablePatientSeriesCanStartEarliest()` - is tracked as [SPEC-4.6-0005](../SPEC-4.6-0005/finding.md), alongside the identical pattern in `InProcessPatientSeries`.)

## Interpretation

As written, "is completable" always contributes +1 regardless of the actual finish-date/max-age comparison it's supposed to be scoring.

The two extra scoring conditions are recorded as an open question, not resolved by guessing: this could be a deliberate, undocumented refinement to the spec-literal Table 8-11 logic, or a copy-forward from a different section's scoring logic that doesn't belong here. If real Supporting Data commonly has gender-restricted or near-max-age series competing under this scoring path, this materially changes the outcome versus a spec-literal implementation - worth checking against real FITS/Supporting Data cases before assuming either explanation. Tracked, not fixed, per standing project direction.

## Affected

- Spec sections: 8.6 (page 91)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.NoValidDoses`
- FITS cases: none identified in this pass
