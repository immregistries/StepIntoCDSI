# SPEC-4.6-0007: NoValidDoses's "is completable" score never decrements, and two undocumented conditions run

**Status:** confirmed (the always-increments defect's fix - see "Fix merged" - was reviewed and merged by the project owner on 2026-09-03; the two undocumented scoring conditions remain an open question, see Interpretation)
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

`NoValidDoses.evaluate_ACandidatePatientSeriesIsCompletable()`'s `if`/`else` both call `patientSeries.incPatientScoreSeries()` - there is no code path that ever decrements for this condition, contradicting Table 8-11's documented +1/-1 split. This looks like a copy-paste slip: the `else` branch should very likely call `descPatientScoreSeries()`, matching the pattern every other condition in this scoring family uses.

Separately: two scoring conditions run in this class that aren't present in Table 8-11 at all - a gender-match bonus, and an exceeded-maximum-age penalty.

(A related, separate bug in this same class - a `==`/`!=` Date reference-equality error breaking tie-detection in `evaluate_AScorablePatientSeriesCanStartEarliest()` - is tracked as [SPEC-4.6-0005](../SPEC-4.6-0005/finding.md), alongside the identical pattern in `InProcessPatientSeries`.)

### Re-verified against current source (2026-09-02)

The always-increments defect was re-read directly in `cdsi-engine/src/main/java/org/openimmunizationsoftware/cdsi/core/logic/NoValidDoses.java` (lines 104-108 at commit `bde6b70`) rather than taken from this finding, and independently confirmed in the compiled artifact: `javap -c` on `cdsi-engine-5.3.4.jar` shows `evaluate_ACandidatePatientSeriesIsCompletable()` invoking `incPatientScoreSeries` twice (bytecode offsets 70 and 77) and `descPatientScoreSeries` never - while the sibling `evaluate_ACandidatePatientSeriesIsAProductPatientSeries()` in the same class invokes `inc` then `desc` as Table 8-11 requires.

## Interpretation

As written, "is completable" always contributes +1 regardless of the actual finish-date/max-age comparison it's supposed to be scoring.

The two extra scoring conditions are recorded as an open question, not resolved by guessing: this could be a deliberate, undocumented refinement to the spec-literal Table 8-11 logic, or a copy-forward from a different section's scoring logic that doesn't belong here. If real Supporting Data commonly has gender-restricted or near-max-age series competing under this scoring path, this materially changes the outcome versus a spec-literal implementation - worth checking against real FITS/Supporting Data cases before assuming either explanation. Tracked, not fixed, per standing project direction.

## Fix merged

Reviewed and merged by the project owner on 2026-09-03, commit `9fd975c` on `cdsi-reference` (cherry-picked from the investigating agent's `58a7e32`).

Scope: **only** the always-increments defect. The `Date` reference-equality bug (SPEC-4.6-0005) and the two undocumented scoring conditions were deliberately left untouched; both remain open questions.

The change is one line, in the `else` branch of `evaluate_ACandidatePatientSeriesIsCompletable()`:

```java
-          patientSeries.incPatientScoreSeries();
+          patientSeries.descPatientScoreSeries();
```

This makes the condition follow the +1/-1 split Table 8-11 specifies, using exactly the `if`/`else` shape the next method in the same class (`evaluate_ACandidatePatientSeriesIsAProductPatientSeries()`) already uses correctly for the same kind of condition. Nothing else in the file was changed.

Note on semantics, for the reviewer: the minimal fix also means a series whose forecast finish date or maximum-age date is *unavailable* (either is null) now scores -1 rather than +1, because the existing `if` already grouped "not completable" and "cannot be determined" into the same `else`. That follows Table 8-11's "Not true" column literally; whether "cannot be determined" ought to be a third, zero-scoring outcome is a separate specification question this fix does not attempt to settle.

### New unit test

`cdsi-engine/src/test/java/org/openimmunizationsoftware/cdsi/core/logic/NoValidDosesCompletableTest.java` (JUnit 4, matching the module's existing test conventions) - closes the "no dedicated unit test" gap this finding recorded for `NoValidDoses`. It drives the single scoring condition in isolation, invoking the private method reflectively so the production change stays a one-line fix. Five cases: completable scores +1; not-completable scores -1; a completable series strictly outscores a non-completable one; no finish date scores -1; a series with no forecast is not scored at all.

Against the pre-fix code, 3 of the 5 fail (each `expected:<-1> but was:<1>`); the 2 that pass pre-fix confirm the harness itself is sound. All 5 pass after the fix.

### FITS verification

Both runs used reference set `acip-4.6-sd-4.65-fits-8183b45d` (Logic Specification 4.6, Supporting Data 4.65, 4896 fixtures), verified by `ReferenceSetVerifier` at the start of each run. Same command for both (`mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest`), and the engine jar actually under test was confirmed by disassembly to be pre-fix for the first run and post-fix for the second.

| | Before | After |
| --- | --- | --- |
| Executed | 4896 | 4896 |
| Passed | 3364 | 3364 |
| Failed assertions | 1531 | 1531 |
| Execution errors | 1 | 1 |

- Run bundles: `2026-09-02T151219-...` (before) and `2026-09-02T153311-...` (after), under `cdsi-fits-tests/target/fits-runs/`.
- The after-run's own `changed-cases.json` reports `added: []`, `removed: []`, `statusChanged: []`.
- Compared independently case-by-case from both `results.jsonl` files: **0 cases newly passing, 0 regressions, 0 status changes of any kind.**
- Stronger check, since an unchanged status can still hide a changed value: comparing each case's recorded `actualHash` (the hash of the engine's actual forecast output) across all 4896 cases shows **0 cases whose engine output changed at all**.

So the fix is a strict no-op on this fixture set: it does not fix any FITS case, and it regresses none. The unit test, not FITS, is what demonstrates the corrected behaviour.

### Why the corrected scoring changes no FITS output

"No output changed" was not accepted at face value - the corrected branch is heavily exercised, so the absence of any effect needed explaining. Temporary instrumentation (counters only, reverted before commit) was added to `NoValidDoses` and `SelectPrioritizedPatientSeries` and the full suite re-run, to measure the fix's effect at each stage of the chain rather than only at the end.

First, the corrected branch is not dead code. Across the suite:

- `NoValidDoses.process()` ran **23,996** times, every one of them with more than one patient series in play.
- The "is completable" condition was evaluated **1,521,215** times: **284,480** took the true branch, **1,236,735** took the `else` branch - i.e. over 1.2 million score contributions genuinely flipped from +1 to -1.
- Those branch outcomes are *not* uniform per invocation: **21,678** of the 23,996 invocations were mixed (some series completable, some not), so the fix really does change candidates' scores relative to one another.

The score is also genuinely consumed: `SelectBestPatientSeries` (4.5) copies the same `PatientSeries` object references into `selectedPatientSeriesList`, and `SelectPrioritizedPatientSeries` (8.7) picks the highest `getScorePatientSeries()` from that list, which flows on to `DetermineBestPatientSeries` (8.8) and the reported forecast. So this is a live scoring path, not a dead one.

The effect disappears at the selection step. Since the only behavioural difference is that the `else` branch contributes -1 rather than +1, each series' pre-fix score is exactly `currentScore + 2 * (number of times it took that branch)`, which lets one instrumented run compute both the fixed-score winner and the pre-fix-score winner and compare them directly:

- **112,750** selections total; **71,440** with a non-empty candidate list; **54,352** with more than one candidate, where the score can actually decide the outcome.
- **10,057** selections where at least one candidate's score genuinely differed between the fixed and pre-fix scoring.
- **1,586** selections where the fix shifted the *competing* candidates by **different** amounts - the cases where it could in principle have reordered them, rather than moving them all equally.
- **0** selections where the *selected* patient series differed. (0 errors in the comparison itself, so this count is not masking a swallowed exception.)

That is the direct explanation, and note that it is not the easy one: the shift is *not* always uniform across the competing candidates. In 1,586 selections the correction moved competitors by different amounts and genuinely could have reordered them - yet in every one of those the series that was already winning still won, so the argmax never changed and nothing downstream of the selection could change either.

This is a measured result about these 4896 fixtures, not a proof that the condition can never matter. The unit test shows the condition now discriminates as Table 8-11 requires; a case that puts a completable and a non-completable series in close enough competition for the same antigen would be expected to show a difference. The counts above were identical across two independent instrumented runs, so they are stable, not sampling noise.

## Affected

- Spec sections: 8.6 (page 91)
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.NoValidDoses`
- FITS cases: none - no FITS case changed status or output in either direction (see FITS verification above)
