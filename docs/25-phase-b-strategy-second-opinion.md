# Phase B Strategy - Outside Review

This note is an outside strategy review for the agents and project owner working
through the Phase 21 per-step specification-conformance effort. It is not a
replacement for `cdsi-engine/AGENTS.md`, `cdsi-fits-tests/AGENTS.md`, or the
step-level documentation in `cdsi-reference`. Treat it as a second opinion on
how to approach the fixing phase once enough Phase A evidence exists.

## Overall Goal

The goal is not simply to make tests pass. The goal is to make StepIntoCDSI
follow the CDSi Logic Specification and Supporting Data as closely as possible,
while using tests to measure where the implementation, the specification, the
supporting data, and FITS expectations agree or disagree.

This project is a demonstration, testing, and measurement application for AIRA.
It is not currently a production clinical decision system. That matters for
prioritization. Patient-safety completeness is important long term, but the
near-term emphasis should remain on the core age-based schedule mechanisms that
the current test cases actually exercise: dose evaluation, forecast need/date
generation, and patient-series selection.

The desired FITS target is at least 90% passing if it can be reached honestly,
with the longer-term aspiration of getting as close to 100% as the specification,
supporting data, and fixtures allow. Passing more FITS cases is valuable, but
passing them by drifting away from the specification is not.

## Phase B Posture

Phase B should probably not proceed unit by unit in specification order. The
Phase A step tests are exposing clusters of related defects that cut across
multiple units: shared date rules, loader/domain representation gaps, decision
table semantics, and orchestration/state issues. Fixing those one unit at a time
risks duplicated partial fixes or, worse, local workarounds that make one test
green while preserving the deeper defect.

A better Phase B unit of work is a related problem cluster:

1. identify the cluster and the evidence behind it;
2. classify the discrepancy before editing code;
3. propose a coherent fix that follows the specification;
4. predict which step tests should improve and which FITS groups/cases may move;
5. make the smallest general change that resolves the root cause;
6. run the focused tests, full engine tests, and FITS regression checks;
7. document the finding, deviation, or unresolved question before moving on.

No FITS improvement can still be success if the change makes the engine more
spec-correct and causes no regressions. However, a change that moves the project
backward should not be accepted as a narrow local fix. If a correct-looking fix
breaks another part of the logic, the unit of work may need to be enlarged so
the whole cluster lands in a better, internally consistent state.

## Source Priorities

When sources disagree, avoid treating any one source as automatically
authoritative:

- The Logic Specification is the primary design target.
- Supporting Data is the concrete schedule data the engine must interpret.
- FITS is the end-to-end measurement suite and a strong signal, but not an
  absolute oracle.
- The per-step JUnit tests are the diagnostic map for spec-conformance.

If one FITS case appears wrong, it is acceptable to document and continue. If a
larger group points to a likely specification or supporting-data defect, the
implementation may need to follow the likely intended behavior, but that should
be documented as a deviation or finding for later CDSi/CDC follow-up.

## Suggested Early Problem Clusters

These are not commands or assignments. They are candidate early clusters that
look foundational to the current evidence and aligned with the near-term FITS
goal.

### 1. Forecast Candidate Date Unification

The `FORECASTDTCAN-1` candidate earliest date appears to be implemented in more
than one place, especially around Section 7.4 and Section 7.5, with divergent
sets of candidate dates. There is also evidence of a shared `DateRules` rule
object that may have been intended as the common home but is not currently the
active implementation.

This is a good early candidate because it lives directly in the ordinary
forecast-date path. It affects both whether a forecast is needed and what date is
reported. A fix should avoid making two local copies merely agree by coincidence;
the strategic direction is one shared calculation that both 7.4 and 7.5 rely on,
with any currently unrepresentable rule inputs documented rather than hidden.

Likely benefit: some 7.4 and 7.5 step-test failures should become green, and
FITS forecast-date failures may improve.

Main risk: the full rule includes inputs such as inadvertent administrations and
conflict end dates that may not yet be represented cleanly. That should shape
the fix scope.

### 2. Basic Section 7.5 Forecast-Date Fallbacks

Separate from the shared candidate-date problem, Section 7.5 has ordinary date
fallback and storage behavior that appears central to forecast output:
recommended-date fallback, null handling for missing latest recommended
intervals, and preserving unadjusted recommended/past-due dates on the forecast.

