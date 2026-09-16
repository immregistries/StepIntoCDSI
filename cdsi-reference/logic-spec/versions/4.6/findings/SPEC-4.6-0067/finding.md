# SPEC-4.6-0067: SELECTB-2 must see every evaluation on a target, not only the last one

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

HIB leftovers after blank-maxAge completable (SPEC-4.6-0066) were 20 fails / 4 current-plan uids. Two of those are CALCDT-5 (skip). `2013-0348` already had the FITS-correct dates on the 12-month 2-dose series; Chapter 8 published the PRP-OMP product series instead. The same last-evaluation reading also left HepB `2018-0020` (Heplisav product path leftover from 0066) and MEN B `2024-0081` (Bexsero then Trumenba) on a product series.

## Evidence

SELECTB-2: a scorable patient series has all valid doses only if **all evaluations** based on the target doses that are part of the series have evaluation status `Valid`. Table 8-9 awards +2 / −2 for product (SELECTB-23) **and** all valid doses.

`TargetDose.getEvaluation()` returns the last entry in `evaluationList`. 6.10 writes Not Valid when the dose is not allowable, then a later allowable dose **adds** a Valid evaluation on the same target. `hasAllValidAdministeredDoses` inspected only that last evaluation, so `[Not Valid, Valid]` counted as all valid.

Live `HIB-2013-0348`: DOB 2025-07-12, CVX 48 at 12 months then CVX 49 at 8 weeks − 5 days. FITS wants ED/RD 2026-10-27 (12-month 2-dose Dose 2 +8 weeks; the 49 is TOO_SOON). Engine published PRP-OMP 2026-09-29. PRP-OMP Dose 1 list is `[NOT_VALID/cvx48, VALID/cvx49]`; 12-month series already had 10-27 at score +2. Reconstructing Table 8-9 after the list walk: PRP-OMP −2 product/all-valid, +3 completable, 0 most-valid tie, −2 closest (two remaining vs one), +1 finish = 0; 12-month −2 +3 +0 +2 −1 = +2. SELECTBEST-2 picks +2.

Live `HepB-2018-0020`: CVX 08 then Heplisav-B CVX 189. FITS wants 2026-10-27 (3-dose Dose 3). Heplisav is the same `[Not Valid, Valid]` product-path shape. Live `MEN-B-2024-0081`: Bexsero CVX 163 then Trumenba CVX 162.

## Fix (scoped)

`PatientSeries.hasAllValidAdministeredDoses` walks `TargetDose.getEvaluationList()`, not `getEvaluation()`. A later Valid evaluation does not erase an earlier Not Valid one. Remaining unevaluated target doses still do not count against.

Do not special-case Hib/HepB/MenB uids. CALCDT-5 is unchanged. `2013-0308` stays out (4-dose complete via skip at exactly 12 months vs FITS NOT_COMPLETE with null dates).

## Verification

Unit: `InProcessPatientSeriesTest` 39/39 (new: an earlier Not Valid evaluation on a satisfied target still fails SELECTB-2). Engine suite 835 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T140951` vs SPEC-4.6-0066 run `2026-09-16T133824`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4515 | **4529** | **+14** |
| HIB | 505 | **510** | **+5** |
| HepB | 372 | **377** | **+5** |
| MEN B | 88 | **92** | **+4** |
| COVID-19 | 178 | 178 | 0 |

- FAIL→PASS: all copies of HIB `2013-0348` (5), HepB `2018-0020` (5), MEN B `2024-0081` (4).
- PASS→FAIL: none. COVID parked; allowlist not regenerated (same 6 CVX 213 assertion failures).
- HIB remaining: 15 fails / 3 uids (`2013-0299`, `2013-0357` CALCDT-5; `2013-0308` skip-at-12-months complete vs FITS not complete).
- HepB remaining: 10 fails / 2 uids (`2013-0220`, `2013-0238` CALCDT-5).
- MEN B remaining: none.
