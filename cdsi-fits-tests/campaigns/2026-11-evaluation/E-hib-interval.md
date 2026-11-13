# E: Hib dose 5 days short of the interval called valid

Follow `.cursor/rules/fits-evaluation.mdc` and `cdsi-fits-tests/AGENTS.md`. Do not commit.

## Goal

One dose. HIB-2013-0308 ("#4 at 12 months of age, interval between #3-4 8 weeks - 5
days"): dose 3 (cvx 48, on the evaluation date) is NOT_VALID / D (too soon) in FITS and
VALID in the engine, with no reason. The forecast also fails.

```bash
python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --cases HIB-2013-0308
```

## Check existing work first

This case is already analysed:

- SPEC-4.6-0018 (confirmed): Table 6-6's conditional-skip begin-age boundary must be
  inclusive (`>=`) but the code used a strict comparison. Names HIB-2013-0308. The fix was
  confirmed in isolation but not merged because it caused a 14-case FITS regression at
  the time. Read the finding for why.
- SPEC-4.6-0020 (confirmed): best-series selection (SELECTB rules).

So the likely story is: a conditional skip fires (or doesn't) on the wrong side of the
12-month boundary, the dose lands on a target dose with a shorter interval, and so a dose
5 days short (beyond the 4-day grace period) passes.

## What to do

1. Re-test SPEC-4.6-0018's fix against today's fixtures (1137 cases, current CDSi +
   AIRA). The 14-case regression was measured on the old 4896-case set; see whether it
   still happens, and whether it also changes dose evaluations.
2. If the regression is gone or now explained, propose merging that fix. If not, update
   the finding with what you measured and leave it unmerged.

## Stop and ask if

- The fix still regresses other cases. Report which ones and why; the owner decides.
