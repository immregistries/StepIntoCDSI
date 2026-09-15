# SPEC-4.6-0052: Last forecast target SKIPPED never reaches Determine Forecast Need

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

After assessment-relative seasonal projection (SPEC-4.6-0051), 31 FLU FITS cases still failed with expected `COMPLETE`, actual `NOT_COMPLETE`. Seven clinical uids (duplicated across plans):

- `2013-0171`, `2013-0184`, `2018-0025`, `2018-0026`, `2019-0004`, `2019-0016`, `2025-0020`

Each fixture has one or more current-season Influenza doses and expects `serieStatus: COMPLETE` (done for the season).

Influenza Supporting Data encodes “one dose per season” as Dose 2 `recurringDose: Yes` plus conditional-skip sets that fire when the patient already has a qualifying current-season dose. Typical path:

1. Dose 1 SKIPPED (age ≥ 9) or SATISFIED; Dose 2 SATISFIED by the season dose.
2. Recurring clone created as `NOT_SATISFIED`; no AARs left → forecast on the clone.
3. 7.1 correctly SKIPPED the clone (already vaccinated this season) and returned to 4.4.
4. 4.4’s FORECAST neighborhood saw `SKIPPED`, found no next target dose, and called `setNextPatientSeries()` **without** running Table 7-10.
5. `PatientSeriesStatus` stayed `null` → vaccine-group status fell through to `NOT_COMPLETE`.

Table 7-10 Rule 2 would have assigned `COMPLETE` (no `NOT_SATISFIED` remaining, ≥1 `SATISFIED`). Related null-status stranding also appears in SPEC-4.6-0033, but that case was an `UNNECESSARY` placeholder overwritten back to `SKIPPED`; here the last target is a real forecast skip.

## Fix

In `EvaluateAndForecastAllPatientSeries` FORECAST neighborhood: when the last target is `SKIPPED` and `PatientSeriesStatus` is still null, route to `DETERMINE_EVIDENCE_OF_IMMUNITY` so 7.2–7.4 can run. Once 7.4 has set a status, a second return with the same skipped last dose advances the outer series loop (avoids re-entering 7.4 forever).

## Affected

- Spec sections: 4.4, 7.1, 7.4 (Table 7-10)
- Code: `EvaluateAndForecastAllPatientSeries`
- FITS: FLU COMPLETE cluster listed in `finding.yaml`
