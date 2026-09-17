# FITS Suite Performance And Data Boundary Review - Outside Note

This note is an outside performance review of the offline FITS JUnit harness.
It is advisory only. It does not replace `cdsi-fits-tests/AGENTS.md`, and it is
not a request to change clinical logic. The immediate concern is Phase B safety
and iteration speed: the full FITS suite is expected to be run many times, and
the current run time is roughly 15-16 minutes.

The recommendation here is to make an explicit architectural boundary before
Phase B logic repair begins:

```text
SupportingDataModel
  static knowledge-base / schedule data
  loaded once per selected Supporting Data set
  not mutated during forecasting

DataModel
  one forecast run's mutable working state
  created fresh per request or FITS case
  discarded after the run
```

This should be treated as a non-functional refactor. It should not change
clinical behavior, FITS expected/actual outcomes, known-passing case membership,
or per-step logic semantics. Its purpose is to make the original intended design
real: Supporting Data stays loaded, while each run gets fresh mutable state.

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

## Why The Current Boundary Is Risky

The original design idea appears sound: for each forecast run, create fresh run
state, while keeping Supporting Data loaded. The current implementation does not
provide that boundary. It avoids state leakage by rebuilding everything for each
case, but that makes FITS slow.

`DataModel` currently mixes two categories of state:

- static Supporting Data: CVX map, antigen map/list, vaccine groups, antigen
  series, schedules, observations, live-virus conflicts;
- per-request runtime state: forecast input, patient, immunization history,
  selected antigen lists, target doses, patient series, steppers, forecasts,
  loop guards, current logic step, previous/current target dose, and other
  traversal fields.

This means there is no object that cleanly means "the loaded knowledge base" and
no object that cleanly means "this one forecast run." A caller either reloads the
entire ZIP into a new `DataModel`, or it would have to reuse a `DataModel` and
manually clear every mutable field. The second path would be a kludge and should
not be the primary strategy.

The risk is not only that there are many fields to reset. It is also that the
domain objects are mutable and many getters expose live mutable collections. For
example, supporting-data structures such as antigens, vaccine types, antigen
series, series doses, intervals, allowable vaccines, and preferable vaccines are
all regular mutable domain objects. Runtime objects then point back to them:

- a `PatientSeries` tracks an `AntigenSeries`;
- a `TargetDose` tracks a `SeriesDose`;
- a `Forecast` tracks a `TargetDose`;
- a `VaccineDoseAdministered` references a loaded `VaccineType`.

That pointer structure is reasonable, but only if the loaded objects are treated
as read-only during processing. The code does not currently enforce that. A
shared cached `DataModel` could therefore leak accidental mutation from one FITS
case into the next.

So the problem is not "JUnit is slow." The problem is that the engine lacks a
real static-data/runtime-state boundary. The performance issue is the symptom
that makes the missing boundary expensive.

## Recommended Refactor

The preferred direction is to introduce `SupportingDataModel` as the explicit
home for static Supporting Data, then let `DataModel` own only the mutable state
for one forecast run.

This is not a CDSi Logic Specification concept. It is an implementation boundary
the engine needs so the specification logic can be run repeatedly and safely.

Conceptually:

```java
SupportingDataModel supportingData =
    DataModelLoader.loadSupportingData("supporting-data-4.65-508.zip");

DataModel dataModel = new DataModel(supportingData);
dataModel.setForecastInput(testCase.toForecastInput());
```

or, if the loader owns the cache:

```java
DataModel dataModel = DataModelLoader.createDataModel(supportingDataSet);
```

where `createDataModel(...)` returns a fresh `DataModel` but attaches a cached
`SupportingDataModel` internally instead of reparsing the ZIP every time.

For compatibility, `DataModel` can initially keep many of its existing
supporting-data getters and delegate them to `SupportingDataModel`. That would
avoid forcing every logic step to change at once:

```java
public Map<String, VaccineType> getCvxMap() {
  return supportingDataModel.getCvxMap();
}

public List<AntigenSeries> getAntigenSeriesList() {
  return supportingDataModel.getAntigenSeriesList();
}
```

This allows the project to draw the boundary first, then migrate call sites more
gradually if desired.

## What Belongs In `SupportingDataModel`

Candidate static Supporting Data fields:

- CVX map;
- antigen map and derived antigen list;
- vaccine-group map and derived vaccine-group list;
- antigen series list;
- schedule list;
- live-virus conflict list;
- observation map;
- clinical guideline observation map, if it is loaded from Supporting Data;
- future supporting-data-owned structures such as association-age metadata,
  conditional-skip definitions, immunity definitions, and contraindication
  definitions.

