# A1: inadvertent doses come out "never evaluated"

Follow `.cursor/rules/fits-evaluation.mdc` and `cdsi-fits-tests/AGENTS.md`. Do not commit.

## Goal

Six doses that FITS calls NOT_VALID with reason J (inadvertent vaccine) are reported as
NOT_EVALUATED: no antigen in the engine's best patient series has an evaluation linked
to the dose. A production record would silently lose these invalid doses.

| Case | Dose | Vaccine | FITS | Engine |
|---|---|---|---|---|
| DTAP-2013-0058 | 1 | 115 Tdap to a 2-month-old | NOT_VALID / J | NOT_EVALUATED (D, T, P) |
| DTAP-2013-0059 | 2 | 115 Tdap to an infant | NOT_VALID / J | NOT_EVALUATED (D, T, P) |
| DTAP-2013-0060 | 3 | 115 Tdap to a child under 4 | NOT_VALID / J | NOT_EVALUATED (D, T, P) |
| DTAP-2013-0069 | 1 | 115 Tdap at 7 years minus 5 days | NOT_VALID / J | NOT_EVALUATED (D, T, P) |
| POL-2024-0071 | 2 | 178 bOPV after April 2016 | NOT_VALID / J | NOT_EVALUATED (Polio) |
| RSV-2025-0010 | 1 | 305 Abrysvo (maternal RSV) to an infant | NOT_VALID / J | NOT_EVALUATED (RSV) |

```bash
python cdsi-fits-tests/campaigns/2026-11-evaluation/eval_report.py --cases DTAP-2013-0058,DTAP-2013-0059,DTAP-2013-0060,DTAP-2013-0069,POL-2024-0071,RSV-2025-0010
```

All six are inadvertent vaccines in Supporting Data 4.65 (115 for the Diphtheria and
Pertussis series, 178 for every Polio series, 305 for the RSV infant series). Most of
these cases already pass the forecast, so the rejection itself probably happens; what's
missing is the record of it.

## Where to look first

1. `cdsi-engine/.../core/logic/EvaluateForInadvertentVaccine.java`, the YES outcome of
   Table 6-13. When the target dose has no evaluation yet it creates `new Evaluation()`
   and sets status and reason, but compare that with
   `DataModel.setEvaluationForCurrentTargetDose(...)`: that helper also sets the antigen
   and the `VaccineDoseAdministered`. An evaluation with no administered dose can't be
   tied to a dose by anything downstream (the FITS comparator skips it, and so would a
   production report).
2. Confirm this is the cause before changing anything: check what evaluation, if any, is
   on the target dose after 6.3 for DTAP-2013-0058 (step-through UI or a focused unit
   test), and whether the same path explains POL-2024-0071 and RSV-2025-0010.
3. Read the 6.3 step package
   (`cdsi-reference/logic-spec/versions/4.6/steps/06-03-evaluate-for-inadvertent-vaccine/index.md`)
   for what state changes the spec requires for an inadvertent dose.

## Related findings

- SPEC-4.6-0060 (open): inadvertent previous dose and CALCDTINT-1; names POL-2024-0071
  and DTAP-2013-0060. Your change touches the same path; check its forecast effect.
- SPEC-4.6-0024 (confirmed): FORECASTDTCAN-1 and inadvertent doses; names RSV-2025-0010.

## Watch for

- The comment in the YES outcome explains why `evaluatedAgainstTargetDose` is written
  there (CALCDTINT-1). Keep that behavior.
- Linking the evaluation may change which series scores best (8.x counts evaluations),
  and so the forecast. Check `statusChanged` in `changed-cases.json`.

## Stop and ask if

- The evaluation is linked correctly and the doses are still NOT_EVALUATED. Then the gap
  is elsewhere (series selection, or the comparator), and the owner should decide.
- Fixing it changes the forecast of any currently passing case.
