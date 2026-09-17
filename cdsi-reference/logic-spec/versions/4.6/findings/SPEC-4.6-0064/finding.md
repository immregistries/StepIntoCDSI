# SPEC-4.6-0064: 8.4 left in-process series in consideration at score 0

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

HPV leftovers after CONDSKIP-1 (SPEC-4.6-0063) were 98 fails / 22 uids. The largest cluster (55 copies / 11 uids) was COMPLETE vs engine NOT_COMPLETE: three HPV doses at about age 9 with a 4-week then 12-week gap. FITS treated the series as complete. The engine's 3-dose start-under-15 series was Complete on the stepper, but Chapter 8 selected the default 2-dose series (Dose 2 too soon for the 5-month 2-dose interval).

## Evidence

Table 8-5 Rule 1: "All complete patient series in the series group should be scored. Apply the complete patient series scoring business rules to these scorable patient series only. In-process patient series and patient series with 0 valid doses are not scored and dropped from consideration."

Table 8-7 awards +1 / 0 / -1 only to complete series for "has the most valid doses" (SELECTB-19). A tie at the maximum scores 0.

SELECTBEST-2: highest score, then best-ranked `seriesPreference`.

HPV 4.65: 2-dose series is default, preference 1; 3-dose start under 15 is preference 2. Live `HPV-2013-0409`: DOB 2014-09-01; CVX 62 on 2023-08-28, 2023-09-25, 2023-12-18. FITS wants COMPLETE. Engine: 2-dose Dose 2 NOT_VALID/TOO_SOON; female and male 3-dose-under-15 both COMPLETE with 3 valid doses, tied at score 0. 8.4 did not drop the in-process 2-dose series. 8.7 picked preference 1.

## Fix (scoped)

After Table 8-7, 8.4 removes every non-COMPLETE series from `scorablePatientSeriesList`. 8.7 then selects among complete series only.

Series-level `requiredGender` is still unparsed (SPEC-4.6-0029). Male 3-dose twins still complete and still tie at 0; either is COMPLETE, which is enough for this cluster. Gender vocabulary normalization is a later round. CALCDT-5 is unchanged.

## Verification

Unit: `CompletePatientSeriesTest` 17/17 (was 15). Two new tests: in-process series are dropped from the scorable list; an in-process default at score 0 cannot beat a complete series on seriesPreference. Engine suite 829 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T121132` vs SPEC-4.6-0063 run `2026-09-16T114454`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4400 | **4455** | **+55** |
| HPV | 428 | **483** | **+55** |
| DTAP | 867 | 867 | 0 |
| POL | 623 | 623 | 0 |
| COVID-19 | 178 | 178 | 0 |

- FAIL→PASS: all five copies of HPV `2013-0409`, `0413`, `0444`, `0445`, `0450`, `0459`, `0465`, `0466`, `0472`, `0473`, `0475`.
- PASS→FAIL: none. COVID parked; allowlist not regenerated (same 6 CVX 213 assertion failures).
- HPV remaining: 43 fails / 11 uids. Date cluster `0423`/`0424`/`0425`/`0426`/`0430`/`0483` (30). Cervarix `0437`/`0438` engine COMPLETE vs FITS NOT_COMPLETE (10). Historical AGED_OUT `0467`/`0480`/`0481` (3).