These objects should be loaded once per selected data set and then treated as
immutable for the duration of a forecast run and across FITS cases.

## What Belongs In `DataModel`

Candidate per-run mutable fields:

- forecast input;
- patient;
- immunization history;
- assessment date;
- current logic step and previous logic step;
- current antigen, vaccine group, target dose, previous target dose, current
  forecast, and current antigen administered record;
- selected antigen lists and position counters;
- selected antigen administered record lists and position counters;
- target dose lists and position counters;
- patient series stepper;
- selected, scorable, prioritized, and best patient-series lists;
- forecast list and vaccine-group forecast list;
- loop guards and orchestration state such as `Neighborhood`;
- any log or trace state added later.

These should be new for every request or FITS case.

## Avoid Reset-And-Reuse As The Main Design

Adding `DataModel.resetForNextRun()` and reusing one instance would probably be
the fastest patch, but it is the wrong default shape for this project. It would
depend on an exhaustive list of fields to clear, and that list is exactly what
Phase B will continue to change.

A missed field could contaminate the next test case. A field cleared too
aggressively could erase loaded schedule data. Either error would undermine the
FITS suite as a regression signal.

The better design is fresh `DataModel`, shared `SupportingDataModel`.

## Mutability Guardrails

Because the existing domain model was not built as immutable, the first version
of `SupportingDataModel` may need pragmatic guardrails rather than a full
immutability rewrite.

Possible guardrails:

- expose unmodifiable maps/lists from `SupportingDataModel` where practical;
- keep mutation methods package-private or loader-only where practical;
- add tests that fingerprint the loaded Supporting Data before and after a run;
- run two very different FITS cases back to back in both orders and assert the
  results are identical by case id;
- avoid sharing any object that is known to be mutated during processing;
- document any shared mutable object that is intentionally treated as read-only.

A full deep-copy of the entire supporting-data graph per case would be safer
than sharing mutable objects, but it may erase the performance benefit. The
right balance is likely to make the loaded graph read-only by convention and
tests first, then tighten immutability over time.

## Optional Runner Improvements

After the data boundary exists, the FITS harness could still support faster
developer modes:

- full diagnostics: current behavior, all failure bundles;
- fast regression: record only `results.jsonl` and summary, possibly omit
  pretty-printed per-failure files;
- targeted group/case: run only a selected case or vaccine group.

These are secondary. They do not solve the main Supporting Data reload cost or
the architectural ambiguity by themselves.

## Parallelism

JUnit parallel execution is another possible speed lever, but it should not be
the first one.

The current implementation records through a synchronized `FitsRunRecorder`, so
recording could probably tolerate parallel dynamic tests. The larger concern is
engine and domain thread-safety. If each case has its own fully independent
`DataModel`, parallelism may be safe. If a cached Supporting Data graph is shared
across cases, then that graph must be treated as immutable or protected from
mutation.

Recommendation: establish the `SupportingDataModel`/`DataModel` boundary before
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
be the `SupportingDataModel` split, not test-runner micro-optimization.

## Recommendation

Make the Supporting Data/runtime-state split its own enabling work item before
Phase B enters repeated full-suite repair cycles.

The target design should be:

- load and verify the selected Supporting Data once per suite run;
- create a fresh per-case `DataModel` without reparsing the ZIP;
- keep static schedule/knowledge-base content in `SupportingDataModel`;
- make `DataModel` the mutable forecast-run context;
- preserve deterministic results and diagnostic output;
- preserve all existing clinical behavior;
- prove no cross-case contamination with tests that run two very different cases
  back to back in both orders;
- keep the existing full diagnostic mode available for investigations.

If successful, this could plausibly move full-suite FITS feedback much closer to
the original servlet-era expectation and make Phase B substantially cheaper to
run without weakening the regression guardrail.

## Acceptance Criteria

This refactor should be considered successful only if:

- the FITS pass/fail/error counts are unchanged before and after the refactor;
- previously passing engine tests do not regress;
- previously known-passing FITS cases do not regress;
- each FITS case/request receives a fresh mutable `DataModel`;
- Supporting Data is loaded once per selected data set in a suite run;
- the code makes it clear which fields are static Supporting Data and which are
  per-run state;
- tests demonstrate that running cases in different orders does not change their
  results;
- any remaining shared mutable Supporting Data objects are documented and
  protected by tests from accidental mutation during forecasting.

This should be done before clinical logic fixes because it makes later Phase B
changes safer to reason about. The project should not have to wonder whether a
logic fix changed the rule, changed the data, or leaked state from the previous
run.
