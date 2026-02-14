# Separation of Concerns in CDSi Architecture

## Purpose

This document explains the **three-layer architectural separation** underlying
Clinical Decision Support for Immunizations (CDSi) and how that separation is
mirrored within the Step Into CDSi implementation.

The CDC CDSi design intentionally divides responsibility across:

1. **Supporting Data** — configuration and schedule content  
2. **Logic Definition** — clinical rules and decision semantics  
3. **Processing Model** — algorithmic orchestration and iteration  

This separation exists to:

- reduce conceptual and implementation complexity,
- improve long-term maintainability,
- and isolate **schedule evolution** from **core logic behavior**.

Understanding this structure is essential for safe debugging,
refactoring, and AI-assisted reasoning.

---

## The Three-Layer CDSi Model

### 1. Supporting Data (Configuration Layer)

Supporting data contains the **parameterized description of immunization
schedules**, including:

- patient series definitions,
- target dose timing constraints,
- interval rules,
- contraindications and skip conditions,
- vaccine group relationships,
- and temporal effective/cessation boundaries.

This layer answers the question:

> **What are the current clinical schedule rules?**

It is expected to:

- evolve over time,
- vary across jurisdictions or policy updates,
- and change **without requiring code modification**.

Because of this, supporting data must remain:

- externalized,
- versioned,
- and independently testable.

---

### 2. Logic Definition (Rule Semantics Layer)

Logic definition expresses the **clinical reasoning model** used to interpret
supporting data, including:

- business rules,
- decision tables,
- evaluation status semantics,
- constraint reconciliation,
- and forecast determination logic.

This layer answers:

> **How should schedule data be interpreted to reach a clinical decision?**

Unlike supporting data, logic definition should remain:

- **stable across schedule updates**,  
- consistent across implementations,  
- and deterministic for identical inputs.

Changes here represent **true CDSi logic evolution**,  
not routine schedule maintenance.

---

### 3. Processing Model (Algorithmic Orchestration Layer)

The processing model governs **execution flow**, including:

- iteration across patient series,
- traversal of target doses,
- comparison with administered history,
- transition from evaluation to forecasting,
- and final best-series selection.

This layer answers:

> **When and in what order are rules applied to patient data?**

It transforms:

- static configuration **and**
- deterministic rule logic  

into a **working forecasting algorithm**.

Errors in this layer typically cause:

- widespread FITS regression,
- incorrect forecast timing,
- or systemic status mismatches.

---

## Why the CDC Designed This Separation

### Reducing Complexity

Without separation:

- schedule content,
- rule semantics,
- and execution flow  

would become tightly coupled.

This would make:

- reasoning difficult,
- debugging slow,
- and correctness fragile.

Layering allows each concern to be understood **independently**.

---

### Improving Maintainability

Clinical schedules change frequently.  
Core CDSi logic changes rarely.

By isolating:

- **supporting data updates**  
  from  
- **logic or orchestration changes**,  

the architecture allows:

- schedule maintenance without code rewrites,
- safer long-term evolution,
- and clearer responsibility boundaries.

---

### Isolating Schedule Changes from Logic Changes

A key architectural goal is:

> Updating vaccine timing or eligibility  
> must **not require altering CDSi rule logic**.

If a schedule update forces code changes,  
the separation of concerns has been violated.

This principle protects:

- clinical correctness,
- interoperability across implementations,
- and regression stability.

---

## How Step Into CDSi Mirrors This Architecture

Step Into CDSi reflects the three-layer model through:

### Supporting Data Integration

- External configuration artifacts populate
  **domain structures and logic tables**.
- Schedule changes can be incorporated
  **without rewriting orchestration code**.

---

### LogicStep Rule Implementation

- Business rules and decision tables are implemented in
  **discrete LogicStep classes**.
- Rule semantics remain stable even as
  **supporting data evolves**.

---

### ForecastServlet and Orchestration Flow

- Iteration, evaluation sequencing, and best-series selection
  are controlled by the **processing model implementation**.
- This layer is intentionally separate from:
  - rule semantics,
  - and schedule configuration.

---

## Common Violations of Separation

### Hard-coding Schedule Values in Logic

Leads to:

- fragile updates,
- hidden regressions,
- and divergence from specification intent.

---

### Embedding Orchestration Decisions in Rule Logic

Causes:

- duplicated control flow,
- inconsistent state transitions,
- and debugging difficulty.

---

### Recomputing Rule Semantics in Multiple Layers

Results in:

- non-deterministic outcomes,
- maintenance drift,
- and FITS instability.

Maintaining strict boundaries prevents these failures.

---

## Implications for Debugging and Refactoring

Effective debugging begins by identifying **which layer is responsible**:

- Incorrect timing or eligibility → likely **supporting data**  
- Wrong clinical interpretation → likely **logic definition**  
- Widespread regression or looping issues → likely **processing model**

This layered diagnosis dramatically reduces
**time to root cause**.

---

## Implications for AI-Assisted Development

AI reasoning becomes far more reliable when:

- configuration,
- rule semantics,
- and orchestration  

are **clearly separated and documented**.

This enables AI to:

- localize defects to a specific layer,
- avoid hallucinating cross-layer fixes,
- and propose minimal, correct changes.

Without separation, AI suggestions become
**structurally unsafe**.

---

## Relationship to Other Documentation

This architectural view connects directly with:

- `10-input-artifacts-and-dependencies.md` — supporting data foundations  
- `12-decision-table-semantics-and-rule-evaluation-order.md` — rule logic  
- `11-processing-model-orchestration.md` — execution sequencing  

Together, these describe the **complete CDSi system structure**.

---

## Design Intent

The intent of this document is to reinforce a central CDSi principle:

> Long-term correctness depends on  
> **strict separation between configuration, rules, and orchestration**.

By preserving this architecture, Step Into CDSi supports:

- maintainable schedule evolution,
- deterministic debugging,
- regression-resistant refactoring,
- and trustworthy AI-assisted improvement.
