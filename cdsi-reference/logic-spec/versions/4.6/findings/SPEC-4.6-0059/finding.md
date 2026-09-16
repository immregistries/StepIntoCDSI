# SPEC-4.6-0059: CALCDTSKIP-5 from the immediate previous administered dose

**Status:** open  
**Category:** IMPLEMENTATION_MISMATCH

## Why

Polio Dose 3 Evaluation skip is an OR of (age ≥ 4 years) and (age ≥ 4 years − 4 days AND interval ≥ 6 months − 4 days). `POL-2013-0639` sits in the grace window: the third IPV is exactly 4 years − 4 days after DOB and 6 months − 4 days after dose 2, so Dose 3 must skip and the same shot must evaluate Dose 4.

Isolated 6.2 on the loaded Dose 3 skip object did skip when a previous AAR was supplied. The full pipeline left Dose 3 `SATISFIED` / `VALID` and forecast Dose 4 at last-shot + 6 months (`2027-03-01`) instead of `COMPLETE`.

## Evidence

Table 6-5 CALCDTSKIP-5: the conditional skip interval date is the date administered of the **immediate previous vaccine dose administered** plus the condition's Interval.

`DateRules.CALCDTSKIP_5` read `getAntigenAdministeredRecordThatSatisfiedPreviousTargetDose()`. That setter is only called from isolated 6.2 / 7.1 / 7.6 unit tests. Production 4.4 stores the previous shot as `previousAntigenAdministeredRecord` when it advances past a consumed AAR, and never writes the "satisfied previous target" field.

With the interval date null, Table 6-8's second condition is No (no assumed value), the AND grace set fails, the 4-year set is also unmet four days before the fourth birthday, Table 6-11 does not skip, and 6.3 onward treats the shot as a valid Dose 3.

Live `689dc945e4b05decee50df54-POL-2013-0639`:

- DOB `2022-09-05`; IPV `2023-09-05`, `2026-02-05`, `2026-09-01`; assessment `2026-09-01`.
- 4 years − 4 days from DOB = `2026-09-01`. 4 years = `2026-09-05`. 6 months − 4 days from dose 2 = `2026-08-01`.
- Expected `COMPLETE`. Engine `NOT_COMPLETE`, ED/RD `2027-03-01`.

## Fix (scoped)

CALCDTSKIP-5 uses `previousAntigenAdministeredRecord` first, then the old isolated-test field as a fallback so existing 6.2/7.1/7.6 fixtures still name the same date.

Out of scope: POL RD off-by-1 vs CALCDT-5 (`0631`/`0681`/`0721`); OPV `2024-0071`.

## Verification

Unit: `EvaluateConditionalSkipForEvaluationTest.calcdtskipFiveUsesThePreviousAntigenAdministeredRecordFourFourStores`, `EvaluateConditionalSkipForEvaluationTest.polioDoseThreeSkipAtFourYearsMinusFourDaysAndSixMonthsMinusFourDays`. Engine suite 818 tests, same two known reds (8.1/8.7). `logic-spec validate --version 4.6` valid.

FITS run `2026-09-16T014058` vs SPEC-4.6-0058 run `2026-09-16T005852`:

| | Before | After | Delta |
|---|---:|---:|---:|
| Combined | 4229 | **4239** | **+10** |
| POL | 614 | **619** | **+5** |
| HIB | 500 | **505** | **+5** |

- Oracle `POL-2013-0639` PASS (all five copies): series `COMPLETE`.
- Same-cluster FAIL→PASS: `HIB-2013-0324` (Hib Dose 3 skip at 12 months − 4 days AND 8 weeks − 4 days from the previous dose — the same CALCDTSKIP-5 hole).
- Newly failing: none. COVID unchanged (139 PASS / 202 FAIL). Allowlist not regenerated (still 6 parked CVX 213 assertion failures).
- POL remaining: 19 fails / 4 unique uids (`0631`/`0681`/`0721` RD off-by-1 vs CALCDT-5, `2024-0071` OPV).

