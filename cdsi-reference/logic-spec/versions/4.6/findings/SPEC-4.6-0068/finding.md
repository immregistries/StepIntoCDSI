# SPEC-4.6-0068: an inadvertent evaluation must name the administered dose

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Why

Six FITS doses are inadvertent for the series being forecast (Tdap to an infant or a child under 7, bOPV, Abrysvo to an infant). The engine rejects them. The evaluation it stores does not name the administered dose, so a report that looks up evaluations by dose never sees them.

Linking the dose, the same way age evaluation already does, makes those six visible. It also makes ten doses that FITS calls valid come out not valid. Those ten are valid on the series that forecasts and inadvertent on a second best series of the same antigen. The forecast itself does not move.

## Evidence

Table 6-13 Rule 1 (Logic Specification v4.6, page 52): target dose status Not Satisfied, evaluation status Not Valid, reason Inadvertent Administration. Figure 6-4 returns to 4.4. Section 6.4 does not run, so it cannot attach the evaluation.

`DataModel.setEvaluationForCurrentTargetDose` sets status, reason, antigen, and `VaccineDoseAdministered`. Rule 1 was doing `new Evaluation()` and setting only status and reason.

`EvaluationComparator` keeps an evaluation only when `getVaccineDoseAdministered()` is set. It then folds every best patient series of one antigen down to one status per dose, and Not Valid beats Valid.

Before the link, `DTAP-2013-0058` best Diphtheria standard series, target 1:

- Not Valid / Inadvertent Administration, no administered dose
- Valid, cvx 107, dose 2

The comparator reported dose 1 (cvx 115) as never evaluated. After the link, the first evaluation is dose 1, cvx 115, antigen Diphtheria. Tetanus and Pertussis match. `POL-2024-0071` target 2 is cvx 178, dose 2. `RSV-2025-0010` is cvx 305, dose 1, on the RSV 1-dose series.

After the link, `PCV-2015-0023` (PPSV23 at age 65, FITS Valid) has two best Pneumococcal series, both pointing at dose 1 cvx 33:

- Pneumococcal 4-dose series: Not Satisfied, Not Valid, Inadvertent Administration
- Pneumococcal 50+ 2-dose PPSV23-PCV series: Satisfied, Valid

`RSV-2024-0012` (Arexvy at age 52, FITS Valid, series complete) has two best RSV series, both pointing at dose 1 cvx 303:

- RSV 1-dose series: Not Satisfied, Not Valid, Inadvertent Administration, patient series Aged Out
- RSV 75 years+ 1-dose series: Satisfied, Valid, Grace Period, patient series Complete

Forecast pass count stayed 1107. `changed-cases.json` `statusChanged` is empty.

## Interpretation

When the target has no evaluation yet, Rule 1 calls `setEvaluationForCurrentTargetDose`. When an evaluation is already there, Rule 1 still writes Not Valid and Inadvertent Administration onto that evaluation, and sets the administered dose only when the evaluation does not already name one. It does not add a second evaluation. `inadvertentAdministration` and `evaluatedAgainstTargetDose` stay as they are (FORECASTDTCAN-1 and CALCDTINT-1).

The ten new mismatches are the comparator's merge. The inadvertent evaluation was always on that second best series; it was dropped only because the administered dose was null. Once it is linked, Not Valid wins over Valid for the same dose id. The comparator was not changed. Calling those ten doses not valid is the wrong signal for a mis-merge check: the series that forecasts already calls them valid.

FITS run `2026-11-02T201705` against `2026-11-02T200516` (same commit `38f3eb04`, engine link present only in the later run):

| | Before | After |
|---|---:|---:|
| Forecast passing | 1107 | 1107 |
| Doses matched | 2505 | 2501 |
| Not-valid doses missed | 20 | 14 |
| Valid doses called not valid | 21 | 31 |

Newly matched: the six A1 doses, current plan `6a2034b8e4b0766bb234ded7`. Newly mismatched: PCV `2015-0023`, `2019-0009`, `2023-0001`, `2024-0063`, `2024-0102`; RSV `2023-0020`, `2024-0012`, `2024-0056`, `2024-0057`, `2025-0009`. Engine tests: 836 run, the same two failures (`PreFilterPatientSeriesTest`, `SelectPrioritizedPatientSeriesTest`).

The owner accepted the link on 2026-11-13, so this finding is confirmed. The comparator is a separate decision.
