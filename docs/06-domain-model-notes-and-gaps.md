# Domain Model Notes and Interpretation Gaps in Step Into CDSi

## Purpose

This document captures practical guidance for understanding and using the
**domain model diagrams** provided in the CDC CDSi logic specification.

While the diagrams are valuable for conveying the **conceptual structure of CDSi**,
they also introduce challenges for implementation, debugging, and AI-assisted
reasoning. This document explains:

- why the diagrams matter,
- why they are difficult to operationalize,
- and what interpretations or conventions Step Into CDSi adopts in code.

The goal is to provide a **clear, text-based mental model** that complements the
visual diagrams and supports consistent development.

---

## Value of the Domain Diagrams

The CDSi domain diagrams illustrate:

- the major entities involved in forecasting  
  (patient, administered doses, target doses, patient series, forecasts, etc.)
- the relationships between those entities  
- the flow of reasoning from **historical vaccination** to **future recommendation**

These visuals help readers grasp the **overall structure** of CDSi more quickly
than prose alone. In particular, they clarify that CDSi is not a single linear
calculation, but a **stateful evaluation across interconnected objects**.

For new developers, the diagrams provide the first usable picture of:

- how patient history connects to schedule logic,
- how series and doses are organized,
- and where forecasts ultimately emerge.

---

## Practical Limitations of the Diagrams

Despite their conceptual value, the diagrams present several challenges in
real-world implementation.

### Images Are Not Searchable or Executable

Because the diagrams exist primarily as **static images**, they cannot be:

- searched for specific terms,
- referenced programmatically,
- or directly mapped to code structures.

This makes them difficult for:

- developers navigating large codebases,
- automated tooling such as AI assistants,
- and long-term maintenance efforts.

As a result, important structural meaning can remain **implicit rather than explicit**.

---

### Ambiguity in Behavioral Interpretation

The diagrams describe **relationships**, but not always:

- **when** transitions occur,
- **which component controls** a decision,
- or **how iteration boundaries** are enforced.

These behavioral details are instead distributed across:

- narrative specification text,
- business rule descriptions,
- and logic tables.

Implementers must therefore **infer execution semantics** that are not fully
captured visually.

---

### Mismatch Between Conceptual and Executable Models

Domain diagrams are designed for **human understanding**, not direct execution.

Consequently:

- some diagram relationships are **many-to-many** while code requires
  deterministic traversal,
- lifecycle timing of objects may be **unclear**,
- and responsibility boundaries between entities may be **unstated**.

Bridging this gap requires **interpretive design decisions** in software.

---

## Interpretations Adopted in Step Into CDSi

To translate the conceptual domain into executable behavior, Step Into CDSi
applies several consistent conventions.

### Centralized Session State via DataModel

Rather than distributing state across many domain objects, Step Into CDSi uses a
single **DataModel** to carry:

- patient information,
- administered dose history,
- active patient series and target doses,
- intermediate evaluation artifacts,
- and final forecast results.

This simplifies orchestration and ensures that each LogicStep operates on a
**shared, authoritative state**.

---

### LogicSteps as Behavioral Boundaries

Where diagrams show **relationships**, Step Into CDSi introduces **LogicStep
classes** to define:

- when evaluation occurs,
- when forecasting begins,
- how iteration advances,
- and when series selection completes.

This provides an **explicit execution order** that the diagrams alone do not define.

---

### Deterministic Traversal of Conceptual Relationships

Many diagram relationships allow multiple interpretations.  
Step Into CDSi resolves this by enforcing:

- ordered iteration over **patient series**,
- sequential evaluation of **target doses**,
- controlled comparison with **administered records (AARs)**,
- and explicit transition into **forecast generation**.

These rules ensure that the conceptual model becomes a **repeatable algorithm**.

---

## Implications for Debugging and AI Reasoning

Because the authoritative behavior lives in **code and state transitions**, not
just diagrams:

- debugging must rely on **logged execution state**, not visual structure alone,
- AI tools must consume **textual documentation and code**, not images,
- and maintainers must treat diagrams as **guides**, not exact specifications.

For this reason, Step Into CDSi documentation emphasizes:

- narrative explanations,
- structured logging,
- and explicit state tracking.

These provide the **machine-readable clarity** that diagrams cannot.

---

## Recommended Documentation Strategy

To compensate for diagram limitations:

- Preserve diagram intent in **markdown documentation**.
- Describe **entity lifecycles and control flow** in text.
- Record **implementation conventions and assumptions** explicitly.
- Link domain concepts to **LogicStep classes and DataModel fields**.

This approach converts **implicit visual meaning** into **explicit executable knowledge**.

---

## Relationship to Other Documentation

This document complements:

- **01-overview-subchapter-loops.md** — structural orchestration model  
- **02-spec-to-code-mapping.md** — translation from specification to code  
- **04-what-to-log-debug-tables.md** — observable runtime state  

Together, these materials transform the CDSi domain model from a
**conceptual diagram** into a **practical implementation framework** suitable for
debugging, extension, and AI-assisted development.

---

## Design Intent

The intent of this document is not to replace the CDC domain diagrams, but to
make their meaning **operationally usable** within Step Into CDSi.

By clarifying assumptions, resolving ambiguities, and documenting executable
interpretations, the project establishes a **stable conceptual foundation** for
achieving full CDSi conformance.
