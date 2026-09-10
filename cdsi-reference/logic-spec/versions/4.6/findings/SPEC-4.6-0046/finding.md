# SPEC-4.6-0046: Rewrite Chapter 8 series selection against CDSi Logic Spec v4.6

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Chapter 8 scoring still failed Role A tests after the SPEC-4.6-0045 foundation (`36106fb5`). Copilot's later commit-2 attempt (null-status admit plus Yoda-equals in 8.2/8.3) moved 201 FITS cases (161 PASS→FAIL, 40 FAIL→PASS, net −121) and was discarded. This rewrite keeps the foundation and rewrites 8.1, 8.2, 8.3, 8.5, 8.6, and 8.7 against Tables 8-2, 8-3, 8-5, 8-9, 8-11, and 8-13. Unit 8.4 (SPEC-4.6-0044) and Chapter 9 are not in this change.

Engine suite after commit `85234203`: 783 tests, 26 failures, 0 errors (was 62 failures). Chapter 8 unit counts:

| Unit | After rewrite | Remaining intended reds |
| --- | --- | --- |
| 8.1 PreFilterPatientSeries | 23/24 | SELECTSCORE-2 Risk priority compared Risk-only |
| 8.2 IdentifyOnePrioritizedPatientSeries | 14/14 | none |
| 8.3 ClassifyScorablePatientSeries | 17/17 | none (Table 8-5 gap recorded below) |
| 8.5 InProcessPatientSeries | 37/37 | none |
| 8.6 NoValidDoses | 26/26 + 5 completable | gender / max-age remain no-ops |
| 8.7 SelectPrioritizedPatientSeries | 20/21 | 8.7 still compares raw integers |

FITS vs the 2026-09-10 `develop` baseline (3637/4896, 74.3%):

- After rewrite: **3749/4896 (76.6%)**, 1146 failed assertions, 1 execution error. **Net +112.**
- Two identical runs on `85234203` produced `statusChanged: []`.
- HIB: 90 → 45 fails (−45). **HIB-2013-0281 now PASSES** on all five fixture copies.
- HPV: 138 → 98 (−40). PCV: 179 → 155 (−24).
- DTAP: 131 → 147 (**+16** documented regression).
- COVID-19 (260), FLU (71), POL (221) unchanged (SPEC-4.6-0023 seasonal-date cluster).
- Execution error unchanged: `HepA-AART-EXTRA-10` unrecognized CVX `143`.
- `known-passing-cases.txt` was **not** regenerated. Honest comparison is vs the 3637 develop baseline, not that stale allowlist.

Group-level numbers are in `fits-group-delta-vs-develop.json`. The second-run comparison is in `changed-cases-second-run.json`.

## Interpretation

The rewrite is the smallest chapter-wide alignment that makes the Role A tables true without inventing Chapter 7 statuses or a second score field.

**8.1 Table 8-2.** SELECTB-24 admits a null status as not Contraindicated (`!CONTRAINDICATED.equals(status)`), closing the SPEC-4.6-0025 one-line trap by also making later Chapter 8 steps null-safe. SELECTSCORE-2 Standard valid-dose is SATISFIED + VALID, checked against `maxAgeToStart` on the earliest valid administered date. The third Standard bullet (group has no valid dose and no default series) is implemented. Antigen filter applies only when `dataModel.getAntigen()` is set, because isolated unit tests often omit it and `belongsToCurrentAntigen` with a null antigen would empty every scorable list. SELECTSCORE-2 Risk priority is still compared **Risk-only** (remaining red; SPECIFICATION_AMBIGUITY). The historical default-series fallback when nothing qualified and no valid dose exists is kept as SPECIFICATION_AMBIGUITY, not treated as a defect.

**8.2 Table 8-3.** Default-series count walks the relevant patient series (SELECTB-7), not the scorable list, so Rule 1 and Rule 5 can fire. In-process is counted per series (`break` after the first SATISFIED dose). Outcomes 0 and 4 go to `SELECT_NEXT_SERIES_GROUP`. Status comparisons are null-safe (`COMPLETE.equals` / `NOT_COMPLETE.equals`).

**8.3 Table 8-5.** In-process count is NOT_COMPLETE + at least one SATISFIED (SELECTB-16). Valid-dose count is any SATISFIED (SELECTB-21). Table 8-5 has a partition gap: 1 complete + 1 in-process matches no column. The default outcome remains `NO_VALID_DOSES`. Role A tests assert Rule 2 does not fire there; this rewrite does **not** auto-route a lone in-process group to 8.5.

**8.5 Table 8-9.** Scores in-process series only (SELECTB-16). Product + all valid administered uses `scoreIndependent` ±2. Completable uses `isCompletable` (strict `before`) ±3. Most valid doses and closest to completion use `scoreExtremum`. Finish-earliest uses `forecastFinishDate` (SELECTB-12, with `adjustedPastDueDate` fallback) and skips the metric when the series is not completable.

**8.6 Table 8-11.** Start-earliest and completable use the shared scorer. Product is a **penalty**. Gender and exceeded-maximum-age methods exist as empty no-ops and are not called from `evalTable()` (SPEC-4.6-0007). A series with no forecast is not scored on the completable row (score 0), unlike 8.5 which awards −3.

**8.7 Table 8-13.** Empty or whitespace series preference is no preference. The scorable list is read at use time, not construction. Next step is `SELECT_NEXT_SERIES_GROUP`. 8.7 still compares raw integers; 8.1 `resetScore()` is the FITS-facing fix. This rewrite does **not** invent a “this selection only” score.

**FITS.** +112 vs develop is honest and deterministic. DTAP +16 is allowed and documented. COVID/FLU/POL need fixture refresh or a FITS_DIFFERENCE finding, not a nearest-season hack. Chapter 9 (Codex / GitHub issue #71) can land later; rebase that work onto this Chapter 8 rewrite before judging combined FITS.

## Affected

- Spec sections: 8.1, 8.2, 8.3, 8.5, 8.6, 8.7 (pages 87–91)
- Code locations: `PreFilterPatientSeries`, `IdentifyOnePrioritizedPatientSeries`, `ClassifyScorablePatientSeries`, `InProcessPatientSeries`, `NoValidDoses`, `SelectPrioritizedPatientSeries`, `PatientSeriesScoring`, `PatientSeries`
- FITS cases: `HIB-2013-0281` now passing; DTAP net +16 fails; COVID-19 / FLU / POL unchanged