This cluster is attractive because it stays focused on date behavior and avoids
starting with the less-tested recommended-vaccine, dose-number, contraindication,
or administrative-guidance portions of the section.

Likely benefit: 7.5 date-rule tests should improve, and some FITS date
differences may move.

Main risk: date differences can reflect subtle interpretations of the
specification or supporting data. Do not contort the engine to match a fixture if
the fixture conflicts with the documented rule.

### 3. CVX-to-Antigen Association Ages

Section 4.2 expects CVX-to-antigen association to honor association begin/end
age. Current evidence suggests the loader/domain model discards those association
ages, leaving `OrganizeImmunizationHistory` with only a plain antigen list. That
means one administered vaccine can be associated with too many antigens for the
patient's age.

This is upstream of almost everything else. If administered doses are assigned to
the wrong antigen, later Section 6 evaluation, Section 7 forecasting, and Section
8 series selection may all be reasoning over incorrect records.

Likely benefit: 4.2 failures should improve, and Varicella/Zoster-related FITS
behavior may improve.

Main risk: this is a loader/domain representation change, not just a local 4.2
change. Combination products and boundary ages should be handled with care and
checked against real Supporting Data.

### 4. Conditional Skip Context and Accumulation

The same `ConditionalSkip` representation gap appears to affect both Section 6.2
and Section 7.1. The model needs to represent context, and `SeriesDose` needs to
preserve multiple conditional-skip entries rather than retaining whichever one
appeared last in the XML. Evaluation and forecast steps should then filter by
the context they are allowed to use.

This is a good example of why Phase B should avoid narrow per-unit repairs. A
loader-only change that happens to help one side could easily break the other.

Likely benefit: 6.2 and 7.1 failures should improve, and the implementation will
be less dependent on Supporting Data document order.

Main risk: some current FITS passes may be accidental. A successful fix should
make both evaluation and forecast contexts correct together, then account for
any FITS movement.

### 5. Shared Decision Table Semantics

`LogicTable.evaluate()` currently appears to execute every matching column and
allow the last matching outcome to win. That is a shared framework behavior and
could affect many step classes.

This is high leverage but should be approached carefully. Before changing it
globally, Phase B should probably inventory which current step-test failures are
actually caused by overlapping rule columns, and whether the relevant printed
tables establish a clear precedence or exactly-one-match expectation.

Likely benefit: could resolve failures such as Section 6.10 and prevent local
workarounds elsewhere.

Main risk: this has broad blast radius. A global first-match or exactly-one
policy may expose genuine specification ambiguities in tables that are not
mutually exclusive. An impact report may be more valuable than an immediate code
change as the first step.

## Areas To Defer Unless They Block Core Progress

Contraindications, observations, risk factors, lot-number handling, forecast
recommended-vaccine metadata, dose-number metadata, and administrative guidance
all matter for eventual specification completeness. They should not be ignored.
But they are probably not the first pass-rate lever unless the developing
evidence shows they block ordinary age-based schedule behavior or a large FITS
cluster.

The known contraindication/immunity chain is real and cross-cutting, but it is
larger than a simple routing fix. It touches loader behavior, domain types,
schedule/antigen scoping, missing 7.3 decision logic, 7.4 forecast-need
consumption, and later Chapter 8 consumers. That may be worth a dedicated repair
round, but it should not crowd out the core age/interval/date mechanics unless
FITS evidence says it is currently the dominant blocker.

## Practical Success Criteria For Each Phase B Round

A strong Phase B round should end with:

- the root cause classified using the existing finding taxonomy;
- a proposed or implemented fix that follows the Logic Specification rather than
  a specific test fixture;
- relevant per-step JUnit tests improved or intentionally left red with an
  explanation;
- no previously passing engine tests regressed;
- no known-passing FITS cases regressed;
- any changed FITS results accounted for, especially unexpected movement outside
  the target cluster;
- dashboards regenerated only after the project owner decides the result is
  review-worthy;
- findings or deviations recorded in the existing `cdsi-reference` system.

The important balance is this: every round should move the project forward, but
forward does not always mean a higher FITS percentage. Forward means a more
faithful implementation, clearer documentation of unavoidable disagreements, and
no unexplained regressions.
