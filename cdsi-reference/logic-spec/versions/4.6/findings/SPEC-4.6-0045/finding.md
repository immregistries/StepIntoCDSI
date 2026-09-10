# SPEC-4.6-0045: Chapter 8 scoring state lacks domain helpers and an explicit reset boundary

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`PatientSeries.scorePatientSeries` is mutable and is changed only through increment/decrement methods. Before this change there was no reset method. The same valid-dose, product-series, and maximum-age calculations were duplicated across Chapter 8 scoring classes, including copies that did not consume `TimePeriod.getChild()` or the last target dose.

## Interpretation

The foundation adds domain-level query methods, a last-target-dose maximum-age calculation using `TimePeriod.getDateFrom(Date)`, an explicit score reset, and a shared N-way extremum scoring utility. The utility is introduced but not used by scoring rows in this behavior-neutral commit. `PreFilterPatientSeries` resets a series exactly when it enters the scorable list, establishing the scoring-pass boundary without changing the current rule results.

## Affected

- Spec sections: 8.1-8.7
- Code locations: `PatientSeries`, `PatientSeriesScoring`, `PreFilterPatientSeries`
- FITS cases: none changed
