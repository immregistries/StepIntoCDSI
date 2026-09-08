# SPEC-4.6-0020: Chapter 8's series-group loop, implemented - merged despite a documented, project-owner-approved FITS regression

**Status:** confirmed (merged - see "Explicit project-owner decision" below)
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Chapter 8's own overview: "Process steps 8.1 through 8.7 are repeated **for each series group** to identify one prioritized patient series per series group. Process step 8.8 is then used to determine which prioritized patient series are selected as a best patient series." That repetition did not exist anywhere in the engine - see `cross-cutting-notes.md`'s 2026-09-05 "Chapter 8 has no series group" entry, confirmed independently from every one of 8.1-8.8's own Role A sides. Confirmed again here by direct code reading:

- `SelectBestPatientSeries` (4.5) looped only over antigens; no series-group concept existed anywhere in `DataModel` (grep: zero hits for "seriesGroup").
- 8.1 read `patientSeriesStepper().getList()` (5.1's fully unfiltered, all-antigen list).
- 8.4 and 8.7 read `selectedPatientSeriesList` (4.5's pre-8.1 list - the wrong pipeline stage).
- 8.5 and 8.6 read the unfiltered stepper (wrong stage *and* wrong scope).
- 8.8's outer constructor loop correctly filtered `prioritizedPatientSeriesList` by antigen, but its two "equivalent series group" conditions (Table 8-14) read a stale, unrelated field and implemented no equivalence check - `<equivalentSeriesGroups>` (present on 54 of the bundled release's 143 series) was parsed by nothing and had nowhere to be stored.
- Two further, independent bugs compounded this: 8.5/8.6's "sticky boolean" defect (a per-series flag declared outside the scoring loop, never reset per iteration) and 8.6's `numOfEarliestDates` tie-counter (reset to 0 on a new earliest date without counting the series that triggered the reset).

## Interpretation

### The fix

A new `SELECT_NEXT_SERIES_GROUP` `LogicStepType`/class steps through the antigen's distinct series groups (a `Stepper<String>` on `DataModel`, populated once per antigen by 4.5 from `antigenSeriesSelectedList`), rebuilding `selectedPatientSeriesList` scoped to (antigen, group) before each pass through 8.1-8.7. Every scoring-phase exit - 8.2's five shortcut outcomes and 8.7's normal path - now dispatches there instead of straight to 8.8, so 8.8 only ever runs once an antigen's groups are exhausted.

List-source swaps: 8.1 now reads `selectedPatientSeriesList` (not the stepper); 8.4/8.5/8.6/8.7 now read `scorablePatientSeriesList` (not the pre-8.1 list or the stepper).

`AntigenSeries.equivalentSeriesGroups` (a `List<String>`) is now parsed from the `<series>` element's own `<equivalentSeriesGroups>` child (a sibling of `<seriesType>`, **not** nested under `<selectSeries>` where `seriesGroup` lives). 8.8's two "equivalent series group" conditions now scan `prioritizedPatientSeriesList` - the list 8.8's constructor already iterates - checking each *other* entry's series group against the entry currently being judged.

Independent bug fixes made alongside (not caused by scoping, but would remain wrong even after it's fixed): 8.5's and 8.6's sticky-boolean declarations moved inside their per-series loops; 8.6's `numOfEarliestDates` now counts the series that triggers a new-earliest reset instead of discarding it.

A useful, testable consequence: **8.2 and 8.3 needed zero code changes.** Once `scorablePatientSeriesList` is correctly pre-scoped by 8.1, their own scoping asymmetries (8.2's condition 0 missing an antigen filter, 8.3 filtering nowhere at all) become unreachable dead code rather than live defects - exactly what `cross-cutting-notes.md` predicted for 8.3 ("resolvable only by the chapter-wide decision... has nothing to make self-consistent").

### Test verification

Every affected unit's own JUnit suite was checked individually, then all nine together, then the full `cdsi-engine` suite - at each stage every remaining red reconciled exactly against an already-documented, pre-existing, unrelated defect (confirmed by cross-referencing this round's own failure output against each unit's `status.yaml` red-test enumeration):

| Unit | Reds this entry closes | Reds remaining (all pre-existing, unrelated, independently documented) |
| --- | --- | --- |
| 4.5 | (stays fully green; retargeted to the new class's responsibility) | 0 |
| 8.1 | 4 of 8 | 4 (null-status guard, unread maxAgeToStart date check, unimplemented zero-valid-dose bullet, a documented SPECIFICATION_AMBIGUITY) |
| 8.2 | 2 of 5 | 3 (SELECTB-7/16 counting defects) |
| 8.3 | 2 of 4 | 2 |
| 8.4 | 0 of 8 directly (2 cross-cutting tests go green; the other 8 reds are SPEC-4.6-0006's unrelated "break too early" bug) | 8 |
| 8.5 | 5 of 18 (2 sticky-boolean, 3 cross-cutting) | 13 |
| 8.6 | counter-reset portion + 3 cross-cutting | 14 (reference-equality tie bug, product-row sign inversion, two undocumented scoring conditions) |
| 8.7 | 3 of 5 | 2 (SELECTBEST-1 running-total bug, "" reference-equality bug) |
| 8.8 | 6 of 6 (fully closed) | 0 |

A new `SelectNextSeriesGroupTest` (8 tests, all green) directly covers the new class.

Full `cdsi-engine` suite: 771 -> 776 tests, 167 -> 139 failures (**-28**), 1 error unchanged (the one pre-existing `GenerateForecastDatesAndRecommendedVaccinesTest` NullPointerException, confirmed unrelated to this change).

### FITS verification

`mvn -pl cdsi-engine install -DskipTests` was run before the post-fix FITS run. Reference set `acip-4.6-sd-4.65-fits-222cc1c7`, 4896 fixtures, strict case-by-case comparison (caseId + status + expectedHash + actualHash, sorted).

| | Before | After |
| --- | --- | --- |
| Passed | 3378 | 3253 |
| Failed assertions | 1517 | 1642 |
| Execution errors | 1 | 1 |

**175 cases regress** (PCV 143, RSV 20, MEN 12) and **50 improve** (HepA 15, POL 15, MMR 10, PCV 8, MEN 2) - a net **-125**.

### Root cause of the regression - traced, not guessed

Direct engine instrumentation of one regressed case, `5eeca0522cc4517a96b0f417-PCV-2013-0575` (a patient under 50 receiving a forecast of **2076-09-01** for "Pneumococcal 50+ 1-dose PCV series"):

This is **not a defect in this fix**. It is this fix correctly and consistently applying Table 8-2's *own already-flagged* SPECIFICATION_AMBIGUITY - `PreFilterPatientSeries`'s undocumented "no default series fallback" (added because SELECTSCORE-2's own bullets never say how a group's default series becomes scorable; already pinned as characterization, not conformance, in this unit's `status.yaml` notes) - to one series group at a time, exactly the way it already correctly scopes SELECTSCORE-2's own zero-valid-dose bullet ("the number of valid doses is 0 for each relevant patient series **in the series group**" - already green, pre-existing).

"Pneumococcal 50+" sits alone in its own series group (group 3), declares `<minAgeToStart>50 years</minAgeToStart>`, and is itself the group's `defaultSeries`. Nothing in `cdsi-engine` reads `SelectPatientSeries.getMinAgeToStart()` or `getMaxAgeToStart()` at all (confirmed by grep - both are parsed or declared but have zero call sites). Scoped to its own group, this series' within-group valid-dose count is trivially zero, so the fallback admits it as the group's default; with exactly one candidate in its group, Table 8-3 Rule 2 needs no scoring and it proceeds straight through to `bestPatientSeriesList` - regardless of the patient's actual age.

Recorded separately, in full, as **SPEC-4.6-0021** - a second, upstream defect: no business rule anywhere (in Table 8-2 or otherwise) currently excludes an age-inappropriate series from ever becoming a relevant, candidate, or default series in the first place. Before this fix, this didn't matter, because Chapter 8's single antigen-wide competition let the real winner ("Pneumococcal 4-dose series," which has genuine administered doses) outscore the age-inappropriate one in open comparison; the whole point of this fix is that such cross-group comparisons should never have been happening.

## Explicit project-owner decision

Presented to the project owner as a genuine choice - hold this fix pending SPEC-4.6-0021, keep investigating a same-session fix for SPEC-4.6-0021, or merge despite the regression - and the project owner chose to **merge now, accepting the regression**, because:

1. This fix is independently JUnit-verified correct against all nine affected units' own conformance tests, closing 22 previously-red tests.
2. The regression's cause is fully traced to a separate, pre-existing defect (SPEC-4.6-0021), not an error introduced by this fix.
3. The 50-case improvement this fix also produces (including the HepA equivalent-series-group blending SPEC-4.6-0018 had already found correct but could not merge because *this* defect blocked it) represents real, spec-grounded forward progress, and is expected to combine cleanly with SPEC-4.6-0021's eventual fix.

This is recorded here as a **deliberate, informed exception** to the standing "FITS moves forward, not backward" rule this whole campaign has otherwise followed - for CDC/CDSi review, not as an oversight.

## Affected

- Spec sections: 4.5 (page 39), 8.1-8.8 (pages 66-101, Chapter 8 overview + Tables 8-3 through 8-14)
- Code locations: `SelectBestPatientSeries`, `SelectNextSeriesGroup` (new), `PreFilterPatientSeries`, `IdentifyOnePrioritizedPatientSeries`, `ClassifyScorablePatientSeries` (no code change - closes for free), `CompletePatientSeries`, `InProcessPatientSeries`, `NoValidDoses`, `SelectPrioritizedPatientSeries`, `DetermineBestPatientSeries`, `AntigenSeries`, `Stepper`, `DataModel`, `DataModelLoader`, `LogicStepType`, `LogicStepFactory`
- FITS cases: 175 regress, 50 improve, net -125 - see SPEC-4.6-0021 for the blocking defect
