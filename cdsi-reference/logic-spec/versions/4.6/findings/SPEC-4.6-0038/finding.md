# SPEC-4.6-0038: The shared decision-table engine ran every matching rule column instead of stopping at the first (highest-priority) one

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Unit 6.10's last three reds shared a single root cause, and it wasn't local to 6.10 at all. Table 6-31's six rules are priority-ordered: Rule 2 ("Extraneous") is meant to take precedence over Rules 4, 5 and 6 (three separate "Not Valid" reasons) whenever more than one rule's conditions are simultaneously satisfied - a target dose that is both extraneous *and*, say, impacted by a live virus conflict is still fundamentally extraneous, not "not valid."

The shared `LogicTable.evaluate()` - used by every decision table across the entire engine, not just Table 6-31 - iterated every outcome column and called `perform()` on **every** column whose conditions matched, not just the first. Rules 4, 5 and 6 all have a dash (`ANY`) on the age condition, so whenever Rule 2 also matched (age = Extraneous) *and* one of the later rules' own status-cause condition also matched, both outcomes ran in the same pass - Rule 2's `EXTRANEOUS` first, then the later rule's `NOT_VALID` immediately overwriting it.

This was reachable, not theoretical: `EvaluateAge.process` routes unconditionally on to 6.5 regardless of what Table 6-4 decided, so a too-old (`Extraneous`) dose still passes through 6.5-6.9 and can pick up any of their status-cause markers. On the bundled Supporting Data release, 91 of the 92 series doses that can answer `Extraneous` at Table 6-4 also define at least one `allowableVaccine` that could add the "Vaccine" marker Rule 6 reads. status.yaml's own prior notes had already root-caused this exactly, including that the shared class's own commented-out `validColumnCount != 1` check independently documents the original design intent: only one column was ever meant to validate.

## Fix

`LogicTable.evaluate()` now `break`s immediately after the first matching column's `perform()` runs, instead of continuing to check and run every remaining column. This restores standard priority-ordered decision-table semantics - first match wins, columns read left to right in rule-number order - for every `LogicTable` in the engine, matching how every Table 6-31-derived test already read the specification ("Rule 2 comes before Rule 4/5/6").

This is a shared-class change with engine-wide blast radius, so it was verified with extra weight on the full regression suite rather than just unit 6.10's own tests.

## Verification

- Unit 6.10's own suite (`SatisfyTargetDoseTest`): 25/25 green (up from 22/25).
- Full `cdsi-engine` suite (all 783 tests): sorted diff confirms exactly those 3 tests flipped and nothing else - no other decision table in the engine currently depends on more than one column validating simultaneously.
- Full FITS run: 3632 passed (up from 3627) - 5 real cases improved, all 5 test-plan snapshots of `HIB-2013-0356` ("#2 Pentacel at 6 yrs 11.5 mo." - a too-old Hib dose that also triggers another status-cause marker, exactly the overlap this defect predicted), 0 regressions. The first genuinely broad, cross-cutting fix of this Role B pass to move real FITS cases since SPEC-4.6-0032's series-group fix.

## Affected

- Spec sections: 6.10 (page 55, Table 6-31) - the surfacing unit, but the fix is engine-wide
- Code locations: `LogicTable.java` (`evaluate()`)
- FITS cases: 5 real cases improved (HIB-2013-0356, all 5 test-plan snapshots)
