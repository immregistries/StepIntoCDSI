# SPEC-4.6-0027: ConditionalSkip's missing context tracking is real, but the textually-correct fix regresses FITS and doesn't resolve the case that motivated it

**Status:** open (confirmed defect; fix attempted and reverted twice - see "2026-09-09 re-attempt")
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`ConditionalSkip` carries no `context` field at all, and `SeriesDose` holds exactly one `ConditionalSkip` instance - `DataModelLoader` calls `seriesDose.setConditionalSkip(...)` once per `<conditionalSkip>` XML element found, so a series dose declaring two overwrites the first with the second. In the bundled Supporting Data, every `<conditionalSkip>` element carries a `<context>` (140 "Both", 57 "Evaluation", 67 "Forecast"), and 67 series doses carry two - an Evaluation-or-Both instance followed by a Forecast-only one. For all 67, the Forecast-only instance is the one retained; the Evaluation-or-Both instance the specification says 6.2 Evaluate Conditional Skip must use is silently discarded. 7.1 and 7.6.1 have the mirror-image entry condition ("only Forecast or Both").

This was already recorded as a red unit test from an earlier research pass - `EvaluateConditionalSkipForEvaluationTest.aConditionalSkipInstanceCarriesTheContextThatDecidesWhetherSixTwoMayUseIt` and its 7.1/7.6 siblings - each just a "can this rule even be expressed" probe, deliberately not specifying the fix's shape.

Investigated this round via FITS case `5eeca0522cc4517a96b0f417-MCV-2013-0511` (a single MenACWY dose given at exactly 16 years of age; expected `COMPLETE` per ACIP's "no booster needed if the first dose was given at 16+" rule). Its relevant `<conditionalSkip>` data is declared on dose 1's own `<seriesDose>` element - two sibling instances: one `context=Evaluation` ("Target Dose is not needed if the current dose was administered on or after 16 years - 4 days of age"), one `context=Forecast` ("Target Dose is not required for those who are 16 years of age or older").

## The fix

Added `ConditionalSkipContext` (EVALUATION/FORECAST/BOTH), a `context` field on `ConditionalSkip`, and `appliesToEvaluation()`/`appliesToForecast()` (an unset context - only possible in a hand-built test fixture predating this field, never in real Supporting Data - applies unconditionally, preserving every existing test's behavior). Changed `SeriesDose` to hold a list (`getConditionalSkipList()`), keeping `getConditionalSkip()`/`setConditionalSkip()` as backward-compatible single-instance convenience accessors over the list's first element. `DataModelLoader` now parses `<context>` and appends rather than overwrites. `EvaluateConditionalSkip` now selects, from the list, the first instance whose context matches its own arm (`EVALUATE` → `appliesToEvaluation()`; `FORECAST`/`VALIDATING` → `appliesToForecast()`).

## Verification - and why this isn't merged

All three "can context be expressed" probe tests (6.2, 7.1, 7.6) went green. A full sorted diff of the engine suite's failure list showed **exactly** those three tests flipping and nothing else - zero other unit-test regressions.

The full FITS run told a different story:

- **MCV's own target case did not change at all.** `5eeca0522cc4517a96b0f417-MCV-2013-0511`/`0512` remain `NOT_COMPLETE`, byte-identical to before the fix. Direct instrumentation (before any fix) had already shown dose 2's own `Evaluation` object entirely `null` - 6.2 never even attempted to evaluate it - and this was still true after the fix. Whatever lets dose 1's declared conditional skip affect dose 2's own need was not activated by correcting which `ConditionalSkip` instance 6.2 reads. This scenario needs some cross-dose cascade this round did not locate; the context bug's fix was necessary but evidently not sufficient for it.
- **A net FITS regression appeared elsewhere.** Case-by-case diff against the pre-fix baseline: 19 cases newly fail (15 DTaP, 4 PCV), 5 newly pass (all DTaP) - net **-14**. One specific DTaP case (`5eeca0522cc4517a96b0f417-DTAP-2013-0010`): expected earliest/recommended date `03/01/2027`; before the fix the engine already matched that; after, it regresses to `09/29/2026` - a much earlier date, as if a constraint the forecast genuinely depends on stopped applying.

Reverted in full. A fresh FITS run after reverting confirmed the exact pre-fix baseline is restored (3451 passed, 1 execution error; engine suite failure set sorted-diff-identical to before this investigation started).

## Interpretation

The context/list-storage bug is real, confirmed, and independently documented from an earlier pass - not a misreading. But the fix, while textually faithful to Table 6-4's entry condition, interacts with at least one other, not-yet-identified mechanism. Two live hypotheses, neither confirmed at the time:

1. MCV's "16 years" rule needs a genuinely different mechanism (perhaps `ConditionalNeed`, or a cross-dose cascade not yet located) that fixing 6.2's context selection alone cannot supply, regardless of whether the context bug itself is fixed.
2. Some DTaP series dose's Forecast-context-only conditional skip was, before this fix, incorrectly but load-bearing-ly being read by 6.2 (since context wasn't tracked, nothing excluded it) - and 7.1/7.6's own forecast-side processing was not, in practice, independently supplying the same constraint. If so, 7.1/7.6 may have their own, separate gap that only becomes visible once 6.2 stops over-reading Forecast-context data on their behalf.

## 2026-09-09 re-attempt (Role B round 25)

Per the project owner's explicit request to keep working through 6.2's remaining reds while ACIP feedback (filed as GitHub Issues in round 23) is pending on other findings. The round-17 implementation had been fully reverted with nothing left in git history to reuse, so it was reconstructed identically from this finding's own description and re-tested against everything rounds 18-24 have changed since (unit 4.4's recurring-dose fix, 5.1, 6.1, Table 6-6's inclusive boundary, Table 6-7's series-group check, and the extraneous-placeholder guard from SPEC-4.6-0033 - a lot of intervening ground).

