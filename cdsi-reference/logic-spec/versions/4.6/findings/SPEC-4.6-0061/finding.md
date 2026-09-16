# SPEC-4.6-0061: MULTIANTVG-1 last-administered floor never saw production doses

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

DTAP FITS after Polio close-out still had 138 fails. Two due-now clusters were the same 9.3 hole: a priority contained forecast pulled the group earliest date into the past (Pertussis Dose 5 min age 4, or Dose 11 min age 11) even though the patient had already received a DTaP-group shot on a later day.

SPEC-4.6-0049 already wrote the last-administered floor. Unit 9.3 covered it by stuffing the administered vaccine onto `VaccineGroup.vaccineList`. Production never does that.

## Evidence

Table 9-5 MULTIANTVG-1: if any contained forecast is a priority patient series forecast, the group earliest date is the later of (the earliest contained earliest date; the latest date administered of any vaccine dose administered belonging to the vaccine group).

Schedule Supporting Data `vaccineGroups` for `DTaP/Tdap/Td` is a name plus `administerFullVaccineGroup` `No`. `vaccineGroupToAntigenMap` lists Diphtheria, Pertussis, Tetanus. There is no per-group vaccine catalog. `DataModelLoader.readVaccineGroups` leaves `vaccineList` empty. Live dump: `catalogVaccines=0` on every DTaP group forecast.

`belongsToVaccineGroup` compared the administered `Vaccine` to that empty list, so `latestDateAdministeredInVaccineGroup()` was always null.

Oracle `DTAP-2024-0058` (DT as 5th dose): four DTaP CVX 20, then DT CVX 28 on `2019-12-26`. Pertussis Dose 5 is still due (DT has no pertussis) at min age 4 (`2018-06-05`), with `intervalPriority` override. FITS wants `2019-12-26`. Engine had `2018-06-05`.

Oracle `DTAP-2013-0035` (Td, no Tdap, adolescent): five DTaP then Td CVX 09 on `2026-09-01`. Pertussis Dose 11 override min age 11 is `2024-12-24`. FITS wants due now `2026-09-01`. Engine had `2024-12-24`.

## Fix (scoped)

A dose belongs to the current vaccine group when its CVX-to-antigen associations overlap the group's antigens. Catalog identity matching stays for isolated fixtures that fill `vaccineList`. DT 28 and Td 09 belong because they contain Diphtheria and Tetanus.

Out of scope: age-7 catch-up 4-week vs 6-month (Issue #65 / SPEC-4.6-0019); CALCDT-5 Nov 30 vs Dec 1; adult catch-up 6 months from last Td vs last Tdap (`2020-0005`/`2020-0006` — the floor is the last shot's date, not last shot plus interval).

## Verification

Unit: `MultipleAntigenVaccineGroupTest` 39/39 (was 37). New tests: antigen-overlap membership with an empty catalog; a HepB shot must not floor an MMR priority earliest date. Engine suite 823 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T103931` vs SPEC-4.6-0060 run `2026-09-16T021103`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4248 | **4257** | **+9** |
| DTAP | 754 | **763** | **+9** |
| POL | 623 | 623 | 0 |
| COVID-19 | 139 | 139 | 0 |
| MMR | 213 | 213 | 0 |

- FAIL→PASS: all five copies of `DTAP-2013-0035` and all four copies of `DTAP-2024-0058`.
- Newly failing: none. COVID unchanged (139 PASS / 202 FAIL). Allowlist not regenerated (still 6 parked CVX 213 assertion failures).
- DTAP remaining: 129 fails / 26 unique uids. Largest leftover clusters are still Issue #65 age-7 catch-up and CALCDT-5, not this floor.
