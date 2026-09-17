# Phase B FITS Progress Ledger - Outside Note

This note is an outside recommendation for Phase B reporting infrastructure. It
is advisory only. It is meant to describe the need and the review goals, not to
dictate the exact file format or command names.

## Problem

The current FITS dashboard is useful for seeing the latest state of the world:
overall pass/fail/error counts, pass rate, vaccine-group breakdown, and current
non-passing cases. That is not enough for Phase B.

Phase B will make larger, reviewed changes: first the `SupportingDataModel` /
`DataModel` separation, then foundational logic fixes. For those changes, the
project owner needs to see not only "what is the current pass rate?" but also:

- what changed because of this fix?
- did any previously passing case fail?
- which vaccine groups improved or worsened?
- which cases newly pass?
- which cases newly fail?
- did runtime improve?
- which finding, architectural change, or repair round explains the movement?

Without a reviewed delta view, the project has to compare full FITS runs by hand
or rely on the latest dashboard plus memory. That is too weak for the early
Phase B changes, where each repair may touch shared infrastructure or core logic.

## Current Infrastructure

The existing infrastructure already has most of the raw material:

- every full FITS run writes a bundle under
  `cdsi-fits-tests/target/fits-runs/<run-id>/`;
- each bundle includes `run.json`, `summary.json`, `results.jsonl`,
  `changed-cases.json`, and failure details;
- `fits-results.html` renders the latest run as a committed snapshot;
- `changed-cases.json` compares against the most recent previous local run.

The gap is that `changed-cases.json` is a local convenience, not a reviewed
project record, and `fits-results.html` only shows the latest run. There is no
small, intentional artifact that says, "This approved change moved the suite
from A to B, with these gains, these regressions, and this explanation."

## Recommended Concept

Add a lightweight reviewed progress ledger for Phase B.

The ledger should promote selected before/after comparisons into committed
project artifacts. Raw run bundles can remain disposable under `target/`; the
ledger only needs to preserve the reviewed summary of important transitions.

Conceptually:

```text
Baseline run
  pass/fail/error counts
  pass rate
  runtime
  group breakdown

After run
  pass/fail/error counts
  pass rate
  runtime
  group breakdown

Delta
  newly passing cases
  newly failing cases
  errors resolved or introduced
  group-level changes
  runtime change
  related finding ids or architectural work item
  short reviewer-facing explanation
```

The exact storage format could be YAML, JSON, Markdown, generated HTML, or some
combination. The important point is that each reviewed Phase B repair round gets
a durable, readable before/after record.

## First Proving Use Case

The first use case should be the `SupportingDataModel` / `DataModel` separation.

For that work, success should look different from a clinical logic fix:

```text
Functional result:
  pass/fail/error counts unchanged
  no known-passing FITS regressions
  no unexpected case-status changes

Performance result:
  runtime substantially reduced
  ideally from roughly 15-16 minutes toward 40-90 seconds

Interpretation:
  architecture improved and feedback loop became faster
  clinical behavior intentionally unchanged
```

This is a good proving case for the ledger because the expected functional delta
is zero. If the comparison tooling cannot show "same results, faster run"
clearly, it will not be good enough for later logic fixes where the deltas are
more complicated.

## Phase B Repair Use Case

For a later logic repair, the ledger should help answer:

- which step-test failures did this repair address?
- which FITS cases moved from fail/error to pass?
- did any pass move to fail/error?
- did any failure change shape without becoming a pass?
- are group-level changes concentrated where expected?
- is any unexpected movement explained by the finding or repair narrative?

Some correct, spec-aligned fixes may not improve FITS. That should still be
recordable as progress if the step tests improve and no regressions are
introduced. Conversely, a higher FITS pass count should not be accepted blindly
if it comes with unexplained regressions or a drift away from the Logic
Specification.

## Possible Artifacts

One possible shape:

```text
cdsi-reference/fits-progress/
  baseline.yaml
  entries/
    2026-09-xx-supporting-data-model-split.yaml
    2026-09-xx-forecastdtcan-1.yaml

cdsi-reference/dashboards/fits-progress.html
```

Another possible shape is a generated Markdown ledger with one section per
reviewed change. The main agent should choose the format that fits the existing
`cdsi-reference` tooling best.

Whatever the shape, each entry should probably include:

- entry id or slug;
- date generated;
- before run id and commit;
- after run id and commit;
- related branch or change description;
- related finding ids, if any;
- overall before/after counts and pass rate;
- runtime before/after;
- group-level pass/fail/error deltas;
- newly passing case count and list or linked file;
- newly failing/regressed case count and list or linked file;
- errors resolved or introduced;
- short human explanation;
- review status or reviewer note, if useful.

The ledger does not need to store every full failure detail if that remains in
the run bundles or can be regenerated. It should store enough to preserve the
reviewed claim.

## Dashboard View

A generated dashboard would be useful if it answers these questions at a glance:

- What is the current reviewed baseline?
- What was the pass rate after each major Phase B round?
- How much did runtime change?
- Which rounds introduced no functional change but improved infrastructure?
- Which rounds improved FITS?
- Which rounds were spec-correct but FITS-neutral?
- Did any round introduce regressions, and how were they handled?

This should complement `fits-results.html`, not replace it:

- `fits-results.html`: latest full-suite state.
- progress ledger/dashboard: reviewed before/after history of meaningful
  changes.

## Regression Boundary

The ledger should make regressions visible rather than hiding them in aggregate
counts.

Important distinctions:

- newly passing cases are useful progress;
- newly failing cases are potential regressions;
- known-passing allowlist regressions should remain a hard stop;
- a changed failure that is still failing may still matter if its shape changed;
- an error changing to a failure may be progress even if it is not yet a pass;
- a failure changing to an error is almost certainly a regression.

The dashboard should not only say "+27 passes." It should also say whether any
passes were lost and whether any errors were introduced.

## Suggested Workflow

A possible reviewed workflow for each major Phase B round:

```text
1. Run full FITS before the change, or select an existing reviewed baseline.
2. Make the architectural or logic change.
3. Run focused step/engine tests.
4. Run full FITS after the change.
5. Generate latest FITS dashboard.
6. Generate a before/after progress entry.
7. Review the delta.
8. Commit the change and the reviewed progress artifact together, if approved.
```

The first version does not need to be elaborate. Even a small generated
before/after Markdown report would be a major improvement over manually diffing
two large run bundles.

## Non-Goals

The progress ledger does not need to become a full analytics system.

It does not need:

- a database;
- interactive historical charts;
- every local experimental run;
- automatic approval of new known-passing cases;
- a replacement for findings;
- a replacement for raw diagnostic bundles.

It only needs to preserve the reviewed before/after story for meaningful Phase B
changes.

## Recommendation

Build this after or alongside the `SupportingDataModel` / `DataModel` split, and
before the main sequence of clinical logic repair rounds.

The first ledger entry should ideally prove:

```text
SupportingDataModel split:
  same FITS results
  no known-passing regressions
  materially faster full-suite runtime
```

After that, use the same mechanism for each major Phase B repair cluster. This
will give the project owner, agents, and reviewers a clear way to see whether
the engine is moving forward: more spec-correct, no unexplained regressions, and
eventually a higher FITS pass rate.
