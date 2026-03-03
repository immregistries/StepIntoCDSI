# Chapter Order and Interpretation in the CDSi Specification

## Purpose

This document explains how **chapter ordering** in the CDC CDSi logic specification
affects interpretation, implementation, and debugging within Step Into CDSi.

Although the specification presents logic in a **linear narrative sequence**,
actual CDSi execution depends on:

- orchestration across multiple chapters,
- iterative revisiting of rule domains,
- and contextual interpretation of logic tables.

Without understanding this interaction between **order** and **process**,  
table outcomes may appear ambiguous or contradictory.

---

## The Illusion of Linear Execution

At first reading, the CDSi specification appears to describe a
**step-by-step linear algorithm** progressing from:

1. data preparation  
2. dose evaluation  
3. forecasting  
4. final recommendation  

In reality, CDSi behaves as a **stateful, looping system** where:

- later chapters depend on artifacts created earlier,
- earlier logic may be revisited after new information emerges,
- and orchestration (primarily Chapter 4) governs transitions rather than
  simple chapter sequence.

This means:

> **Chapter order describes logical grouping, not strict execution order.**

---

## Why Logic Table Outcomes Can Appear Ambiguous

Logic tables encode deterministic decisions, yet ambiguity arises when:

- the **timing of table execution** is unclear,
- required **input state has not yet been produced**,
- multiple chapters appear to provide **overlapping authority**, or
- orchestration determines **which table result is ultimately used**.

For example:

- A table may correctly evaluate a dose as valid,  
  but if executed against the **wrong target dose** or **wrong patient series**,
  the overall forecast becomes incorrect.
- Multiple constraint sources may yield different dates,  
  but only one becomes dominant based on **process context** rather than
  table logic alone.

Thus, correctness depends not only on **what tables return**,  
but **when and why they are invoked**.

---

## The Role of Chapter 4 as Context Provider

Chapter 4 provides the **execution context** that resolves ambiguity by defining:

- iteration across **patient series**,
- advancement through **target doses and administered records**,
- transition from **evaluation to forecasting**,
- and selection of the **best patient series**.

Without Chapter 4’s orchestration:

- later chapters cannot be interpreted reliably,
- table outcomes lack operational meaning,
- and execution order becomes indeterminate.

In implementation terms:

> **Chapter 4 transforms static rule logic into a working algorithm.**

---

## Multi-Next-Step Chapters and Branching Behavior

Some chapters allow **multiple possible next steps** depending on:

- rule outcomes,
- iterator state,
- or completion conditions.

These multi-branch transitions are a primary source of:

- orchestration defects,
- infinite or premature loop termination,
- and forecast timing errors.

Correct handling requires explicit decision logic in code rather than
implicit reliance on narrative ordering.

---

## Rules of Thumb Learned During Implementation

### 1. Always Interpret Tables Within Process Context

A logic table result is only meaningful when evaluated alongside:

- current patient series,
- current target dose,
- active administered record,
- and orchestration phase.

Never debug a table in isolation.

---

### 2. When Behavior Is Ambiguous, Check Chapter 4 First

Most unexpected outcomes originate from:

- incorrect loop advancement,
- premature transition to forecasting,
- or wrong series prioritization.

Before modifying rule logic, confirm that **orchestration is correct**.

---

### 3. Missing Inputs Usually Indicate Earlier Chapter Failure

If a table lacks required inputs:

- the defect is typically **upstream**, not in the table itself.
- tracing backward across chapter boundaries is essential.

---

### 4. Forecast Errors Often Reflect Evaluation Context, Not Forecast Logic

Incorrect forecast dates frequently result from:

- wrong target dose satisfaction,
- skipped evaluation artifacts,
- or null reference dose calculations.

Thus, debugging should begin in **evaluation chapters**, not forecast chapters.

---

### 5. Determinism Requires Explicit State Boundaries

Narrative descriptions may imply state transitions,
but executable correctness requires:

- explicit iterator control,
- clear phase transitions,
- and deterministic next-step selection.

These must be enforced in code, not inferred from prose.

---

## Implications for Debugging and AI Reasoning

For both human developers and AI assistants:

- chapter order should be treated as **conceptual grouping**,  
  not strict runtime flow.
- ambiguity in table outcomes should trigger examination of:
  - orchestration state,
  - iterator position,
  - and prior chapter execution.

Effective reasoning therefore depends on combining:

- **structural understanding** (looping model),
- **state visibility** (debug logging),
- and **specification context** (chapter intent).

---

## Relationship to Other Documentation

This document builds upon:

- **01-overview-subchapter-loops.md** — structural looping architecture  
- **02-spec-to-code-mapping.md** — translation from spec narrative to code  
- **03-debugging-playbook.md** — practical failure analysis workflow  

Together, these establish the **interpretive framework**
required to implement CDSi reliably.

---

## Design Intent

The intent of this document is to prevent a common implementation failure:

> Treating the CDSi specification as a simple linear script.

By clarifying the difference between **chapter order** and **execution context**,
Step Into CDSi establishes a foundation for:

- deterministic orchestration,
- accurate interpretation of logic tables,
- and systematic progress toward full conformance.
