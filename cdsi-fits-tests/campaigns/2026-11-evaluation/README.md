# Campaign 2026-11: dose-evaluation mismatches

Goal: make cdsi-engine's per-dose evaluation (valid / not valid / extraneous) agree with
NIST FITS, because the evaluation will be used to flag production records with many
invalid doses (a sign of a possible mis-merge). Wrongly calling a valid dose not valid
inflates that signal; missing a not-valid dose hides it.

Each prompt file below is meant for one fresh Cursor chat. The shared ground rules are
in `.cursor/rules/fits-evaluation.mdc` (Cursor attaches it when working in
`cdsi-engine` or `cdsi-fits-tests`; mention it explicitly if it doesn't).

## Baseline (2026-11-02, commit after `0b5bafb`, reference set `acip-4.6-sd-4.65-fits-4f705ec9`)

| Measure | Value |
|---|---|
| FITS cases (current CDSi + AIRA v3 + AIRA Extra + sample) | 1137 |
| Forecast passing | 1107 (29 fail, 1 error) |
| Doses compared / matched | 2556 / 2505 (98.0%) |
| Valid doses called not valid | 21 of 2299 |
| Valid doses called extraneous | 10 of 2299 |
| Not-valid doses missed (called valid or never evaluated) | 20 of 240 |
| Extraneous doses matched | 17 of 17 |
| cdsi-engine unit tests | 835 run, 2 known failures (8.1/8.7) |

## Order of work

Work strictly in this order and re-measure after each one: a fix early in the list can
change cases later in the list, and you want to see that before starting the next.

| # | Prompt | Doses | Symptom | Status |
|---|---|---|---|---|
| A1 | [A1-inadvertent-dose-not-linked.md](A1-inadvertent-dose-not-linked.md) | 6 | Inadvertent doses (Tdap to infants, bOPV, Abrysvo to an infant) come out "never evaluated" | done (2026-11-13) |
| A2 | [A2-cervarix-to-males.md](A2-cervarix-to-males.md) | 13 | Cervarix given to males called valid; FITS says inadvertent | not started |
| B | [B-td-tdap-age-7-plus.md](B-td-tdap-age-7-plus.md) | 15 | Td/Tdap catch-up at 7+ and adults called not valid (too soon / too young) | not started |
| C | [C-covid-19.md](C-covid-19.md) | 12 | Seasonal COVID-19 doses called extraneous (65+) or too soon (children) | not started |
| D | [D-pcv-adult.md](D-pcv-adult.md) | 4 | Adult PCV13/PCV15 before PPSV23 called inadvertent | not started |
| E | [E-hib-interval.md](E-hib-interval.md) | 1 | Hib dose 5 days short of the interval called valid | not started |

Status values: not started, investigating, fix proposed (awaiting review), merged,
finding only (no code change), deferred.

A1 and A2 together are 19 of the 20 missed not-valid doses; B is 15 of the 21 valid
doses called not valid. Those three move the two error rates the most.

A1 (SPEC-4.6-0068, committed 2026-11-13): the six target doses match, and forecast
passing stays 1107. Linking the evaluation also makes the harness call ten
previously valid doses not valid (five adult PCV, five adult RSV), because
those doses are inadvertent on a second best series of the same antigen and
Not Valid wins the rollup. Doses matched 2505 to 2501. The owner accepted
the link; the ten rollup mismatches are a harness question for
`EvaluationComparator`. A2 is next.

## Measuring

From the repository root:

```bash
mvn -pl cdsi-engine -am install -DskipTests
mvn -pl cdsi-fits-tests test          # add `clean` if fixtures changed
python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py
python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --cases DTAP-2013-0058,POL-2024-0071
```

`eval_report.py` prints the headline numbers against the baseline above, the dose-level
changes since the previous run (`changed-cases.json`), and the mismatched doses with the
engine's per-antigen status and reason. After a reviewed fix is merged, regenerate the
dashboards from `cdsi-reference/`:

```bash
.venv/Scripts/python -m cdsi_reference_tools fits-tests dashboard
.venv/Scripts/python -m cdsi_reference_tools dashboards index
```

## What changed in the harness to support this campaign

- Every dose in `results.jsonl` and `failures/<case>/evaluations.json` now has
  `perAntigenReason`: the engine's own reason (TOO_YOUNG, TOO_SOON, GRACE_PERIOD,
  INADVERTENT_ADMINISTRATION, ...) next to FITS' `expectedReason` letter code. The FITS
  dashboard shows it beside each antigen.
- `changed-cases.json` now lists `evaluationStatusChanged`, `dosesNewlyMatched`,
  `dosesNewlyMismatched` and `dosesStillMismatchedButChanged`, so an evaluation
  regression is as visible as a forecast one.
- `FitsDownloader` was writing every patient as female. It now records FITS' patient
  sex; 185 fixtures changed to male and the reference set was re-created. The totals
  above did not move, which is itself a clue for A2.
