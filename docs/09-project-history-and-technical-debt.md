# Project History and Technical Debt in Step Into CDSi

## Purpose

This document provides historical and architectural context for maintainers of
the **Step Into CDSi** codebase. Its goal is to explain:

- how the project evolved over time,
- why portions of the implementation now contain **technical debt** or outdated assumptions,
- and why the current phase prioritizes **documentation, logging, and deterministic debugging**
over rapid feature expansion.

Understanding this history is essential for making safe, effective changes.

---

## Origins of the Project

Step Into CDSi began as a **student-driven implementation** of the CDC CDSi
logic specification. The initial objectives were educational rather than
production-oriented:

- demonstrate how CDSi forecasting works,
- map specification chapters into executable code,
- and provide an interactive way to **step through logic execution**.

Because of this origin:

- correctness across the full FITS suite was **not the primary constraint**, and  
- architectural decisions favored **clarity and progress** over long-term rigor.

This foundation enabled rapid learning but also introduced structural fragility.

---

## Transition to Conformance-Oriented Development

Over time, the role of Step Into CDSi shifted from:

> **educational demonstration → conformance-focused reference implementation**

This transition created tension between:

- the **original exploratory architecture**, and  
- the **precision required for clinical rule conformance**.

As FITS testing and deeper debugging began, several realities emerged:

- orchestration correctness is extremely sensitive to small logic changes,
- incomplete or implicit assumptions can invalidate forecasts broadly,
- and lack of observability makes root-cause analysis slow and uncertain.

These discoveries revealed accumulated technical debt.

---

## Sources of Technical Debt

### 1. Educational-First Architecture

Early design choices prioritized:

- readability,
- direct mapping from specification narrative,
- and rapid iteration.

Less emphasis was placed on:

- strict separation of responsibilities,
- deterministic state management,
- or long-term maintainability.

This results in areas where:

- control flow is implicit rather than enforced,
- rule timing assumptions are embedded in code,
- and debugging requires deep manual tracing.

---

### 2. Partial or Evolving Specification Interpretation

The CDSi specification itself is:

- complex,
- distributed across chapters,
- and occasionally ambiguous in execution timing.

During early implementation:

- some interpretations were **incomplete**,
- others were **reasonable but later disproven**, and  
- some logic paths were implemented only **far enough to demonstrate behavior**.

As conformance expectations increased, these areas became visible as debt.

---

### 3. Limited Observability and Logging

Initial logging focused on:

- demonstrating flow to human learners,
- not enabling **systematic debugging**.

Consequences include:

- difficulty diagnosing FITS failures,
- uncertainty about rule execution timing,
- and inability for AI tools to reason over execution traces.

This lack of observability is now one of the **highest-impact technical gaps**.

---

### 4. Incremental Fixes Without Global Regression Guardrails

Historically, fixes were often applied:

- to address a **specific failing scenario**,  
- without always validating **suite-wide impact**.

Because CDSi logic is highly interconnected, this practice allowed:

- regressions to accumulate,
- behavior to drift subtly,
- and confidence in correctness to erode.

This pattern is a classic form of **latent technical debt**.

---

## Why Documentation and Logging Are Now the Priority

The current phase of the project recognizes that:

> **Reliable conformance cannot be achieved through ad-hoc fixes alone.**

Instead, progress requires:

- explicit architectural understanding,
- deterministic execution visibility,
- and reproducible debugging workflows.

Documentation and structured logging provide the foundation for:

- safe refactoring,
- AI-assisted diagnosis and repair,
- regression-resistant improvements,
- and eventual full FITS conformance.

Without this groundwork, further logic changes would likely
**increase instability rather than reduce it**.

---

## Reframing Technical Debt as Recoverable Structure

Although the codebase contains debt, it also has significant strengths:

- a close structural mapping to the CDC specification,
- working orchestration scaffolding,
- FITS integration for objective validation,
- and an interactive stepping interface for deep inspection.

These qualities make the system **recoverable** through:

- improved observability,
- clarified execution semantics,
- and disciplined regression control.

The project is therefore not starting over,  
but **stabilizing and completing an existing foundation**.

---

## Guidance for Maintainers

Maintainers should approach the codebase with the following mindset:

- Assume historical decisions were **contextually reasonable**,  
  even if they now require revision.
- Prefer **clarity and determinism** over minimal code change.
- Validate every behavioral modification against the **full FITS suite**.
- Strengthen **logging and documentation** before altering complex logic.
- Treat regressions as signals of **architectural misunderstanding**,  
  not merely bugs to patch.

This posture converts technical debt into a **structured improvement path**.

---

## Relationship to Other Documentation

This document provides context for:

- **03-debugging-playbook.md** — practical debugging workflow  
- **05-test-execution-metrics.md** — regression detection and validation  
- **alerting semantics documentation** — visibility into unexpected states  

Together, they define the **stabilization strategy** guiding the project’s
current development phase.

---

## Design Intent

The intent of this document is to ensure that future contributors understand:

- **why the system looks the way it does**,  
- **why stability work now takes precedence**, and  
- **how disciplined documentation and logging enable real progress**.

By preserving this context, Step Into CDSi can move from:

> exploratory implementation → trustworthy clinical logic engine.
