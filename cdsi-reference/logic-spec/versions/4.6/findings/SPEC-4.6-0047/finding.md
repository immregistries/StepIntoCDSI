# SPEC-4.6-0047: Chapter 9 table labels and 9.1 reasoning-log wiring were stale

**Status:** resolved
**Category:** IMPLEMENTATION_MISMATCH

Ported onto the Chapter 8 rewrite (`develop` at `d6aff99`) from Codex Chapter 9 commit 1 (`beea5f1`). That commit originally used SPEC-4.6-0045, which this branch already assigned to the Chapter 8 scoring foundation.

## Evidence

Unit 9.1 had one remaining red after SPEC-4.6-0042's collateral fixes:

- `ApplyGeneralVaccineGroupRulesTest.theDecisionTableCitesTableNineTwo` expected Table 9-2, but the implementation still displayed `TABLE 7 - 2 WHAT IS THE VACCINE GROUP TYPE?`.

The same Chapter 9 review found:

- `SingleAntigenVaccineGroup` registered its 0x0 prose-rule placeholder as `Table ?-?` instead of Table 9-3.
- `MultipleAntigenVaccineGroup` labeled the multiple-antigen status cascade as Table 9-3; the specification numbers that decision table as Table 9-4.
- `ApplyGeneralVaccineGroupRules` did not call `setLogicStepSink` on its `LogicTable`.
- The 9.1 test pinned that empty-step-log behavior and had to move with the fix.

## Fix

Updated the three Chapter 9 table labels to cite Table 9-2, Table 9-3, and Table 9-4, wired 9.1's logic table to the step sink, changed the 9.1 observed-behavior test to assert the classification in the step log, and removed the inert commented-out placeholder decision-table scaffold from 9.2.

Traceability and viewer/logging only. FORECASTVG aggregation is SPEC-4.6-0048 (9.2) and SPEC-4.6-0049 (9.3).

## Affected

- Spec sections: 9.1, 9.2, 9.3
- Code locations: `ApplyGeneralVaccineGroupRules.java`, `SingleAntigenVaccineGroup.java`, `MultipleAntigenVaccineGroup.java`
- FITS cases: none expected
