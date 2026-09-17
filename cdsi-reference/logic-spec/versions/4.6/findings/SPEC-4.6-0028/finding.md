# SPEC-4.6-0028: Three compounding defects in 4.4's Figure 4-6 terminal handling - recurring dose, off-by-one, and a missing TargetDoseStatus

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Context

First unit tackled under the project owner's revised strategy: stop hunting for FITS-group 100% wins and instead work forward through failing JUnit tests unit by unit, starting at 4.4, with cross-section changes explicitly permitted and FITS regressions acceptable *if the mechanism is understood* rather than a hard gate. `status.yaml`'s own pre-existing notes for unit 4.4 (from an earlier research pass) had already diagnosed two of the three defects below precisely enough to fix from directly; the third was found only after fixing the first two exposed it.

## Defect 1: the recurring-dose check only ever looked at the wrong dose

`moveToNextTargetDoseIfAvailable()` only asked "is this target dose recurring?" once the target dose list was already exhausted - and at that point it checked whichever dose happened to be *last* in the list, not necessarily the one that was just satisfied. Figure 4-6 asks the recurring question of *every* satisfied target dose, mid-series or not. Nine non-final series doses across the bundled Supporting Data release (all COVID-19) declare `recurringDose=Yes`, and none of them ever got a duplicate created - `satisfyingARecurringMidSeriesTargetDoseAlsoCreatesAnotherOne` pinned exactly this.

## Defect 2: `markRestAsExtraneous()`'s off-by-one

By the time any caller reaches `markRestAsExtraneous()`, `aarPos` already points at the first un-evaluated record - a SATISFIED/SUBSTITUTED/UNNECESSARY dose advances `aarPos` past the record it just consumed before calling `moveToNextTargetDoseIfAvailable()`; a SKIPPED dose never advances it at all, so it's still sitting on the current, untouched record either way. The method's loop started at `aarPos + 1`, silently skipping the first genuinely leftover record - it was never marked EXTRANEOUS, never represented by any `TargetDose` at all, just dropped. `everyRecordLeftUnevaluatedWhenTargetDosesRunOutIsMarkedExtraneous` and `anExplicitlyNonRecurringLastTargetDoseAlsoMarksTheRestExtraneous` both pinned this (the second via the "no RecurringDose object at all" arm that previously never reached the marking code because of a related bug in defect 1's original logic).

## Defect 3: the placeholder TargetDose's status was never set (found via a self-inflicted regression, not by inspection)

Fixing defects 1 and 2 alone passed all three targeted JUnits cleanly - a sorted diff of the full 783-test `cdsi-engine` suite showed exactly those three tests flipping and nothing else. The full FITS run told a different story: passed cases dropped from 3451 to 3211, a 245-case regression (HIB 100, DTAP 66, VAR 66, HepB 8, MMR 5) against only 19 improvements. Spot-checking one case per affected group showed two symptoms: HIB/VAR lost their forecasts entirely (`"No forecasted vaccine group matched CVX n"`), while DTaP/HepB/MMR kept a forecast but with the wrong status/dates.

Root-caused by reading `moveToNextTargetDoseIfAvailable()` directly rather than reverting on the strength of the regression's size alone (per this round's new "explain the mechanism before accepting or reverting" discipline): the fix's recurring-dose check fired unconditionally on every call, including calls made for a **SKIPPED** target dose - but `moveToNextTargetDoseIfAvailable()` is invoked for SKIPPED doses too (they never consumed an AAR, so nothing about a copy of one changes why it was skipped). If a SKIPPED dose's own `RecurringDose` is `Yes`, checking recurring status on it creates a duplicate that is *also* SKIPPED on its own turn - which then re-triggers the same duplication, forever, until `EvaluateAndForecastAllPatientSeries`'s own `MAX_TOTAL_CYCLES`/`MAX_REPEATED_STATE_CYCLES` loop guards trip and force an early, incomplete transition to `SELECT_BEST_PATIENT_SERIES`. That explained HIB/VAR's total-forecast-absence symptom exactly.

