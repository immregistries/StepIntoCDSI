# SPEC-4.6-0062: CONDSKIP-1 Total never counted a dose without a targetDose

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

DTAP leftovers after MULTIANTVG-1 still included adult and ≥7 catch-up cases whose next date was 6 months out when FITS wanted a 5-year booster or age 11. Pertussis Dose 8/9 Supporting Data already skips those targets when the patient has Td on or after 7 years. CONDSKIP-1 never saw the Td.

## Evidence

Table 6-5 CONDSKIP-1: count administered doses whose vaccine type is one of the conditional skip vaccine types (or any type if unspecified), whose date administered is in the begin/end age and start/end date windows, and whose Evaluation Status is Valid when the conditional skip dose type is Valid, or any status when the dose type is Total.

Pertussis standard series and start-at-12-months series, Dose 8, context Both, set 2: "Dose is not required if the patient has received 1 or more doses of Td on or after 7 years." `doseType` Total, `doseCount` 0, `doseCountLogic` greater than, `vaccineTypes` 09; 113; 138; 139; 196. Dose 9 set 2 is the same list at `doseCount` 1 (2 or more Td).

`CONDSKIP_1` returned before looking at dose type whenever `vaccineDoseAdministered.getTargetDose()` was null. 6.10 writes `targetDose` only on SATISFIED. Td contains tetanus and diphtheria, not pertussis, so a Td shot never received a Pertussis `targetDose`.

Oracle `DTAP-2020-0008` (adult, dose 3 of Td at 6 months): Tdap 115 then two Td 139. FITS wants ED `2031-09-01` / RD `2036-09-01`. Engine had `2027-03-01`. After this fix: `2031-09-01`.

Oracle `DTAP-2013-0019` (1 Tdap, 2 Td, child ≥7): FITS wants `2030-01-28` (age 11). Engine had an earlier catch-up date. After this fix: `2030-01-28`.

Oracle `DTAP-2013-0010` (#1 Td to #2 Tdap, forecast #3 in six months): FITS wants `2027-03-01`. Engine now `2029-05-05` (11th birthday). Dose 8's 1+ Td skip matches the Supporting Data text; FITS still wants the 6-month third catch-up.

## Fix (scoped)

Total counts a matching administered dose even when it was never evaluated against the current antigen. Valid still requires a Valid evaluation, preferring `evaluatedAgainstTargetDose` (6.10 / 6.3 write this for every outcome) over `targetDose` (SATISFIED only, and a later antigen can overwrite it).

First-match Conditional Skip instance is unchanged. Using every matching instance (Both then Forecast-only) is spec-plausible but lands DTaP catch-up on the 6-month track FITS still expects at 4 weeks (Issue #65 / SPEC-4.6-0019). `2013-0007` stayed PASS.

Out of scope: CALCDT-5 Nov 30 vs Dec 1; Issue #65 4-week vs 6-month; Td 4wk-5d validity (`2020-0007`).

## Verification

Unit: `EvaluateConditionalSkipForEvaluationTest` 47/47 (was 46). New test: Total counts Td CVX 139 with no `targetDose`. Engine suite 824 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T111010` vs SPEC-4.6-0061 run `2026-09-16T103931`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4257 | **4301** | **+44** |
| DTAP | 763 | **807** | **+44** |
| POL | 623 | 623 | 0 |
| COVID-19 | 139 | 139 | 0 |
| MMR | 213 | 213 | 0 |

- FAIL→PASS: `2013-0016`, `2013-0019`, `2013-0021`, `2013-0034`, `2013-0092`, `2013-0134`, `2020-0005`, `2020-0006`, `2020-0008`, `2020-0009`, `2022-0001` (54 copies).
- PASS→FAIL: all five copies of `DTAP-2013-0010` (`2027-03-01` → `2029-05-05`) and `DTAP-2013-0067` (`2027-03-01` → `2030-07-04`). Those ten leave the known-passing allowlist. COVID unchanged (139 PASS / 202 FAIL). Newly passing ids are not added to the allowlist.
- DTAP remaining: 85 fails. `2013-0007` stayed PASS (Issue #65 4-week vs 6-month).