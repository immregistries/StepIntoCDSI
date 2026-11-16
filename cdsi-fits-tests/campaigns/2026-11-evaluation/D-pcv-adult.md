# D: adult PCV13/PCV15 before PPSV23 called inadvertent

Follow `.cursor/rules/fits-evaluation.mdc` and `cdsi-fits-tests/AGENTS.md`. Do not commit.
Do A1 and A2 first; this cluster also goes through the inadvertent-vaccine step.

## Goal

Four adult pneumococcal doses that FITS calls VALID are NOT_VALID with engine reason
INADVERTENT_ADMINISTRATION. Each case is a PCV dose followed a year later by PPSV23 (33).

| Case | Patient | Dose 1 | FITS target |
|---|---|---|---|
| PCV-2015-0021 | born 1960 | 133 PCV13 | 152 |
| PCV-2022-0012 | 66 years | 215 PCV15 | 215 |
| PCV-2023-0002 | adult | 133 PCV13 | 152 |
| PCV-2025-0002 | 55 years | 215 PCV15 | 152 |

```bash
python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --group PCV --all-doses
```

## What we already know

- In Supporting Data 4.65 none of the adult pneumococcal series ("Pneumococcal 50+ 1-dose
  PCV series", "50+ 2-dose PCV13 series", "50+ 2-dose PPSV23-PCV series") lists any
  inadvertent vaccine. Only the childhood series list one, and it is 33 (PPSV23), not a
  PCV.
- So either the engine evaluates these adults in a childhood series, or step 6.3 is
  matching the wrong vaccine type.

## Where to look

1. For PCV-2015-0021, find the best patient series and which target dose each dose was
   evaluated against. If it is a childhood series, look at 5.1 (relevant series by age)
   and 8.x (best series selection).
2. If it is an adult series, look at `EvaluateForInadvertentVaccine`: is it comparing the
   right dose (the AAR being evaluated) with the right target dose's inadvertent list?
   `VaccineType.equals` compares CVX codes, so 133 should never equal 33.
3. A1 changes what 6.3 records; re-run this cluster's cases after A1 is merged before
   digging, in case the picture changed.

## Stop and ask if

- The engine's series choice is defensible under the spec and FITS expects a different
  one. That's a finding (possibly FITS_DIFFERENCE), not a code change.
