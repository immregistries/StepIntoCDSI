# SPEC-4.6-0025: SELECTB-24 excludes a null patient series status from candidacy, but the correct fix has a large, unaudited blast radius

**Status:** open (confirmed, not fixed - attempted and reverted this round)
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`PreFilterPatientSeries.java`'s candidate-admission filter:

```java
if (patientSeries.getPatientSeriesStatus() != null
        && !patientSeries.getPatientSeriesStatus().equals(PatientSeriesStatus.CONTRAINDICATED)) {
```

SELECTB-24's actual text: "A relevant patient series that is the basis of a patient series forecast must be considered a candidate scorable patient series if... the patient series forecast does not have a patient series status of 'Contraindicated.'" A `null` status trivially satisfies "does not have a status of Contraindicated" - it should be admitted, not excluded. The all-contraindicated fallback loop has the matching bug (`status != null && status.equals(CONTRAINDICATED)`).

This is already pinned by an existing unit test, `PreFilterPatientSeriesTest.selectbTwentyFourASeriesWithNoPatientSeriesStatusIsStillNotContraindicated`, written in an earlier Role A pass and red until this round.

Traced from FITS case `675c41f8e4b089d2bdc0c483-COVID-19-2023-0071` (a 9-month-old with two valid Moderna doses, expected `COMPLETE`). Direct instrumentation showed `dataModel.getPatientSeriesStepper().getList()` correctly contained both of COVID-19's group-1 series with `patientSeriesStatus = null` - not yet classified at this point in the pipeline - both individually age-appropriate and unexcluded by SPEC-4.6-0021's series-group gate. Yet `dataModel.getBestPatientSeriesList()` ended up empty for COVID-19 entirely: both candidates were silently discarded by 8.1 because of their null status, leaving nothing for any later Chapter 8 step to work with. This exact mechanism accounts for 74 of COVID-19's 275 FITS failures ("No forecasted vaccine group matched CVX 213").

## What was tried, and why it was reverted

Changed the guard to match SELECTB-24's literal text:

```java
if (patientSeries.getPatientSeriesStatus() != PatientSeriesStatus.CONTRAINDICATED) {
```

This fixed the target unit test cleanly - full `cdsi-engine` suite went from 137 to 136 failures, confirmed via a sorted diff to be exactly that one test and nothing else newly broken.

The full FITS run told a completely different story: `passedCases` fell from 3451 to 3426 and `executionErrors` rose from 1 to **270**. The dominant causes:

- 48 cases: `NullPointerException: ... PatientSeries.getPatientSeriesStatus() ... is null` - some later Chapter 8 step calls `.getPatientSeriesStatus().equals(...)` directly, unguarded.
- 213 cases: an unqualified `NullPointerException: null` - a different, not-yet-located null dereference, reached only once null-status series are allowed to survive this far into the pipeline.
- 8 cases: the same `getTargetDoseList()` null-guard gap SPEC-4.6-0022 fixed in a different call site, recurring here in a new context.

Reverted immediately given the net effect (270 execution errors, -25 net passing cases) is a severe regression, confirmed restored to the exact pre-attempt baseline (3451 passed, 1 execution error) via a fresh FITS run after the revert.

## Interpretation

This is a genuine SELECTB-24 conformance defect, not a misreading - the spec text is unambiguous and the existing test already proves it. But it cannot be fixed as a one-line change: `PreFilterPatientSeries` has been the de facto gatekeeper every later Chapter 8 step (8.2-8.9) implicitly relies on to never see a `PatientSeries` with a null status. That assumption is baked into call sites scattered across those later steps, none of which this round had the scope to find and harden one at a time.

Fixing this properly requires two things together, not one: (1) SELECTB-24's own one-line fix (admit a null status), and (2) auditing and null-guarding every downstream `.getPatientSeriesStatus()` call this admits new reachability to - likely several call sites across 8.2 through 8.9, given the volume and spread of the 270 execution errors this round's attempt surfaced (almost certainly spanning several antigens beyond COVID-19, not just the one traced here). That is a substantially larger, more careful piece of work than this round's scope, and is recorded here as open rather than shipped as a partial, regression-causing patch.

## Materiality

At minimum, COVID-19's 74 "no forecast at all" FITS failures trace to this exact mechanism. Given the breadth of the 270 execution errors surfaced by the attempted fix, the true number of FITS cases this affects - once every downstream null-status call site is also hardened - is likely substantially larger than 74, and probably touches multiple antigens beyond COVID-19.

## Affected

- Spec sections: 8.1 (page 87, Table 8-1, SELECTB-24)
- Code locations: `PreFilterPatientSeries.java` (the fix), plus an unknown number of downstream Chapter 8 call sites (8.2-8.9) that assume a non-null `PatientSeriesStatus` once a series reaches `scorablePatientSeriesList`/`prioritizedPatientSeriesList` - not yet identified individually
- FITS cases: at least 74 (COVID-19), likely more once fully scoped
