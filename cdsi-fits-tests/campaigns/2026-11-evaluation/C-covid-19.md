# C: seasonal COVID-19 doses called extraneous or too soon

Follow `.cursor/rules/fits-evaluation.mdc` and `cdsi-fits-tests/AGENTS.md`. Do not commit.

## Goal

Twelve COVID-19 doses that FITS calls VALID (FITS target CVX 213):

- **Ten doses, adults 65+, called EXTRANEOUS** (no reason): COVID-19-2025-0113 (doses
  2-3), 2025-0114 (doses 2-4), 2025-0130 (doses 2-6). The first dose is valid and every
  later dose is extraneous, over several seasons. All three also fail the forecast.
- **Two doses, children, called NOT_VALID / TOO_SOON**: COVID-19-2025-0075 dose 2 (21
  days after dose 1) and 2025-0100 dose 2 (22 days after dose 1). Both pass the forecast.

```bash
python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --group COVID-19 --all-doses
```

## What we already know

- In Supporting Data 4.65 the standard series for 2 years+ is "COVID-19 start at 2
  years+ shared clinical decision-making series": 2 doses, where **dose 1 is recurring
  (Yes) and dose 2 is not (No)**. That's an unusual shape: the recurring dose comes first.
- The EXTRANEOUS status with no reason comes from
  `EvaluateAndForecastAllPatientSeries.markRestAsExtraneous()` (around lines 391 and
  539-563): when no target dose remains and the dose just processed was not recurring,
  every remaining dose is marked extraneous.

## Where to look

1. How a recurring dose 1 followed by a non-recurring dose 2 is walked in 4.4: when is
   the recurring target duplicated, which target does dose 2 get, and why do doses 2+
   fall into `markRestAsExtraneous`. Read the 4.4 step package and the recurring-dose
   business rules in the spec.
2. For the children's TOO_SOON cases, find which target dose and which interval the
   second dose was checked against. If it was checked against the recurring dose's
   interval (e.g. a seasonal gap) while FITS treats it as a valid second dose of the
   season, that is the same walking problem as above.
3. Check whether any existing finding covers COVID recurring doses
   (`grep -ri recurring cdsi-reference/logic-spec/versions/4.6/findings`).

## Stop and ask if

- The fix needs a reading of the spec's recurring-dose rules that isn't clearly
  supported by the text. COVID-19 supporting data changes often; record what you find.
- Any non-COVID series with a recurring dose (Influenza, some boosters) changes
  results. Run the full suite and check every changed case.
