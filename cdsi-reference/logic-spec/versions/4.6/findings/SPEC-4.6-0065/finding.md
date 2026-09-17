# SPEC-4.6-0065: live-virus conflict only saw same-antigen history

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

VAR leftovers after HPV's 8.4 drop (SPEC-4.6-0064) were 35 fails / 7 uids, all NOT_COMPLETE date diffs. Three shapes, one cause: live-virus rules only looked at the antigen-filtered selected AAR list (6.7) or at the current antigen's previous AAR (7.5). MMR and LAIV never conflicted with a Varicella evaluation, and a Varicella Dose 1 forecast never floored at a live-virus +28d end.

## Evidence

Section 6.7 validates a dose "against previous administered vaccines." CALCDTCONFLICT-1/2 and CONFLICT-3 are product-type pairings from `<liveVirusConflicts>`, not same-antigen pairings. 4.4's `setupSelectedAntigenAdministeredRecordList` keeps an AAR only when `aar.getAntigen() == dataModel.getAntigen()`, so previous MMR is gone by the time 6.7 runs for Varicella.

Table 7-12 CALCDTLIVE-4 is the latest CALCDTCONFLICT-2 end date; FORECASTDTCAN-1 includes it. The previous `findLatestConflictEndIntervalDate` required both `getAntigenAdministeredRecord()` and `getPreviousAntigenAdministeredRecord()` on the antigen being forecast, so a first Varicella target after MMR never received a conflict-end floor.

Bundled 4.65: MMR(03)→VAR(21) 1d/28d/28d; LAIV4(149)→VAR 28/28; VAR→VAR minEnd 24d end 28d; MMRV(94)→VAR 28/28.

Live clusters (assessment 2026-09-01 unless noted):

- 0803/0817/0818: too-young Dose 1. Engine used 12-month min age. FITS wants the Not Valid shot + conflictEnd 28d (not min 24d).
- 0815/0825/0832: MMR or LAIV 27d before VZ/MMRV. Engine counted Dose 1 Valid and forecast Dose 2. FITS invalidates the VAR antigen and wants MMR/LAIV+28d.
- 0840: MMR only, no VZ. Engine 12-month min age (already past). FITS wants MMR+28d.

## Fix (scoped)

6.7 iterates `antigenAdministeredRecordList` for doses on or before the current date, skipping the current VDA (and its multi-antigen AAR twins). 7.5 CALCDTLIVE-4 takes the latest CALCDTCONFLICT-2 end over history doses whose previous type conflicts with a preferable vaccine type of the target being forecast: Valid or unevaluated → `minConflictEndInterval`; otherwise `conflictEndInterval`.

Do not invent a 28-day from-previous interval on VAR Dose 1. CALCDT-5 is unchanged. 7.4 still omits CALCDTLIVE-4 from its own FORECASTDTCAN-1; FITS dates come from 7.5.

## Verification

Unit: `EvaluateVaccineConflictTest` 26/26 (new: previous MMR not on the Varicella selected list still conflicts). `GenerateForecastDatesAndRecommendedVaccinesTest` 44/44 (new: Valid MMR floors a Varicella forecast at +28d; Not Valid previous takes conflictEnd 28d not min 24d). Engine suite 832 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T124850` vs SPEC-4.6-0064 run `2026-09-16T121132`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4455 | **4505** | **+50** |
| VAR | 183 | **218** | **+35** |
| MMR | 213 | **233** | **+20** |
| ZOSTER | 98 | 93 | −5 |
| COVID-19 | 178 | 178 | 0 |

- FAIL→PASS: all five copies of VAR `2013-0803`, `0815`, `0817`, `0818`, `0825`, `0832`, `0840`; MMR `2013-0540`, `0547`, `0549`, `0563`.
- PASS→FAIL: ZOSTER `2015-0019` (5 copies). Title is "MMR to Zoster interval 28 - 1 days." CVX 121 at 27d after MMR is inside the bundled MMR→Zoster live window. FITS still treats 121 as Valid and wants recombinant Dose 2 at +8 weeks. CONFLICT-3 says Not Valid; recombinant Dose 1 is then due from the last shot. Explained leftover; drop those five allowlist rows. COVID parked; allowlist not fully regenerated (same 6 CVX 213 assertion failures).
- VAR remaining: none (218/218).
- MMR remaining: 15 fails / 3 uids (`0528`, `0530`, `0539`) — single-antigen measles/mumps/rubella mix, not this live-virus scan.