Result: **worse than round 17's measurement, not better.** Full FITS run, case-by-case diffed against the true current baseline: 3627 → 3597 passed, **0 improvements, 30 regressions, all DTaP.** MCV's own target case still did not change - dose 2's `Evaluation` object is still entirely `null`, exactly as before, confirming hypothesis 1 a second time, independently, weeks apart and on a substantially different codebase.

The DTaP regression this time was traced far enough to connect it to something concrete. Every one of the 30 regressed cases (6 distinct scenarios, deduplicated across the fixture set's repeated test-plan-ID prefixes) shows the identical shape:

| Case | Expected earliest | New actual earliest |
| --- | --- | --- |
| `...-DTAP-2013-0010` | 2027-03-01 | 2026-09-29 |
| `...-DTAP-2013-0020` | 2027-02-27 | 2026-03-01 |
| `...-DTAP-2013-0067` | 2027-03-01 | 2026-09-29 |
| `...-DTAP-2020-0005` | 2027-03-01 | 2026-09-01 |
| `...-DTAP-2020-0006` | 2027-03-01 | 2026-09-05 |
| `...-DTAP-2020-0007` | 2026-09-29 | 2026-09-06 |

One direction, every time: the fixture expects the *later* answer (the longer, roughly-6-month interval track), and the context-corrected engine now computes an *earlier* one (the shorter, roughly-4-week interval track). This is the exact same shape as the DTaP regression traced in detail in [SPEC-4.6-0019](../SPEC-4.6-0019/finding.md)'s part A investigation the same day - there, correctly making Table 6-9's Equal row reachable let several intervening DTaP target doses skip for their own, independently-justified reasons, landing the forecast on a later target dose with a longer interval than a historical fixture expects. This fix's context correction does the same thing through a different door: excluding the Forecast-only `ConditionalSkip` instance from 6.2's own evaluation (as Table 6-4 requires) changes which conditions 6.2 sees as true, which changes which target doses get skipped during evaluation, which shifts the forecast the same way.

**This is very likely one clinical question, not two.** Whoever resolves [GitHub Issue #65](https://github.com/immregistries/StepIntoCDSI/issues/65) (filed for SPEC-4.6-0019's DTaP question) should re-attempt this fix at the same time - the same ACIP answer probably settles both findings' DTaP regressions together, since both are the same "which interval track applies once intervening doses skip" question, just reached via two different, independently-buggy code paths.

Reverted in full again; a fresh FITS run confirmed the revert restores the exact round-24 baseline (3627 passed, 0 changed cases).

Recorded as open per the same discipline as SPEC-4.6-0018/0019: a textually-correct fix that regresses FITS needs its interaction fully traced before merging, not shipped on unit-test strength alone.

## Affected

- Spec sections: 6.2 (page 58, Table 6-4's entry condition), 7.1 (page 71, mirror entry condition), 7.6.1 (page 74, same rule cited again)
- Code locations: `ConditionalSkip.java`, `SeriesDose.java`, `DataModelLoader.java`, `EvaluateConditionalSkip.java` (all reverted, no code shipped)
- FITS cases: MCV's target case (unresolved, needs its own cross-dose-cascade investigation); the DTaP regression (30 cases as of 2026-09-09) is believed to be the same open question as [GitHub Issue #65](https://github.com/immregistries/StepIntoCDSI/issues/65)
