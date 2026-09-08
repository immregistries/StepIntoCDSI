# SPEC-4.6-0019: Table 6-9's Equal row only matches "equal", never the Supporting Data's actual "equal to" - fix confirmed correct, but exposes a separate NPE

**Status:** open (not merged - see "Why this is not merged")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Table 6-9's row label in the spec text is "Equal" (bare). LT69's code:

```java
if (caConditionalSkipElements.getFinalValue().getDoseCountLogic().equalsIgnoreCase("equal")) {
```

Grepping the bundled Supporting Data (`cdsi-engine/src/main/resources/*.xml`) for `<doseCountLogic>`: 303 occurrences total - 12 "Greater Than", 69 "equal to", 222 "greater than". Zero bare "equal", zero "less than", anywhere in the release. The Equal row is unreachable on any real forecast as currently coded.

Two existing tests pin both spellings separately:

- `tableSixNineEqualRowAsTheSupportingDataSpellsIt` - `vaccineCount("equal to", 2, 2)` expected true - **red**.
- `tableSixNineEqualRowAsTheImplementationSpellsIt` - `vaccineCount("equal", 2, 2)` expected true - **green**, an existing characterization test that must not regress.

Because the second is green and must stay green, any real fix has to be additive (support both spellings), not a replacement.

## Interpretation

Fix attempted:

```java
.equalsIgnoreCase("equal") || .equalsIgnoreCase("equal to")
```

This closes the red test in isolation. But run alone against the full FITS suite (all other candidate changes from this round reverted), it causes **324 execution errors** (up from the baseline's 1), masking whatever its true effect on pass/fail counts would otherwise be.

### Root cause of the 324 errors

Inspected one failure bundle, `5eeca0522cc4517a96b0f417-HPV-2013-0395`:

```
NullPointerException: Cannot invoke "org.openimmunizationsoftware.cdsi.core.domain.Forecast.getLatestDate()"
because the return value of "org.openimmunizationsoftware.cdsi.core.domain.PatientSeries.getForecast()" is null
```

This is not in `EvaluateConditionalSkip` - it is downstream, in forecast-assembly code that calls `PatientSeries.getForecast().getLatestDate()` without a null check (call sites of this exact shape exist in `InProcessPatientSeries`, among others). Making the "equal to" branch reachable causes some target doses to be skipped that previously were not, which leaves at least one `PatientSeries` without a `Forecast` having been assembled for it by the time this downstream code runs - a pre-existing gap that the bare-"equal"-only bug had, until now, coincidentally kept unreachable.

## Why this is not merged

The dose-count fix itself is correct - identical pattern to SPEC-4.6-0017's Table 6-8 fix and SPEC-4.6-0018's Table 6-6 fix. But merging it requires first fixing a separate, undiagnosed NPE in the forecast-assembly path, which is out of scope for unit 6.2's bounded round: 6.2 does not own `PatientSeries`'s forecast lifecycle, and fixing the NPE without understanding why the forecast is missing risks masking a real defect with a null check instead of a real fix.

**Recommended path:** a dedicated round on `PatientSeries.getForecast()`'s null case (likely a Chapter 8 / patient-series forecast-assembly unit) should investigate why a `PatientSeries` can reach this call site with no `Forecast` assembled, before this dose-count fix is re-attempted.

**Materiality if eventually fixed cleanly:** the largest of unit 6.2's remaining reds - all 69 equality dose-count conditions in the bundled release are spelled "equal to", so Table 6-9's Equal row is completely dead code on any real forecast until this is resolved.

## Affected

- Spec sections: 6.2 (page 56)
- Code locations: `EvaluateConditionalSkip` (the dose-count fix); `PatientSeries` / `InProcessPatientSeries` (the blocking NPE, not yet investigated)
- FITS cases: 324 cases error out if merged today without also fixing the NPE
