# SPEC-4.6-0036: Mislabeled attribute and a constructor side effect that could double-fire, closing a cross-step landmine 6.6 flagged

**Status:** confirmed, merged
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Unit 6.7's last three reds - both deliberately left open by [SPEC-4.6-0015](../SPEC-4.6-0015/finding.md)'s earlier round as a separate, unrelated defect from the loop/interval bugs it fixed:

**1. Mislabeled attribute.** Table 6-23 row 2 published label `"Supporting Data (Live Virus Conflict)"` instead of `"Vaccine dose administered"` - a copy-paste artifact from the neighboring `caPreviousVaccineType` attribute (which genuinely is Supporting-Data-sourced, added per candidate previous dose). The row's *value* was already correct; only the label was wrong.

**2. Wrong-outcome side effect.** `EvaluateVaccineConflict`'s first internal table (labelled "Table 4-20") has three outcome columns: the current type isn't an impacted type (correctly does nothing but log), the current type is impacted with a previous dose to check (evaluates further), and - the buggy one - no vaccine dose administered on or before the current one, i.e. nothing to possibly conflict with. That third outcome appended `"VirusConflict"` to the target dose's shared status cause instead of doing nothing, exactly the same class of mistake its "not impacted" sibling avoids.

**3. Doubled by a redundant re-evaluation.** The constructor eagerly calls `.evaluate()` on its own tables to determine how many previous doses to loop over before it can even finish constructing - explicitly called out in this step package's own test javadoc as unusual ("every other chapter-6 step's constructor only reads the DataModel... this one evaluates a decision table straight away"). `process()` then unconditionally calls `evaluateLogicTables()` again on those same table instances. With gap 2's side effect present, a full `process()` call fired it twice - once at construction, once in `process()` - producing a doubled `"VirusConflictVirusConflict"`.

Cross-step consequence, already flagged by unit 6.6's own status.yaml notes: 6.6's `EvaluateAllowableIntervalTest` fixtures leave `antigenAdministeredRecordList` empty (unlike a real pipeline run, which always has the current dose in that list) - so every one of 6.6's runs that reached `next()` was silently appending a spurious `"VirusConflict"` onto the status cause through 6.7's constructor. Invisible to 6.6's own tests (they only assert on their own "Interval" marker), but a latent hazard for anything built on 6.6 in isolation.

## Fix

- `caCurrentVaccineType` is now constructed with the label `"Vaccine dose administered"` (matching `caDateAdministered`, Table 6-23's other row) instead of the Supporting-Data label.
- The third outcome column no longer touches the status cause at all - it now only logs, matching its "not impacted" sibling. With no side effect left, the redundant `evaluateLogicTables()` call in `process()` is harmless (idempotent logging only) and was left untouched, since removing it is outside this defect's bounds.

## Verification

- Unit 6.7's own suite (`EvaluateVaccineConflictTest`): 25/25 green (up from 22/25).
- Full `cdsi-engine` suite: sorted diff confirms exactly those 3 tests flipped and nothing else.
- Full FITS run: 3627 passed both before and after, case-by-case diffed to 0 changed cases - a genuine correctness fix (including closing the 6.6 cross-step landmine) the current fixture set doesn't happen to exercise, the same category as SPEC-4.6-0026, SPEC-4.6-0030, SPEC-4.6-0034, and SPEC-4.6-0035.

## Affected

- Spec sections: 6.7 (page 51, Table 6-23)
- Code locations: `EvaluateVaccineConflict.java` (constructor, Table "4-20" outcome 2)
- FITS cases: none currently exercise a target dose with zero prior administered doses at all against a live-virus-conflict-impacted vaccine type
