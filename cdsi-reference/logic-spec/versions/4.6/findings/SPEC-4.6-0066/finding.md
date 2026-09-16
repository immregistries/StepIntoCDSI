# SPEC-4.6-0066: blank last-dose maxAge is no upper bound, not "not completable"

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

HepB leftovers after live-virus history-wide conflict (SPEC-4.6-0065) were 25 fails / 5 current-plan uids. Three of those (0211, 0212, 2018-0020) already had the FITS-correct dates on a worse-scored series; Chapter 8 published a different in-process series instead.

## Evidence

SELECTB-3: a series is completable when its forecast finish date is less than the last target dose's maximum age date. Table 8-9 awards +3 / −3; Table 8-11 awards +1 / −1. SELECTB-11 only lets completable series compete for finish-earliest.

FORECASTDT-4: the forecast latest date is blank when there is no maximum age date. Bundled 4.65 HepB 3-dose and 4-dose last doses have empty `<maxAge/>`; adolescent 2-dose Dose 2 has `maxAge` 16 years.

`isCompletable` required a non-null maximum-age date, so every series with no upper bound scored "not true." `hasMaximumAge` treated a non-null empty `TimePeriod` (what `DataModelLoader` builds from `<maxAge/>`) as present, then `getDateFrom` returned null.

Live `HepB-2013-0211`: two CVX 43 at 11 years, 4 months − 5 days. FITS wants ED/RD 2026-10-27 (3-dose Dose 3 +8 weeks). Engine published adolescent 2-dose 2027-01-01. Stepper: 3-dose valid=2 score=−6 forecast 10-27; adolescent valid=1 score=0 TOO_SOON forecast 2027-01-01; 4-dose valid=2 score=−8 ED=09-01. Reconstructing Table 8-9: 3-dose −2 product, −3 completable, 0 most-valid tie, 0 closest tie, −1 finish = −6; adolescent −2 +3 −2 +0 +1 = 0 because it was the only completable series and won finish-earliest. SELECTBEST-2 picked 0 over −6.

Live `HepB-2013-0212`: 43, 43, 08. FITS wants 2026-08-26 (4-dose Dose 4). Engine published adolescent 2026-12-21; 4-dose already had 08-26 at score −4.

## Fix (scoped)

`PatientSeriesScoring.isCompletable`: a finish date with no maximum-age date is completable; a missing finish date is not. `PatientSeries.hasMaximumAge` requires `TimePeriod.isValued()`, so empty `<maxAge/>` is not a cap.

Do not special-case HepB uids. CALCDT-5 is unchanged. 2013-0220 / 2013-0238 stay out (recommended-date vs CALCDT-5). 2018-0020 still selects Heplisav-B 2-dose after this scoring change (product + completable outscores the 3-dose default) and is leftover.

## Verification

Unit: `InProcessPatientSeriesTest` 38/38 (new: blank last-dose maxAge awards +3). `NoValidDosesCompletableTest` 6/6 (new: no maximum age scores +1). Engine suite 834 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T133824` vs SPEC-4.6-0065 run `2026-09-16T124850`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4505 | **4515** | **+10** |
| HepB | 362 | **372** | **+10** |
| COVID-19 | 178 | 178 | 0 |

- FAIL→PASS: all five copies of HepB `2013-0211`, `2013-0212`.
- PASS→FAIL: none. COVID parked; allowlist not regenerated (same 6 CVX 213 assertion failures).
- HepB remaining: 15 fails / 3 uids (`2018-0020` Heplisav product path, `2013-0220`, `2013-0238` CALCDT-5).
