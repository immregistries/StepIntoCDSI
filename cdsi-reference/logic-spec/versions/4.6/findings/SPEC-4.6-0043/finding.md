# SPEC-4.6-0043: Wiring 7.6's validation logic for real is JUnit-clean but triggers a genuine skip/re-forecast infinite loop in production - reverted

**Status:** open (reverted, not merged)
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Unit 7.6 (Validate Recommendation) has two narrowly-scoped, well-diagnosed defects:

**1. `ValidateRecommendation.process()` was overridden to unconditionally advance** to `EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES` without ever calling `evaluateLogicTables()`. The section's whole purpose - "Conditional Skip is used to determine if a forecast is illogical" - has never run once in this engine's history.

**2. The shared `EvaluateConditionalSkip` base class's CONDSKIP-2 reference-date switch had a placeholder `VALIDATING` arm** that always used the `PAST` sentinel (01/01/1900) instead of the forecast's own earliest date, so even if 7.6's tables ran, every conditional-skip window would be permanently out of range.

Fixing both (deleting the override; substituting the real earliest date in the `VALIDATING` arm) took unit 7.6's own suite from 6/15 to 13/15 green, and ran completely clean against the rest of `cdsi-engine`: a sorted diff across all 783 tests - including 6.2's and 7.1's own suites, re-run explicitly alongside 7.6's since all three share this exact base class - confirmed exactly the 6 targeted tests flipped and nothing else changed.

**The full FITS suite told a different story.** `passedCases` dropped from 3637 to 3486 (-151), and `executionErrors` rose from the standing baseline of 1 to 234. 233 of those 234 were a brand-new exception never seen in this Role B pass: `IllegalStateException: Logic steps stuck in a loop at 7.1` / `7.2` / `7.3` / `7.4` / `7.5` / `7.6` (63 at 7.4, 59 at 7.2, 44 at 7.6, 40 at 7.1, 20 at 7.3, 7 at 7.5) - the engine's own loop-guard tripping.

This is exactly the blind spot unit-level JUnit tests cannot see: every one of 6.2's, 7.1's and 7.6's own test classes hand-builds a single target dose in isolation and never drives `process()` across a real, multi-target-dose patient series through repeated skip-and-re-forecast cycles. Only a real FITS case's full 4.4-through-7.6 loop can surface a cycle that never terminates - and since 7.6's decision tables have never actually executed before, this cycle has been latent and completely invisible until today.

## What happened, and why it was reverted

7.6 loops back to 7.1 on a "skip" outcome for re-forecasting; 7.1's own skip outcome loops back to 4.4. The specification clearly intends this to terminate - a skipped dose should not remain permanently re-forecastable through the exact same skip condition - but for a large number of real patients (the loop guard fired for 233 distinct cases, spread across every step from 7.1 to 7.6), it does not.

Reverted in full via `git checkout` on both files, confirmed by a fresh build (`ValidateRecommendationTest` back to 8 failures/15, matching the pre-attempt state) and a clean `git status`.

## Why this is recorded as related to, but distinct from, SPEC-4.6-0027

7.1's, 7.6's and 6.2's CONDSKIP-2 reference-date arms are the same three-way switch already tracked as incomplete and regression-prone in [SPEC-4.6-0027](../SPEC-4.6-0027/finding.md) (the `ConditionalSkip` context field, open pending GitHub Issue #65's ACIP guidance on the DTaP catch-up interval question). It's plausible the same underlying clinical/data question that makes SPEC-4.6-0027's fix regress DTaP cases is also why a target dose keeps re-qualifying for the same skip on every re-forecast pass here - but that has not been traced, so this is recorded as a related, open question rather than assumed identical. Whoever picks this up should trace one looping FITS case end to end (e.g. `5eeca0522cc4517a96b0f417-DTAP-2013-0074`) before reattempting either fix.

## A separate, minor finding along the way

`ValidateRecommendationTest.theIntervalConditionIsAnsweredAgainstTheForecastedEarliestDateToo` never populates `dataModel.getImmunizationHistory()`'s dose list, so Table 6-8's own "has at least one dose been administered" condition (verified correct and unrelated to this defect) answers No regardless of the reference-date fix, leaving `onlyConditionTable().isMet()` false. Not a `cdsi-engine` defect - a one-line test-fixture gap, left undisturbed since editing tests is out of scope for Role B.

## Disposition

Classified `blocked_category: would_regress_other_tests` per `cdsi-engine/AGENTS.md` step 6 - any FITS regression is an automatic stop, not a judgment call, even with a fully clean JUnit run. The project owner should decide whether to trace the loop directly or defer 7.6 until SPEC-4.6-0027 resolves.

## Affected

- Spec sections: 7.6 (page 85)
- Code locations: `ValidateRecommendation.java`, `EvaluateConditionalSkip.java` (shared with 6.2, 7.1)
- FITS cases: 233 real cases entered an unterminated skip/re-forecast loop under the attempted fix (not shipped)
