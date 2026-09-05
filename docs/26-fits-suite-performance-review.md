# FITS Suite Performance Review - Outside Note

This note is an outside performance review of the offline FITS JUnit harness.
It is advisory only. It does not replace `cdsi-fits-tests/AGENTS.md`, and it is
not a request to change clinical logic. The immediate concern is Phase B
iteration speed: the full FITS suite is expected to be run many times, and the
current run time is roughly 15-16 minutes.

## Summary

The largest visible inefficiency is that each FITS case rebuilds the full
Supporting Data object graph from the ZIP.

`FitsFixtureTest` creates one dynamic JUnit test per fixture. Each dynamic test
calls:

```java
FitsEngineRunner.run(testCase, supportingDataSet)
```

and `FitsEngineRunner.run(...)` begins by calling:

```java
DataModel dataModel = DataModelLoader.createDataModel(supportingDataSet);
```

That call opens the selected supporting-data ZIP, reads each XML entry into
memory, parses DOM documents, and builds the `DataModel` supporting-data
structures. Since there are 4,896 FITS fixtures, the standard 4.65 Supporting
Data ZIP is effectively reparsed 4,896 times in one full run.

This matches the project owner's suspicion: the suite appears to be paying the
static Supporting Data load cost per case, even though only request/patient state
needs to be fresh for each case.

## Evidence From The Current Run

The latest local FITS bundle reviewed was:

```text
cdsi-fits-tests/target/fits-runs/2026-09-02T133929-786530700Z-bde6b70-acip-4.6-sd-4.65-fits-8183b45d
```

Its `run.json` records:

- started: `2026-09-02T13:39:29.786530700Z`
- finished: `2026-09-02T13:55:26.815987900Z`
- executed cases: `4896`
- pass/fail/error: `3364 / 1531 / 1`

Parsing `results.jsonl` shows the recorded per-case durations sum to about:

```text
942,345 ms = 15.7 minutes
average: 192 ms/case
p50: 95 ms
p90: 380 ms
p95: 499 ms
p99: 1365 ms
max: 7442 ms
```

The important part is that this timer is inside the dynamic test and wraps
`FitsEngineRunner.run(...)`, which includes `DataModelLoader.createDataModel`.
So most of the 15-minute wall-clock time is not Maven startup or dashboard
generation. It is the per-case runner path.

The diagnostic bundle itself is relatively small:

```text
6132 files
4.48 MB total
1532 failure directories
results.jsonl: about 3.0 MB
```

The diagnostic writer is still worth reviewing later, but it does not look like
the first-order explanation for the 15-minute run.

## Why This Is Plausibly Much Slower Than The Servlet Run

The current servlet path also appears to create a fresh `DataModel` per FITS
case in `FitsServlet.runTestsForGroup(...)`, so the comparison is not yet fully
explained by one obvious servlet-vs-JUnit difference. Still, the current JUnit
runner definitely reloads Supporting Data per case, and that is enough to make
the current harness structurally expensive.

Possible reasons the old manual servlet run felt closer to one minute:

- it may have run an older or smaller fixture set;
- it may have used a different Supporting Data loading path before ZIP-based
  resource selection was introduced;
- it may not have written full per-failure diagnostic bundles;
- it may have been measured after warmup or with fewer groups;
- the current engine logic may now do more work per case than the old servlet
  runner did.

Those possibilities are worth checking, but they do not change the main finding:
the FITS harness should not need to reparse static Supporting Data thousands of
times.

## Important Design Constraint

Caching cannot safely mean "reuse the same `DataModel` instance for every case"
unless the engine has a complete and well-tested reset boundary.

`DataModel` currently mixes two categories of state:

- static Supporting Data: CVX map, antigen map/list, vaccine groups, antigen
  series, schedules, observations, live-virus conflicts;
- per-request runtime state: forecast input, patient, immunization history,
  selected antigen lists, target doses, patient series, steppers, forecasts,
  loop guards, current logic step, previous/current target dose, and other
  traversal fields.

The Phase B fixes themselves are also likely to add or modify fields in this
area. Reusing the same mutable instance without a hard reset would risk
cross-case contamination and false FITS results.

