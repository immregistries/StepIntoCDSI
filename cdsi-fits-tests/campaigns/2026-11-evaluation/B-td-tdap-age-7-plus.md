# B: Td/Tdap catch-up at age 7+ and in adults is called not valid

Follow `.cursor/rules/fits-evaluation.mdc` and `cdsi-fits-tests/AGENTS.md`. Do not commit.

## Goal

Fifteen doses that FITS calls VALID are NOT_VALID in the engine. Almost all of these
cases pass the forecast, which is why the forecast-only dashboard never showed them.

| Case | Dose(s) | Engine reason (Diphtheria/Tetanus) | Pertussis |
|---|---|---|---|
| DTAP-2013-0010 | 2 (Tdap after Td, 28-4 days) | TOO_SOON | VALID |
| DTAP-2013-0016 | 3 (Tdap) | TOO_SOON | VALID |
| DTAP-2013-0017 | 3 (Tdap) | TOO_YOUNG | VALID |
| DTAP-2013-0019 | 2, 3 (Td) | TOO_YOUNG | n/a |
| DTAP-2013-0020 | 2 (Td) | TOO_SOON | n/a |
| DTAP-2013-0021 | 2 (Tdap), 3 (Td) | TOO_YOUNG | VALID |
| DTAP-2013-0067 | 2 (Tdap) | TOO_SOON | VALID |
| DTAP-2013-0068 | 2 (DTaP), 3 (Tdap) | TOO_YOUNG | TOO_YOUNG |
| DTAP-2013-0094 | 2 (Pediarix at 7+, 28 days) | TOO_SOON | TOO_SOON |
| DTAP-2020-0005 | 2 (Td, adult, 4 weeks) | TOO_SOON | n/a |
| DTAP-2020-0006 | 2 (Td, adult, 4 weeks - 4 days) | TOO_SOON | n/a |
| DTAP-2020-0010 | 2 (Td, adult, 6 months - 5 days) | TOO_SOON | n/a |

```bash
python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --group DTAP
```

Note the pattern: in five Tdap doses Pertussis says VALID while Diphtheria and Tetanus
say NOT_VALID, so the antigens are in different places in their series. "Too young" for
a Td dose given to a 7-year-old usually means the dose was evaluated against the wrong
target dose (e.g. an adolescent booster) rather than the catch-up dose.

## Check existing work first

- SPEC-4.6-0062 (open): CONDSKIP-1 only counted doses that had a target dose, so a Td
  never evaluated against Pertussis was invisible to Pertussis' "Td on or after 7 years"
  skips. Names DTAP-2013-0010, 0019, 0067, 2020-0005.
- SPEC-4.6-0063 (open): CONDSKIP-1 counted the dose currently being evaluated. Names
  DTAP-2013-0010, 2020-0005.

Read both and check `git log` for whether their fixes were ever merged. If a fix exists
but was left out (e.g. it caused regressions), say so and start from that analysis
instead of redoing it.

## Where to look

1. For one case (start with DTAP-2013-0019: Tdap then two Td, all after age 7), find the
   best patient series per antigen and which target dose each administered dose was
   evaluated against. The failures bundle `evaluations.json` gives the per-antigen
   verdicts; the step-through UI shows the series and target doses.
2. Step 6.2 conditional skip (`EvaluateConditionalSkip`, `businessRules/CONDSKIP_1`):
   the Diphtheria/Tetanus series use skips such as "start of catch-up at 7+" to jump
   over infant doses. If a skip doesn't fire, the dose lands on an infant target with the
   wrong age/interval rules.
3. Steps 6.4 age and 6.5/6.6 intervals: which interval applies (from immediate previous
   dose vs. from a target dose) for the 7+ catch-up doses.
4. DTAP-2020-0005/0006/0010 are adults with a 4-week / 6-month interval; check those
   last, they may be a separate interval issue.

## Stop and ask if

- The cause is in the open findings above and their fix needs a decision you can't make.
- A fix for Diphtheria/Tetanus changes Pertussis results, or any passing forecast.
