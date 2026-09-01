# 7.6 Validate Recommendation

> **Review status:** draft. Like 7.3, this section's stated purpose does not currently run - see Review Findings.

## Source

Logic Specification for ACIP Recommendations v4.6, page 83. Figure 7-9 (Validate Recommended Dose Process Model). No table of its own - section 7.6.1 states its logic "is the same as described in Chapter 6.2" (a third reuse of the shared conditional-skip logic, alongside 6.2 itself and 7.1). Business rule CONDSKIP-2 (reused, per the spec text - not a rule this section defines).

## Purpose

**[SPEC]** "Validate Recommendation interrogates the forecasted earliest date to ensure the forecast makes logical sense. Conditional Skip is used to determine if a forecast is illogical and thus in need of a complete re-forecasting." **[SPEC]**'s own example: a patient behind on Hib gets a first dose at 11 months 1 week, is recommended a catch-up dose in 4 weeks (just past 12 months) - but by the time they return 4 weeks later, the freshly recalculated forecast would actually skip that dose and recommend one 8 weeks out instead. Validating prospectively at forecast time is meant to catch this kind of "forecast that will already be wrong by the time it matters" case before it's returned.

**[SPEC]** Section 7.6.1: "The process model, attribute table, and decision table... [are] the same as described in Chapter 6.2. Only Conditional Skip Instances with a context of Forecast or Both should be used. In cases where a target dose does not specify Conditional Skip attributes, the target dose cannot be skipped. In CONDSKIP-2, the Earliest Date is used" - i.e. this invocation's reference date for the skip check is the forecast's own earliest date, not date-administered (6.2) or assessment date (7.1).

## Entry Conditions

**[SPEC]** Runs after 7.5 has produced a forecast (there's a date to validate).

## Inputs and Attributes, Business Rules, Decision Tables

**[SPEC]** Identical to 6.2's Tables 6-4 through 6-11 - see [6.2's index.md](../06-02-evaluate-conditional-skip/index.md). Not duplicated here.

## State Changes

**[SPEC]** If the conditional-skip check says the dose (at its forecasted earliest date) would actually be skippable, the specification implies re-forecasting is needed rather than returning the now-illogical recommendation.

**[IMPLEMENTATION]** **This never happens.** `ValidateRecommendation extends EvaluateConditionalSkip` and its constructor wires the same skip/no-skip destination pattern as 6.2 and 7.1 (`ConditionalSkipType.VALIDATING`, skip→7.1, noSkip→4.4) - but `process()` is **overridden** to skip the inherited logic entirely:
  ```java
  @Override
  public LogicStep process() throws Exception {
    // setNextLogicStepType(LogicStepType.FORECAST_DATES_AND_REASONS);
    setNextLogicStepType(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES);
    return next();
  }
  ```
  `evaluateLogicTables()` is never called, so none of the inherited conditional-skip decision tables run - the constructor's skip/no-skip wiring is unreachable dead code as currently written. Every forecast proceeds straight to 4.4 regardless of whether it would actually be skippable at its own earliest date. There's also a commented-out reference to a `FORECAST_DATES_AND_REASONS` LogicStepType that doesn't appear to exist under that name in the current `LogicStepType` enum (likely a stale reference from before the chapter was split into 7.1-7.6) - included here as a minor documentation-only artifact, not a functional issue since it's commented out.

## Next Steps

**[SPEC]** Not stated as a transition rule - implied: if valid, forecasting is done for this target dose; if not, re-forecast.

**[IMPLEMENTATION]** Unconditional to **4.4**, always - the "re-forecast via 7.1" path the constructor sets up is never taken. See `transitions.yaml`.

## Plain-Language Walkthrough

As specified, this step is meant to be a final sanity check: take the date just forecast in 7.5, and ask "if the patient actually came back on that exact date, would this dose still make sense, or would some conditional-skip rule have already made it unnecessary by then?" If it wouldn't make sense, the specification wants a full re-forecast rather than handing back a recommendation that's already stale. As implemented, this check is bypassed entirely - the step always reports success and moves on, so a forecast that would in fact be logically inconsistent at its own recommended date is never caught here.

## StepIntoCDSi Implementation

- `org.openimmunizationsoftware.cdsi.core.logic.ValidateRecommendation` (LogicStepType `VALIDATE_RECOMMENDATION`) - `cdsi-engine`, extends the same `EvaluateConditionalSkip` base as 6.2 and 7.1.
- Tests: no dedicated unit test.

## Review Findings

- **`IMPLEMENTATION_MISMATCH` (draft, verified in code):** `process()` is overridden to bypass the inherited conditional-skip evaluation entirely, always transitioning to 4.4. The specification's stated purpose for this section - catching a forecast that would already be logically invalid at its own recommended date, and triggering re-forecasting - does not currently run. This is the second section in Chapter 7 (after 7.3) where the class exists, is wired into the processing chain, and cites the correct business rule (CONDSKIP-2) and shared logic, but the actual validation never executes. Needs engineering follow-up to either finish the override (call `evaluateLogicTables()` properly with the VALIDATING context) or confirm deliberately deferring this check is an accepted, temporary simplification.
