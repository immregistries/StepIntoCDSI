# cdsi-fits-tests

Runs the NIST FITS conformance suite against `cdsi-engine` directly and offline - no servlet container, no live NIST connection once fixtures are downloaded (`org.openimmunizationsoftware.cdsi.fitstests.download.FitsDownloader`, run once against a live NIST FITS account; see its class Javadoc).

```bash
mvn -pl cdsi-fits-tests test
```

`FitsFixtureTest` is a `@TestFactory` that turns every fixture under `src/test/resources/fits/` into a dynamic test. The fixtures themselves are committed, so a normal checkout already has all 4896 of them - `FitsDownloader` is only for refreshing them from a live NIST FITS account, not a prerequisite for a first run.

## Which supporting data set the suite runs against

`cdsi-engine` can bundle more than one supporting-data zip under `src/main/resources/supporting-data/` - the standard CDC set (possibly at more than one published version, if an older one hasn't been removed yet) and, for the UI's benefit, alternative schedules such as a demo/preview set. `DefaultSupportingDataSet.resolve()` picks which one this suite forecasts against, and it is deliberately narrow:

- Only zips matching `supporting-data-<version>[-508].zip` are considered - anything else (a demo/preview set, an alternative schedule) is ignored. FITS conformance is only meaningful against the standard CDC set.
- Among those, it picks the **highest version number**, comparing dot-separated segments numerically (`4.10` > `4.9`), never the first one found or a lexicographic/alphabetical comparison (`"4.10"` sorts before `"4.9"` as a string).

This means adding a newly-published CDC supporting-data zip to `cdsi-engine/src/main/resources/supporting-data/` automatically makes the FITS suite start running against it, with no code change here required - which is the intended behavior: internal testing and verification should always run against the latest published supporting data, even though the web UI can still let a user explicitly select an older version or an alternative schedule (see `cdsi-web`'s `SupportingDataManager.resolveDefaultSupportingDataSet()`, which applies the identical "latest wins, unless explicitly overridden" rule to the UI's own default).

`DefaultSupportingDataSetTest` locks this in against cdsi-engine's real bundled resources - if it starts failing after adding or removing a zip, that's it correctly reporting an ambiguous or unexpected bundled set, not a flaky test.

Retiring an old CDC version's zip from `src/main/resources/supporting-data/` is a deliberate decision for whoever maintains this repository, not something either the FITS suite or the web UI's default forces - both are just consumers of whatever cdsi-engine bundles.

## Reference sets and fixture-set integrity

Before building the dynamic test list, `FitsFixtureTest` calls `ReferenceSetVerifier.loadAndVerify()`, which reads `src/test/resources/reference-set.json` (a plain JSON snapshot exported from `cdsi-reference` - see its `reference-sets/README.md`, Phase 16 of the reference-module plan) and re-derives two checksums:

- The bundled Supporting Data zip named in the reference set (read straight off the classpath, the same way `DefaultSupportingDataSet` finds it) must still hash to what the reference set recorded.
- Every fixture file actually under `src/test/resources/fits/` must still hash, combined, to what the reference set recorded (`ReferenceSetVerifier.computeFixtureSet()` - the identical algorithm, byte-for-byte, as `cdsi-reference`'s Python `compute_fixture_set`; `ReferenceSetVerifierTest` locks in that the two independently-computed values actually agree).

If either has drifted - a newer Supporting Data release replaced the one the reference set was created against, or the fixture files changed - `fitsFixtures()` throws `IllegalStateException` with a message naming exactly what changed, and JUnit reports the whole test class as failed rather than silently running against something the reference set doesn't describe. When that happens, the fix is to create and export a new reference set from `cdsi-reference` (`reference-set create` + `reference-set export`), not to edit this module's copy by hand.

The Logic Specification version recorded in the reference set is not checked here - there's no runtime artifact in a compiled `cdsi-engine` build to compare it against.

## Diagnostic run bundles (Phase 17)

Every run of `FitsFixtureTest` writes a complete, disposable diagnostic bundle to `target/fits-runs/<run-id>/`:

```text
target/fits-runs/<run-id>/
├── run.json            # git commit/branch/dirty, Java/Maven versions, reference set + checksums, test filter
├── summary.json         # discovered/executed/passed/failed/error counts
├── results.jsonl        # one compact JSON object per case: status, expected/actual hashes and values, field differences, duration
├── changed-cases.json   # diff against the most recent *previous* local run, if one exists
└── failures/<case-id>/
    ├── input.json        # the FITS fixture's patient/history/eval-date input
    ├── expected.json     # what FITS expected, per vaccine group
    ├── actual.json       # what the engine actually forecasted
    └── difference.json   # the specific field(s) that diverged, or "no forecasted vaccine group matched"
```

`target/` is Maven's own build directory and already gitignored - nothing here is ever committed automatically. `<run-id>` is `<timestamp>-<abbreviated-git-commit>-<reference-set-id>`, sanitized for use as a directory name on both Windows and POSIX filesystems.

`<case-id>` is `<testPlanId>-<groupName>-<uid>` - **not** just `<groupName>-<uid>`. The same group name (e.g. "HepA") and the same uid numbering scheme recur across different NIST test plans; groupName+uid alone collides for real (confirmed against the actual fixture set: 1053 of 4896 collide that way), which would otherwise make unrelated cases' failure bundles silently overwrite each other. `FitsRunRecorderTest` reproduces this exact scenario with two synthetic cases sharing a group and uid but different test plans, and confirms both bundles are written independently.

Two categories the plan's design for this bundle describes aren't available yet, and are recorded as `null` with an explanatory note rather than guessed at:

- **Baseline-relative fields** (`knownFailures`, `newRegressions`, `newlyPassingCases`, `changedKnownFailures` in `summary.json`, and each case's `baselineComparison` in `results.jsonl`) depend on a reviewed case-level regression baseline - Phase 19 of the reference-module plan, not built yet. `changed-cases.json`'s comparison against the previous local run is a same-machine convenience in the meantime, not a substitute for one.
- **`trace.jsonl`** (a structured, per-decision engine trace for each failure) depends on Phase 18 ("Improve Structured Engine Tracing"), not built yet. It is not written at all right now, rather than written empty - an absent file says "this capability doesn't exist yet," where an empty one would misleadingly say "the engine made no decisions."

## Known-passing allowlist

`cdsi-engine` does not pass every FITS case today, and closing that gap is exactly what `cdsi-reference` and this suite's diagnostic bundles exist to support (see `AGENTS.md`, the agent repair workflow) - but that's not a reason to fail the whole build on every not-yet-investigated case, nor to skip every case and quietly hope they all pass "one day." Some of the current gaps may turn out to be genuine CDSi specification or fixture defects that can never be "fixed" in this engine at all, so a plan that assumes 100% is eventually reachable isn't one to build the build around.

Instead, `src/test/resources/known-passing-cases.txt` is a plain-text, checked-in list of `<testPlanId>-<groupName>-<uid>` case ids (the same sanitized id `FitsTestCase.caseId()` computes and the diagnostic bundle's `failures/<case-id>/` uses - `#`-prefixed lines and blank lines are comments). `FitsFixtureTest` treats the two groups differently:

- **Listed** (currently expected to pass): checked with `Assertions.assertTrue` - if it fails, the build fails. This is the real regression signal: something that used to work broke.
- **Not listed**: checked with `Assumptions.assumeTrue` - if it fails, JUnit reports that case as **skipped/aborted**, not failed. The suite still runs it, still records it into the diagnostic bundle exactly like every other case, and the failure is fully visible there - it just doesn't turn the build red on its own.

This means a green `mvn -pl cdsi-fits-tests test` only ever asserts "every case we've verified we can pass, we still pass" - never "every FITS case passes" or "we're making progress toward 100%." Don't read a growing skip count as a problem by itself; read the diagnostic bundle's `summary.json` for the real pass/fail/error counts.

### Regenerating the allowlist

Only ever regenerate this file from a real run's results, reviewed by hand before committing - never hand-add a case id, and never add one just to make a red build green. Growing the list is exactly the reviewed step 10/11 of `AGENTS.md`'s workflow (record the finding, get the fix reviewed) once a fix is confirmed and merged, not something to do casually.

```bash
mvn -pl cdsi-fits-tests test -Dtest=FitsFixtureTest
```

then, from `cdsi-fits-tests/`, using the run bundle that command just wrote under `target/fits-runs/<run-id>/`:

```python
import json

cases = []
with open("target/fits-runs/<run-id>/results.jsonl", encoding="utf-8") as f:
    for line in f:
        entry = json.loads(line)
        if entry["status"] == "PASS":
            cases.append(entry["caseId"])
cases.sort()

with open("src/test/resources/known-passing-cases.txt", "w", encoding="utf-8", newline="\n") as out:
    out.write("# Case IDs (testPlanId-groupName-uid) that FitsFixtureTest currently\n")
    out.write("# requires to pass - see cdsi-fits-tests/README.md \"Known-passing allowlist\".\n")
    out.write(f"# Generated from run <run-id>, reviewed and committed by hand.\n")
    out.write("# Do not hand-edit case IDs in - only ever add entries generated from a real run.\n")
    for c in cases:
        out.write(c + "\n")
```

Before committing the regenerated file, diff it against the previous version and account for every change:

- **New entries** should correspond to cases you just fixed and can name (a specific finding id, a specific commit) - not an unexplained jump.
- **Removed entries** (a case that used to pass no longer does) is a regression - stop and investigate before regenerating over it; don't let the allowlist quietly shrink to match a broken build.

### A case can be date-relative and flaky, not actually fixed or broken

Some FITS fixtures encode dates relative to "today" at the time NIST resolved them (see the fixture format note above `FitsTestCase`) - it's possible for a case to flip pass/fail purely from calendar drift, with no code change involved. If an allowlisted case starts failing with no corresponding code change, check for date-dependence first (`NOT_REPRODUCIBLE` in the finding taxonomy) before assuming a real regression; if confirmed date-flaky, that's itself worth recording as a finding rather than either leaving it on the list to cause intermittent red builds, or quietly dropping it.
