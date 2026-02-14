# Target Dose Progression and Series State Transitions in Step Into CDSi

## Purpose

This document explains how **target dose progression** and **patient series state
transitions** function within the CDSi processing model and how they are
implemented in Step Into CDSi.

These mechanics form the **state machine at the heart of CDSi**.  
Nearly every clinical outcome—dose validity, skip behavior, forecast timing,
and final recommendation—depends on correctly tracking:

- target dose satisfaction,
- series completion,
- and evaluation status transitions.

Mismanaging this state progression leads to **system-wide forecast errors**,
even when individual rules and decision tables are correct.

---

## Why Target Dose Progression Matters

The CDSi model is fundamentally sequential:

- Each patient series contains an **ordered list of target doses**.
- Evaluation determines whether administered vaccinations **satisfy** those doses.
- Forecasting occurs only for **remaining unsatisfied doses**.

A core invariant governs this behavior:

> A patient cannot advance to the next target dose  
> until the current target dose is satisfied, skipped, or otherwise resolved.

Violating this invariant produces:

- premature forecasts,
- incorrect dose counts,
- invalid interval calculations,
- and widespread FITS failures.

Because of this, **target dose progression is one of the most critical
correctness boundaries in CDSi**.

---

## Target Dose Lifecycle

Each target dose moves through a predictable lifecycle:

1. **Not satisfied**  
   Initial state before evaluation of administered doses.

2. **Satisfied (valid)**  
   An administered dose meets:
   - age requirements,
   - interval constraints,
   - vaccine type rules,
   - and absence of disqualifying conditions.

3. **Not valid / extraneous**  
   An administered dose exists but does not satisfy the target dose due to:
   - age violations,
   - interval violations,
   - wrong vaccine formulation,
   - or other rule failures.

4. **Skipped**  
   Conditional logic determines the dose is:
   - no longer required, or
   - superseded by schedule rules.

5. **Forecast required**  
   No administered dose satisfies the target dose,
   and forecasting must compute:
   - earliest date,
   - recommended date,
   - and status.

Correct CDSi behavior depends on **strictly ordered progression**
through these states.

---

## Series State Transitions

A **patient series** progresses according to the collective state of its
target doses.

### Active Evaluation Phase

During evaluation:

- The system iterates through:
  - administered records (AARs),
  - target doses,
  - and rule checks.

The series remains in evaluation until:

- all AARs are consumed **and**
- the current target dose remains unresolved.

---

### Forecast Phase Entry

The series transitions into forecasting when:

- evaluation can no longer satisfy the current target dose, and
- no additional administered data can change the outcome.

At this point, the system computes:

- forecast timing,
- recommendation status,
- and completion expectations.

Entering forecast **too early or too late** is a common orchestration defect.

---

### Series Completion

A series is considered complete when:

- all target doses are:
  - satisfied,
  - skipped,
  - or clinically resolved.

Completion state influences:

- best-series selection,
- vaccine group evaluation,
- and final recommendation output.

Incorrect completion detection causes:

- extra required doses,
- missing recommendations,
- or wrong prioritization among candidate series.

---

## Evaluation Status Semantics

Evaluation statuses carry **clinical meaning**, not just technical flags.

Common statuses include:

- **Valid** — contributes to satisfying a target dose.
- **Not valid** — present but unusable for satisfaction.
- **Extraneous** — clinically unnecessary or outside schedule logic.
- **Skipped** — bypassed by rule logic rather than satisfied.
- **Not satisfied** — still requires forecasting.

Correct propagation of these statuses is essential because they affect:

- PRDD calculation,
- interval derivation,
- series completion,
- and best-series prioritization.

A single incorrect status can cascade into **global forecast error**.

---

## Common Failure Modes

### Advancing Target Dose Too Early

Causes:

- missing valid administered doses,
- incorrect interval reference dates,
- premature forecasting.

---

### Failing to Advance After Satisfaction

Causes:

- repeated evaluation of the same target dose,
- stalled orchestration loops,
- duplicated or conflicting forecasts.

---

### Incorrect Skip Handling

Causes:

- required doses disappearing,
- unexpected series completion,
- mismatch with FITS expectations.

---

### Status Misclassification

Causes:

- wrong PRDD,
- invalid interval timing,
- incorrect best-series selection.

Because these errors affect **state progression**,  
they typically produce **widespread regression**, not isolated failure.

---

## Observability Requirements

Reliable debugging requires logs that expose:

- current patient series,
- current target dose index,
- administered record position,
- evaluation status transitions,
- entry into forecast phase,
- and series completion decision.

Without this visibility:

- failures appear only as incorrect dates or statuses,
- root cause remains hidden in state progression,
- regression fixes become unreliable.

---

## Relationship to Orchestration

Target dose progression is tightly coupled to:

- **processing model orchestration**  
  (see `11-processing-model-orchestration.md`)
- **decision table outcomes**  
  (see `12-decision-table-semantics-and-rule-evaluation-order.md`)

Decision tables determine **whether a dose satisfies**,  
while orchestration determines **when progression occurs**.

Both must be correct for CDSi to function.

---

## Implications for AI-Assisted Debugging

Because target dose progression forms a **deterministic state machine**,
AI analysis is especially effective when logs include:

- explicit state transitions,
- evaluation status changes,
- and iterator movement.

With this visibility, AI can:

- detect premature or missing transitions,
- identify incorrect satisfaction logic,
- and propose minimal corrections that restore conformance.

---

## Design Intent

The intent of this document is to highlight a central CDSi truth:

> Forecast correctness depends first on  
> **accurate target dose progression and series state transitions**,  
> not on individual rule details.

By making this state machine explicit, Step Into CDSi enables:

- deterministic debugging,
- regression-safe fixes,
- and reliable convergence toward full CDSi conformance.