The cleaner performance direction is to separate "loaded Supporting Data" from
"one forecast run's mutable working state."

## Candidate Improvement Paths

These are options for the main agent to consider, not instructions.

### Option A: Cached Supporting Data Snapshot + Per-Case Working Copy

Load the Supporting Data ZIP once per supporting-data set, cache the resulting
static object graph, and create a fresh per-case `DataModel` from that cached
snapshot.

This likely needs a clear API such as:

```java
LoadedSupportingData loaded = DataModelLoader.loadSupportingData(supportingDataSet);
DataModel dataModel = loaded.newDataModel();
```

or:

```java
DataModel dataModel = DataModelLoader.createDataModelFromCachedSupportingData(supportingDataSet);
```

The hard part is deciding whether `newDataModel()` deep-copies static objects,
shares immutable objects, or shares mutable-but-treated-as-read-only objects.
Because the existing domain model was not designed around immutability, this
should be verified carefully.

### Option B: Split `DataModel` Into Supporting Data And Runtime State

Introduce an explicit supporting-data holder that contains only schedule data,
then let each `DataModel` reference that holder while owning all per-run state.

This is architecturally cleaner and probably the long-term answer. It also has a
larger blast radius because many existing getters on `DataModel` expose the
supporting-data maps/lists directly.

This may be worth doing if Phase B is already going to touch loader/domain
representation for association ages, conditional skips, immunity, or
contraindications.

### Option C: Reset-And-Reuse A DataModel

Add a reset method that clears only per-request state and keeps Supporting Data
loaded.

This is probably the fastest implementation, but it is also the riskiest unless
there is strong test coverage proving every mutable per-run field is cleared and
no static Supporting Data object was mutated during the previous run.

Given the current number of traversal fields, steppers, lists, and loop guards,
this option should be treated carefully. A missed field could make one FITS case
depend on the previous case.

### Option D: Add A Fast Regression Mode Before Refactoring The Loader

As an interim tool, the FITS harness could support modes:

- full diagnostics: current behavior, all failure bundles;
- fast regression: record only `results.jsonl` and summary, possibly omit
  pretty-printed per-failure files;
- targeted group/case: run only a selected case or vaccine group.

This would not solve the main Supporting Data reload cost, but it could improve
Phase B ergonomics while the cache/split design is being reviewed.

## Parallelism

JUnit parallel execution is another possible speed lever, but it should not be
the first one.

The current implementation records through a synchronized `FitsRunRecorder`, so
recording could probably tolerate parallel dynamic tests. The larger concern is
engine and domain thread-safety. If each case has its own fully independent
`DataModel`, parallelism may be safe. If a cached Supporting Data graph is shared
across cases, then that graph must be treated as immutable or protected from
mutation.

Recommendation: establish the Supporting Data/runtime-state boundary before
turning on broad parallelism. Otherwise parallelism may hide cross-case mutation
bugs rather than reveal them.

## Suggested Next Investigation

Before implementing anything, the main agent could run a focused timing
experiment:

1. measure `DataModelLoader.createDataModel("supporting-data-4.65-508.zip")`
   by itself over a small number of iterations;
2. measure one or more FITS cases after the data model has already been loaded,
   if a temporary harness can isolate that;
3. measure diagnostic-bundle writing separately using the existing
   `results.jsonl` data shape.

The expected result is that Supporting Data loading accounts for a large share
of the per-case duration. If that is confirmed, the first performance fix should
be a cache/split of static Supporting Data, not test-runner micro-optimization.

## Recommendation

Make FITS performance its own enabling work item before Phase B enters repeated
full-suite repair cycles.

The target design should be:

- load and verify the selected Supporting Data once per suite run;
- create isolated per-case runtime state without reparsing the ZIP;
- preserve deterministic results and diagnostic output;
- prove no cross-case contamination with tests that run two very different cases
  back to back in both orders;
- keep the existing full diagnostic mode available for investigations.

If successful, this could plausibly move full-suite FITS feedback much closer to
the original servlet-era expectation and make Phase B substantially cheaper to
run without weakening the regression guardrail.
