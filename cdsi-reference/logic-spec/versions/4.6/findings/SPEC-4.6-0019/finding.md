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

**(B) PCV, 4 cases - root-caused and fixed as its own finding.** Traced `675c41f8e4b089d2bdc0c483-PCV-2023-0002` (an adult patient who received PCV13, then PPSV23, then PCV20 five years later - by the title's own description, a textbook-complete adult pneumococcal sequence): expected `COMPLETE`; the engine reported `AGED_OUT`. Root cause, documented in full at [SPEC-4.6-0033](https://github.com/immregistries/StepIntoCDSI/blob/develop/cdsi-reference/logic-spec/versions/4.6/findings/SPEC-4.6-0033/finding.md): a `markRestAsExtraneous()` placeholder target dose still tracks the same `SeriesDose` - and therefore the same `ConditionalSkip` data - as whichever real target dose it duplicates, so routing it through `EvaluateConditionalSkip` again overwrites its `UNNECESSARY` status back to `SKIPPED`, stranding the whole series without a `PatientSeriesStatus` ever being assigned. Fixed at the shared base class level and verified independently of this finding's own spelling fix (shipped with the Equal row still unfixed): 0 regressions, 11 improvements (6 COVID-19, 5 PCV) - confirming this bug was already live on the unmodified codebase for other cases too.

## Why this is still not merged

With (B) now fixed and merged separately (SPEC-4.6-0033), only (A) - the DTaP clinical-correctness question - still blocks merging this specific "equal to" spelling fix. That's an **unresolved clinical-correctness question**, not a code defect, and doesn't meet the bar this project's revised regression tolerance requires (understanding the mechanism is not the same as having the authority to pick an answer for a genuine ACIP judgment call).

**Recommended path:** filed as [GitHub Issue #65](https://github.com/immregistries/StepIntoCDSI/issues/65) for ACIP/CDSi domain-expert input (see the "Open ACIP/CDSi Clinical Questions" dashboard). Re-attempt this fix once that question is answered - re-measure fresh at that point, since SPEC-4.6-0033 and any other intervening fixes will have further changed the numbers from this round's 49/29 split.

## Affected

- Spec sections: 6.2 (page 56)
- Code locations: `EvaluateConditionalSkip` (the dose-count fix, ready to reapply)
- FITS cases: as of this round, 49 cases would newly pass and 29 would newly fail (25 DTaP - clinical-correctness question, still open; 4 PCV - fixed separately as SPEC-4.6-0033) if merged; re-measure once Issue #65 is resolved
