# Business Rule Grouping and Execution Strategy in Step Into CDSi

## Purpose

This document explains how **business rules from the CDC CDSi specification**
are grouped, timed, and executed within the Step Into CDSi implementation.

A recurring challenge in interpreting the specification is that:

- business rules appear in **multiple chapters**,  
- the **exact timing of execution** is not always explicit, and  
- some rules depend on **state created elsewhere** in the process.

To achieve deterministic behavior, Step Into CDSi adopts a clear strategy for:

- where rules are evaluated,
- when they are evaluated,
- and how their results propagate through orchestration.

---

## The Core Problem: Distributed Rule Definitions

In the CDSi specification, business rules are often:

- described narratively in one location,
- encoded structurally in logic tables elsewhere,
- and relied upon implicitly by later chapters.

This distribution creates ambiguity:

- Should a rule run **immediately when encountered** in the text?
- Should it run **only when its data inputs exist**?
- Should it be evaluated **once per series, once per dose, or once per AAR**?

Without consistent interpretation, different implementations may produce
**different clinical outcomes** from the same specification.

---

## Timing Is Not Fully Defined in the Specification

Many business rules depend on values such as:

- Patient Reference Dose Date (PRDD)  
- Minimum or absolute minimum interval  
- Age-derived constraint dates  
- Evidence of immunity or contraindication  

However, the specification may not always state:

- **exactly when** these values must be computed,
- **how long** they remain valid,
- or **which chapter owns responsibility** for computing them.

As a result, implementers must introduce **explicit execution timing**
to ensure stable forecasting behavior.

---

## Step Into CDSi Execution Strategy

Step Into CDSi resolves this ambiguity using three guiding principles.

### 1. Rules Execute Within the Smallest Responsible LogicStep

Each business rule is evaluated in the **LogicStep where its inputs become valid**
and where its outcome directly influences the next control decision.

This ensures:

- minimal duplication of rule execution,
- clear ownership of derived values,
- and traceable alignment between rule outcome and orchestration behavior.

---

### 2. Rule Timing Follows Orchestration Phases

Rules are grouped according to the major CDSi phases:

- **Setup phase** — preparation of patient series and inputs  
- **Evaluation phase** — determination of dose validity and skip conditions  
- **Forecast phase** — reconciliation of timing constraints and recommendation generation  
- **Selection phase** — prioritization of final patient series and forecast  

A rule is executed **only when the process reaches the phase
where its result is meaningful**.

This prevents:

- premature computation using incomplete data,
- stale values persisting across iterations,
- and conflicting rule outcomes.

---

### 3. Derived Values Must Be Recomputable and Traceable

Any value produced by a business rule must:

- be stored in the **DataModel**,
- be recomputable from upstream state,
- and be attributable to a **specific rule or logic table**.

This guarantees that:

- debugging can trace incorrect forecasts to a rule origin,
- AI reasoning can follow causal chains,
- and regression fixes remain localized.

---

## Handling Rules Referenced in Multiple Chapters

When the same conceptual rule appears in multiple parts of the specification,
Step Into CDSi applies the following interpretation:

- The rule is **computed once** at the point where all required inputs exist.  
- Later references are treated as **consumers of the computed value**,  
  not triggers for re-execution.  
- Recalculation occurs only when **upstream state changes**  
  (such as advancing to a new target dose or patient series).

This approach prevents:

- inconsistent duplicate calculations,
- circular dependencies,
- and subtle divergence across chapters.

---

## Common Failure Modes Without Explicit Grouping

If rule grouping and timing are not enforced, the system may exhibit:

- **Null-derived dates** due to premature interval calculation  
- **Incorrect skip behavior** from stale PRDD values  
- **Forecast drift** caused by recomputing constraints at the wrong time  
- **Series mis-selection** when evaluation artifacts are incomplete  

These failures often appear as **clinical mismatches**
rather than obvious technical errors.

---

## Implications for Debugging and Logging

Because rule timing is critical:

- logs must reveal **when a rule executed**,  
- which **inputs were present**,  
- and what **value was produced**.

Alerts should occur when:

- a rule executes with missing inputs,
- a required derived value is null,
- or a fallback path replaces expected rule output.

This visibility is essential for:

- deterministic debugging,
- regression isolation,
- and AI-assisted defect analysis.

---

## Relationship to Other Documentation

This document complements:

- **02-spec-to-code-mapping.md** — structural translation of spec logic into code  
- **07-chapter-order-and-interpretation.md** — resolving ambiguity caused by chapter ordering  
- **alerting semantics documentation** — signaling missing or mistimed rule outcomes  

Together, these define how Step Into CDSi converts
**distributed narrative rules** into a **deterministic execution model**.

---

## Design Intent

The intent of this strategy is to ensure that:

- every business rule has a **clear execution boundary**,  
- every derived value has a **traceable origin**,  
- and CDSi behavior remains **stable and reproducible** across runs.

By imposing explicit grouping and timing, Step Into CDSi transforms
a **narratively distributed specification** into a **reliable forecasting engine**.
