# Reference Implementation Stewardship Pass

This note describes a later cleanup and alignment phase to consider after the
Phase A per-step tests and Phase B logic repair work have substantially settled.
It is advisory. It is not a request to start broad cleanup before the
specification-conformance and FITS repair work have stabilized.

## Purpose

StepIntoCDSI is intended to be a reference-style implementation of the CDSi
Logic Specification, used for testing, measurement, step-through inspection, and
feedback to CDSi/CDC. For this project, code clarity is not merely cosmetic. A
reviewer should be able to walk from the Logic Specification to the engine code,
then to the tests and documentation, without needing hidden project history to
explain why things are named or organized the way they are.

The stewardship pass should make the implementation read like the specification
where it is implementing specification logic, while clearly preserving the
infrastructure and application affordances needed to run, inspect, test, and
demonstrate that implementation.

## Goal

The goal is not to delete every piece of code that is not literally printed in
the Logic Specification. The goal is to make each piece of code honest about
what it is.

Useful categories:

```text
Spec Logic
  Implements a documented CDSi processing step, business rule, decision table,
  state change, or Supporting Data interpretation.

Implementation Infrastructure
  Required to run the model: Supporting Data loading, runtime orchestration,
  loop guards, APIs, servlet/FHIR adapters, diagnostics, caching, regression
  harnesses, and dashboard generation.

Demonstration / Testing Affordances
  Features intentionally added so AIRA can inspect, measure, debug, or step
  through the model, such as filtering to one antigen or series, step-through
  views, diagnostic logging, controlled test execution, and other conveniences
  for human review.
```

All three categories may be valid. The problem is not that non-spec code exists.
The problem is when non-spec code looks like a CDSi processing step, when old
implementation artifacts remain in the main flow, or when naming/comments imply
specification authority that the document does not support.

## What To Preserve

Some behavior exists because the application needs it, not because the Logic
Specification defines it. That behavior should be preserved when it is useful
and deliberate.

Examples:

- evaluating or stepping through only one antigen or series so the UI can focus
  on a human-readable slice of the process;
- step-through rendering and detailed diagnostic logs;
- loop guards that prevent a malformed or buggy process from running forever;
- FITS fixture loading, known-passing regression checks, and diagnostic bundles;
- dashboards and reference-module tooling;
- servlet, FHIR, and other adapter code that converts external inputs into the
  engine's internal model.

These should be documented as implementation infrastructure or testing/demo
affordances. They should not be removed merely because they are not themselves
CDSi business rules.

## What To Clean Up

The stewardship pass should look for code and documentation that is obsolete,
misleading, duplicative, or difficult to reconcile with the current model.

Candidate cleanup areas:

- dead or unreachable logic steps;
- placeholder step labels such as non-section values or pseudo-sections;
- stale table references, old chapter numbers, and inaccurate log messages;
- duplicate implementations of the same business rule;
- code paths that reflect an older routing model and are no longer active;
- comments that describe intentions no longer true in the code;
- TODOs that have either been resolved or need to become real findings;
- helper classes that should be clearly labeled as infrastructure rather than
  specification steps;
- public mutable state that blurs the line between Supporting Data and per-run
  forecast state;
- documentation that disagrees with the code, mappings, findings, or current
  dashboards.

For example, an unmapped engine class may deserve one of several outcomes:

- remove it if it is truly unused and obsolete;
- fold its behavior into the numbered step where the specification places that
  behavior;
- preserve it as infrastructure if it performs a real non-spec function;
- document it as a deliberate implementation guardrail if it must remain outside
  the specification's step structure.

The desired result is one-for-one alignment where the code is implementing CDSi
steps, and explicit labeling where the code is doing something else.

## Naming And Structure

Where practical, names should follow the Logic Specification even when a more
idiomatic software name might be shorter or cleaner. This includes:

- step class names;
- business-rule helper names;
- method names for calculated dates and rule outputs;
- condition-attribute names;
- state-change terminology;
- test method names;
- findings and dashboard labels;
- comments and log messages that explain rule execution.

This does not mean every internal helper must use long specification prose. It
does mean that a reader should not have to guess whether a class maps to Section
7.4, Table 7-10, FORECASTDTCAN-1, or a local application convenience.

## Relationship To Phase B

This stewardship pass should probably happen after major Phase B logic repairs,
not before them, with one exception: architectural boundary work that makes Phase
B safer, such as separating static `SupportingDataModel` content from per-run
mutable `DataModel` state, may need to happen earlier.

During Phase B, avoid broad cosmetic churn. If a repair touches a stale comment,
wrong log message, or misleading helper name in the immediate area, it is fine to
clean it up. But repository-wide cleanup should wait until the functional
direction is stable enough that the cleanup will not be repeatedly invalidated.

## Guardrails

The stewardship pass should follow these guardrails:

- do not change clinical behavior accidentally;
- do not remove non-spec behavior just because it is non-spec;
- do not hide implementation infrastructure by pretending it is a spec step;
- do not rename or reorganize code in ways that break traceability to existing
  findings, tests, dashboards, or step packages;
- do not collapse useful test/demo affordances into undocumented side effects;
- do not delete an apparent leftover until its reachability and purpose have
  been checked;
- document any intentional deviation from the Logic Specification or any
  necessary implementation choice the specification leaves open.

A successful stewardship pass should make the project calmer to read: fewer
historical leftovers, clearer ownership boundaries, better naming, and a more
direct path from source document to implementation to tests.

## Working Definition

The work can be summarized this way:

```text
Make the codebase read as a faithful reference implementation of the CDSi Logic
Specification, while clearly preserving the infrastructure and application
affordances needed to run, inspect, test, and demonstrate that implementation.
Remove or relocate unjustified leftovers, not legitimate implementation choices.
```
