
# Processing Model Orchestration in Step Into CDSi

## Purpose

This document explains the **processing model orchestration** that governs how
Clinical Decision Support for Immunizations (CDSi) logic is actually executed
within Step Into CDSi.

While the CDC CDSi specification defines:

- evaluation rules,
- forecasting rules,
- and decision tables,

those elements alone do **not** produce a working forecasting engine.

Correct behavior depends on the **processing model** that:

- iterates across patient history,
- evaluates multiple candidate patient series,
- advances through target doses,
- and ultimately selects the best forecast outcome.

This orchestration layer is the **primary determinant of correctness** in CDSi
implementations.

---

## Logic Definition vs. Processing Model

The CDSi specification separates two distinct concerns:

### Logic Definition

This includes:

- business rules,
- decision tables,
- and constraint calculations.

These define **what should happen** for a single evaluation context.

---

### Processing Model

The processing model defines:

- **when** rules execute,
- **how often** they execute,
- **which patient series and target dose** they apply to,
- and **how intermediate results combine** into a final forecast.

Because real patients may have:

- multiple administered doses,
- multiple applicable series,
- and partially satisfied schedules,

evaluation and forecasting must run **repeatedly and iteratively**, not once.

This orchestration transforms static rules into a **working clinical algorithm**.

---

## Why Orchestration Errors Cause Systemic Failure

In deterministic CDS engines like CDSi:

- A single rule defect typically affects **one scenario**.
- An orchestration defect affects **nearly every scenario**.

Common orchestration failure modes include:

- advancing to the wrong target dose,
- evaluating the wrong patient series,
- entering forecasting too early or too late,
- or failing to preserve required intermediate state.

These defects frequently appear in FITS as:

- widespread date mismatches,
- incorrect status outcomes,
- or dramatic drops in pass percentage.

Therefore:

> Most large-scale CDSi failures originate in the **processing model**,  
> not in individual decision tables.

---

## Core Iterative Structure of CDSi Processing

At a high level, CDSi orchestration follows this repeating structure:

1. **Create relevant patient series** for an antigen.
2. For each patient series:
   - Generate **target doses**.
   - Iterate across **administered dose history (AARs)**.
   - Evaluate **dose validity and satisfaction**.
3. When evaluation completes:
   - Enter **forecast generation** for remaining unsatisfied doses.
4. After all series are processed:
   - **Select the best patient series**.
   - Produce the **final vaccine group forecast**.

This structure forms a **nested loop system**:

- outer loop → patient series  
- middle loop → target doses  
- inner loop → administered records  

Correct loop control is essential for correctness.

---

## Chapter 4 as the Execution Backbone

Within the CDC specification, Chapter 4 provides the
**authoritative orchestration model**.

It defines:

- evaluation neighborhoods,
- forecast neighborhoods,
- iterator advancement rules,
- and termination conditions.

In Step Into CDSi, Chapter 4 is effectively implemented through:

- coordinated **LogicStep sequencing**, and  
- controlled **DataModel state transitions**.

If Chapter 4 routing is incorrect:

- rule logic may still execute correctly,
- but overall forecasting becomes invalid.

---

## Target Dose State Progression

A central orchestration invariant is:

> A patient cannot advance to the next target dose  
> until the current target dose is satisfied or skipped.

This invariant controls:

- iterator advancement,
- evaluation completion,
- and entry into forecasting.

Violating this rule causes:

- premature forecasts,
- incorrect dose counts,
- and widespread FITS failures.

---

## Transition from Evaluation to Forecasting

Forecasting begins **only after**:

- all relevant administered doses are processed,
- the current target dose remains unsatisfied,
- and no further evaluation paths exist.

Premature transition leads to:

- missing valid doses,
- incorrect earliest/recommended dates,
- or null reference dose calculations.

Late transition leads to:

- duplicate evaluations,
- infinite or stalled loops,
- or overwritten forecast state.

Thus, this boundary is one of the **most sensitive points** in CDSi.

---

## Best-Series Selection as Final Resolution

After all patient series are evaluated and forecasted,
the processing model must:

- compare candidate series,
- apply prioritization rules,
- and select a **single authoritative outcome**.

Errors here produce:

- clinically plausible but incorrect forecasts,
- inconsistent behavior across antigens,
- or silent divergence from FITS expectations.

Because all prior work feeds into this step,
selection defects are **high impact but hard to diagnose**.

---

## Observability Requirements for Orchestration

To debug orchestration reliably, logs must expose:

- current patient series,
- current target dose index,
- administered record position,
- transition into forecast phase,
- and final best-series selection.

Without this visibility:

- failures appear as date mismatches only,
- root cause remains hidden in loop control,
- and regression fixes become guesswork.

Structured orchestration logging is therefore essential.

---

## Implications for AI-Assisted Debugging

AI reasoning is most effective when:

- rule logic is deterministic,
- execution traces are complete,
- and state transitions are explicit.

Because CDSi meets these conditions,
AI can reliably:

- trace failures backward through orchestration,
- distinguish rule defects from loop defects,
- and propose minimal corrective changes.

However, this is only possible when the
**processing model is clearly documented and observable**.

---

## Relationship to Other Documentation

This document is foundational and connects directly to:

- **01-overview-subchapter-loops.md** — structural looping model  
- **03-debugging-playbook.md** — practical failure analysis  
- **07-chapter-order-and-interpretation.md** — resolving ambiguity in rule timing  
- **05-test-execution-metrics.md** — detecting orchestration regressions  

Together, these define the **mechanical core** of Step Into CDSi behavior.

---

## Design Intent

The intent of this document is to establish a critical principle:

> In CDSi, correctness is governed primarily by  
> the **processing model orchestration**, not individual rules.

By making orchestration explicit, Step Into CDSi enables:

- deterministic debugging,
- regression-safe fixes,
- AI-assisted convergence,
- and eventual full conformance with the CDSi specification.
