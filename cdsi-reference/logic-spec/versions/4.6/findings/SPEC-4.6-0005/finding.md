# SPEC-4.6-0005: Date reference-equality bug breaks tie-detection in 8.5 and 8.6

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`InProcessPatientSeries.evaluate_ACandidatePatientSeriesCanFinishEarliest()` compares forecast dates with:

```java
tmpDate == patientSeries.getForecast().getLatestDate()
patientSeries.getForecast().getLatestDate() != tmpDate
```

This is Java reference equality (`==`/`!=`) on `java.util.Date` objects, not `.equals()`. `Forecast`'s date fields are ordinary `java.util.Date`, never a cached, interned, or shared instance - confirmed by inspection. Two different `PatientSeries` objects whose forecasts compute to the identical calendar date will almost always be *different* `Date` instances in memory, so `==` evaluates `false` even when the dates genuinely match.

The identical pattern appears in `NoValidDoses.evaluate_AScorablePatientSeriesCanStartEarliest()`.

## Interpretation

The tie-detection both methods are supposed to perform (counting how many candidate series share the earliest/latest date) essentially never counts a real tie as a tie in practice, in both locations. This is a distinct bug from [SPEC-4.6-0006](../SPEC-4.6-0006/finding.md) (8.4's tie-*handling* gap, once a tie is detected) - here the tie is never detected in the first place.

`InProcessPatientSeries`'s other four scoring conditions are correctly implemented, including proper tie handling for "has the most valid doses" via a `greatestElementPosList` pattern that collects every tied series - this confirms the class knows how to handle ties correctly elsewhere, making this `==`/`.equals()` slip look like an isolated copy-paste error rather than a systemic misunderstanding. Tracked, not fixed, per standing project direction.

## Affected

- Spec sections: 8.5 (page 90), 8.6 (page 91)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.InProcessPatientSeries`, `org.openimmunizationsoftware.cdsi.core.logic.NoValidDoses`
- FITS cases: none identified in this pass