Gating the recurring-dose check to only fire when the just-processed dose's status was `SATISFIED`, `SUBSTITUTED`, or `UNNECESSARY` (matching the switch statement's own "advance both AAR and target dose" branch at the call site) eliminated the loop-guard trips entirely. The remaining DTaP/HepB/MMR symptom (previously-passing `COMPLETE` cases now wrongly `NOT_COMPLETE`) traced to a separate, pre-existing bug that defect 2's fix simply exposed for the first time: `markRestAsExtraneous()` creates a placeholder `TargetDose` per leftover record and sets its `Evaluation` to `EXTRANEOUS`, but never calls `setTargetDoseStatus(...)` on it - so the placeholder's `TargetDoseStatus` stays at the class default, `NOT_SATISFIED`. `DetermineForecastNeed`'s Table 7-10 condition 0 scans the entire target dose list for any `NOT_SATISFIED` entry to decide whether the series needs another dose - so every leftover-AAR placeholder wrongly forced the whole series to `NOT_COMPLETE`.

This third bug pre-dates this round's changes; it was invisible before because defect 2's off-by-one meant a series with exactly one leftover AAR produced zero placeholders (the buggy loop ran zero iterations for that case), so the missing-status gap was never exercised. Correcting the off-by-one is what first exposed it.

This project's own `statuses.md` concept doc independently confirms Table 3-2 (Target Dose Status) defines only three values - Not Satisfied, Satisfied, Skipped - with no "extraneous" counterpart at all, and separately flags `TargetDoseStatus.SUBSTITUTED`/`UNNECESSARY` as having no direct Table 3-2 correspondence. `UNNECESSARY` is the closest existing value to "an extra dose that wasn't required" and is otherwise dead code today (nothing in the codebase sets it), so using it here needed no enum change.

## Fix

All three fixed together in `EvaluateAndForecastAllPatientSeries.java`, in one bounded round (per this round's relaxed cross-section rule, though in practice only this one file needed changes):

- `moveToNextTargetDoseIfAvailable()` now checks the recurring-dose flag before advancing position (so a mid-series recurring dose's duplicate is present by the time the list-exhaustion comparison runs), but only acts on it when `justProcessedDose.getTargetDoseStatus()` is `SATISFIED`, `SUBSTITUTED`, or `UNNECESSARY`.
- `markRestAsExtraneous()`'s loop now starts at `aarPos`, not `aarPos + 1`.
- Each placeholder `TargetDose` it creates now gets `setTargetDoseStatus(TargetDoseStatus.UNNECESSARY)` explicitly, alongside the existing `EvaluationStatus.EXTRANEOUS` evaluation.

## Verification

- Unit 4.4's own `EvaluateAndForecastAllPatientSeriesTest`: all 21 tests green (0 red, down from 3).
- Full `cdsi-engine` suite: 134 failures, 1 error (down from 137/1) - sorted diff confirms exactly the 3 targeted tests flipped and nothing else changed.
- Full FITS run, case-by-case diffed against the true pre-fix baseline (not the intermediate, loop-guard-tripping attempt): 3451 -> 3511 passed. **0 regressions, 60 improvements** (51 DTaP, e.g. `5eeca0522cc4517a96b0f417-DTAP-2013-0040`; 9 COVID-19, e.g. `675c41f8e4b089d2bdc0c483-COVID-19-2024-0087`). The HIB/VAR/HepB/MMR cases that regressed under the first, incomplete fix attempt show zero net change against the true baseline once defect 1 was correctly gated - that regression was self-inflicted by the first attempt, not a pre-existing condition, and fully resolved.

## Affected

- Spec sections: 4.4 (pages 35-38, Figures 4-5/4-6)
- Code locations: `EvaluateAndForecastAllPatientSeries.java` (`moveToNextTargetDoseIfAvailable()`, `markRestAsExtraneous()`)
- FITS cases: 60 newly passing across DTaP and COVID-19; no regressions
