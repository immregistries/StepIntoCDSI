# SPEC-4.6-0063: CONDSKIP-1 counted the dose currently being evaluated

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

DTAP leftovers after CONDSKIP-1 Total (SPEC-4.6-0062) still included two-valid no-Td catch-up cases whose next date was age 11 when FITS wanted a 6-month Dose 9. Pertussis start-at-12-months Dose 8 already skips when the patient has more than 1 Valid dose. CONDSKIP-1 counted the shot 6.2 was evaluating as one of those Valids.

## Evidence

Table 6-5 CONDSKIP-1: count administered doses whose vaccine type is one of the conditional skip vaccine types (or any type if unspecified), whose date administered is in the begin/end age and start/end date windows, and whose Evaluation Status is Valid when the conditional skip dose type is Valid, or any status when the dose type is Total.

6.2 runs after 6.1 accepts the current dose for evaluation. That VDA is already on `ImmunizationHistory.vaccineDoseAdministeredList`.

Pertussis start-at-12-months series, Dose 8, context Both, set 1: "Dose is not required if the patient has received more than 1 dose." `doseType` Valid, `doseCount` 1, `doseCountLogic` greater than, empty `vaccineTypes`.

Oracle `DTAP-2013-0008` (DTaP at ≥12 months, Tdap at 7): FITS wants ED `2027-03-01` (6-month Dose 9). Engine skipped Dose 8 because Valid count was 2 (DTaP plus the Tdap under evaluation), satisfied Dose 9 with Tdap, and forecast Dose 10 at the 11th birthday. After this fix: Tdap satisfies Dose 8, forecast Dose 9 at `2027-03-01`.

Oracle `DTAP-2013-0065` (Issue #65 4-week vs 6-month cluster): FITS wants 4 weeks. Engine had 6 months because Dose 8 was skipped. After this fix: 4 weeks.

Oracle `DTAP-2020-0005` (adult Tdap then Td at 4 weeks): FITS wants `2027-03-01` (6 months from the Td). Engine after this fix: `2027-02-04` (6 months from the Tdap). Same shape as Issue #65 / SPEC-4.6-0019.

## Fix (scoped)

When `ConditionalSkipType` is EVALUATE, CONDSKIP-1 skips the current `AntigenAdministeredRecord`'s VDA by object identity. 7.1 and 7.6 pass null, so every administered dose still counts.

First-match Conditional Skip instance is unchanged. Evaluating every matching instance (Both then Forecast-only) is still out of scope (Issue #65 / SPEC-4.6-0019). CALCDT-5 Nov 30 vs Dec 1 is unchanged.

## Verification

Unit: `EvaluateConditionalSkipForEvaluationTest` 49/49 (was 47). Two new tests: the dose under evaluation is not counted; two prior Valids still are. `EvaluateConditionalSkipForForecastTest` 13/13 (was 12). New test: forecasting still counts the last administered dose. Engine suite 827 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T114454` vs SPEC-4.6-0062 run `2026-09-16T111010`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4301 | **4400** | **+99** |
| DTAP | 807 | **867** | **+60** |
| COVID-19 | 139 | 178 | +39 |
| POL | 623 | 623 | 0 |
| MMR | 213 | 213 | 0 |

- FAIL→PASS DTAP: `2013-0008`, `2013-0010`, `2013-0020`, `2013-0065`, `2013-0067`, `2013-0088`, `2013-0093`, `2013-0094`, `2013-0128`, `2013-0135`, `2013-0163`, `2020-0004`, `2020-0007`, `2020-0010` (70 copies).
- PASS→FAIL DTAP: all five copies of `DTAP-2020-0005` (`2027-03-01` → `2027-02-04`) and `DTAP-2020-0006` (`2027-03-01` → `2027-02-08`). Neither was on the known-passing allowlist. Issue #65 leftover: FITS wants 6 months from the Td; engine now intervals from the Tdap. COVID +39 FAIL→PASS is parked and not added to the allowlist.
- DTAP remaining: 25 fails / 5 uids. CALCDT-5 `2013-0011`/`0095`/`0136` (Nov 30 vs Dec 1) and these two `0005`/`0006` copies.
