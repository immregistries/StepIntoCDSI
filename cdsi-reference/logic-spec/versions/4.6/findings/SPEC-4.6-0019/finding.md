# SPEC-4.6-0019: Table 6-9's Equal row only matches "equal", never the Supporting Data's actual "equal to"

**Status:** open (not merged - see "2026-09-09 re-investigation" and "Why this is still not merged")
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

## Why this was not merged (as of the original investigation)

The dose-count fix itself is correct - identical pattern to SPEC-4.6-0017's Table 6-8 fix and SPEC-4.6-0018's Table 6-6 fix. But merging it required first fixing a separate, undiagnosed NPE in the forecast-assembly path, which is out of scope for unit 6.2's bounded round: 6.2 does not own `PatientSeries`'s forecast lifecycle, and fixing the NPE without understanding why the forecast is missing risks masking a real defect with a null check instead of a real fix.

**Materiality if eventually fixed cleanly:** the largest of unit 6.2's remaining reds - all 69 equality dose-count conditions in the bundled release are spelled "equal to", so Table 6-9's Equal row is completely dead code on any real forecast until this is resolved.

## 2026-09-09 re-investigation (Role B round 23)

Re-investigated at the project owner's explicit request for a dedicated investigation and a proposed solution, rather than a re-attempt on the strength of the original numbers alone.

**The original blocker is gone.** Re-measured fresh against the current codebase: the same additive fix now produces **0 execution errors** (down from 324) and a net **+20** (49 improvements, 29 regressions). The `PatientSeries.getForecast()` NPE this finding originally traced appears to have been resolved incidentally by one or more intervening fixes from this same session - most likely unit 4.4's SPEC-4.6-0028 (which touched exactly the "every patient series gets a forecast assembled" guarantee).

**But the 29 regressions turned out to be two different problems, traced by direct instrumentation and fixture inspection, not assumed:**

**(A) DTaP, 25 cases - a spec-vs-fixture correctness question.** Traced `5eeca0522cc4517a96b0f417-DTAP-2013-0007` (title: "Forecast #3 (Td) in 4 weeks"; patient exactly 7 years old at assessment, 2 prior valid doses) end to end. The fixture expects the engine to forecast series dose 3, whose own Supporting Data `<interval>` element (`minInt: 4 weeks, fromPrevious: Y`) matches the fixture's expected date exactly. Once the Equal row becomes reachable:
- Series doses 2 through 7 each independently carry an "Age >= 7 years" skip condition, already present in the Supporting Data and entirely unrelated to this fix, that a patient exactly 7 years old now satisfies.
- Dose 7 additionally carries its own "2 or more valid doses" Equal-row condition (now reachable for the first time), which this patient's 2 valid doses also satisfy.
- The walk advances past all of them to dose 8 (satisfied by the patient's own Tdap dose) and lands on dose 9 - whose own `<interval>` element (`minInt: 6 months, fromPrevious: Y`) computes 2027-03-01, not the fixture's expected 2026-09-29.

Both the fixture's expectation and the engine's new answer are individually well-supported by different pieces of the Supporting Data - the fixture by dose 3's 4-week interval, the engine by dose 9's own 6-month interval once doses 2-8 are (each for their own, independently-justified reason) no longer in play. Adjudicating which is the clinically-correct answer requires ACIP domain expertise this investigation doesn't have. This is the same category of open question as HepA's already-documented "Extra Test Cases" spec-vs-fixture conflict (SPEC-4.6-0026) - not something code-reading alone can resolve.

**(B) PCV, 4 cases - a genuine, unexplained defect.** Traced `675c41f8e4b089d2bdc0c483-PCV-2023-0002` (an adult patient who received PCV13, then PPSV23, then PCV20 five years later - by the title's own description, a textbook-complete adult pneumococcal sequence): expected `COMPLETE`; the engine now reports `AGED_OUT`. Unlike (A), no Supporting-Data-level justification for this was found - a patient who has legitimately completed their sequence should not read as aged out merely because some other target dose was newly skipped for a dose-count reason. **Not root-caused to a specific line** within this investigation's scope; the mechanism most likely lives in how `PatientSeriesStatus` (`COMPLETE` vs `AGED_OUT`) gets decided once a tail target dose is `SKIPPED` for a dose-count exemption rather than fully evaluated - `DetermineForecastNeed` and wherever `AGED_OUT` gets assigned are the most likely places to look next.

## Why this is still not merged

Unlike SPEC-4.6-0018 (whose regression traced entirely to an already-documented, well-understood, separately-owned defect - Chapter 8's missing series-group loop), this fix's regression set contains at least one **genuinely unexplained defect** (B) and one **unresolved clinical-correctness question** (A). Neither meets the bar this project's revised regression tolerance actually requires - understanding the mechanism, not just measuring the net number.

**Recommended path:** diagnose and fix (B) first - it looks like a self-contained, mechanical `PatientSeriesStatus` bug, not a clinical judgment call, and is the more tractable of the two. Get a domain-expert read on (A) separately; it may turn out the fixture (from CDC's own historical test plan) predates a Supporting Data revision, the same way HepA's conflict did. The fix itself - the one-line additive spelling match - needs no further work and is ready to reapply once both are resolved.

## Affected

- Spec sections: 6.2 (page 56)
- Code locations: `EvaluateConditionalSkip` (the dose-count fix, ready to reapply); `PatientSeriesStatus` assignment (likely `DetermineForecastNeed`, for regression B, not yet root-caused)
- FITS cases: 49 cases would newly pass, 29 would newly fail (25 DTaP - clinical-correctness question; 4 PCV - genuine defect) if merged today
