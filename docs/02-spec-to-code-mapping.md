# Mapping the CDSi Specification to Step Into CDSi Code

## Purpose

This document explains how the **narrative structure of the CDC CDSi logic specification** is translated into the executable architecture of the Step Into CDSi implementation.

The goal is to provide developers—and AI assistants such as GitHub Copilot—with a clear mental model of how a **specification sub-chapter becomes runnable code**, and how control flows from one logical step to the next.

Understanding this mapping is essential for:

- debugging orchestration defects,
- locating the correct place to implement fixes,
- adding meaningful logging,
- and ensuring behavioral alignment with the CDC specification.

---

## From Narrative Specification to Executable Steps

Each CDSi sub-chapter describes a **bounded unit of reasoning** within the larger evaluation and forecasting process.  
Although written in prose, the structure of a sub-chapter is highly consistent and can be decomposed into four conceptual layers:

1. **Step description** – what the logic step is intended to accomplish.
2. **Attributes and data inputs** – the patient, dose, or schedule information required.
3. **Business rules** – conditional reasoning that determines outcomes.
4. **Logic tables** – structured decision matrices encoding rule behavior.

Step Into CDSi mirrors this structure directly in code so that the **execution path follows the same conceptual order** as the written specification.

---

## Representation in Step Into CDSi

### LogicStep Classes

Each major logical phase or sub-chapter in the specification is implemented as a dedicated **`LogicStep` subclass**.

A LogicStep is responsible for:

- reading required values from the shared **DataModel**,  
- executing the rule logic defined for that step,  
- updating the DataModel with any new state or results,  
- and determining **which LogicStep executes next**.

This creates a **chain of execution** that parallels the narrative flow of the specification.

---

### The DataModel as Shared State

The **DataModel** acts as the in-memory representation of the patient evaluation session.

It carries:

- immunization history,
- patient demographics,
- derived evaluation artifacts,
- forecast constraints and results,
- and orchestration state such as current series, target dose, and AAR position.

Every LogicStep both **depends on** and **mutates** this shared structure.  
Correct forecasting therefore depends not only on rule correctness, but on **precise state transitions between steps**.

---

### Business Rules in Code

Within each LogicStep, **business rules** from the specification are implemented as executable condition logic.

These rules:

- evaluate dates, ages, intervals, and vaccine properties,
- determine validity or skip conditions,
- and control downstream behavior such as satisfaction of a target dose or transition to forecasting.

Because the specification’s prose can be ambiguous about **when** rules execute,  
Step Into CDSi encodes the **timing and grouping** of rules explicitly within each LogicStep.

---

### Logic Tables as Deterministic Decision Structures

Where the CDC specification defines **decision tables**, Step Into CDSi reproduces them in code as structured logic table components.

Logic tables provide:

- deterministic mapping from **input conditions → outcomes**,  
- traceable alignment with specification content,  
- and a stable surface for debugging and conformance testing.

Most **clinical correctness** issues originate in rule or table behavior,  
but most **systemic failures** originate in orchestration surrounding them.

---

## Determining the Next Step

A defining responsibility of each LogicStep is deciding:

> **What happens next?**

This decision is based on:

- rule outcomes,
- iterator state (patient series, target dose, AAR position),
- phase transitions (SETUP → EVALUATE → FORECAST),
- and completion conditions defined in Chapter 4 of the specification.

Incorrect next-step routing is one of the **most common causes of conformance failure**,  
because even perfectly correct rule evaluations cannot compensate for:

- evaluating the wrong dose,
- forecasting too early or too late,
- or selecting the wrong patient series.

---

## Why This Mapping Matters

This spec-to-code alignment provides several critical benefits:

- **Traceability** – developers can locate the exact code corresponding to a spec sub-chapter.  
- **Debuggability** – failures can be analyzed in terms of specification intent rather than raw code behavior.  
- **Extensibility** – new rules or clarifications from CDC can be inserted into the correct LogicStep boundary.  
- **AI interpretability** – structured correspondence enables AI tools to reason about correctness, logging, and defects using the same conceptual model as the specification.

Without this mapping, debugging devolves into low-level code inspection rather than **specification-driven reasoning**.

---

## Relationship to Other Documentation

This document builds on the structural overview described in:

- **01-overview-subchapter-loops.md**

Subsequent documents describe:

- debugging strategy,
- logging expectations,
- execution metrics,
- and domain interpretation challenges.

Together, they form a **bridge between CDC narrative guidance and executable software behavior**,  
enabling systematic progress toward full CDSi conformance.
