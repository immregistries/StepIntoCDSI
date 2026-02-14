# Input Artifacts and External Dependencies in Step Into CDSi

## Purpose

This document describes the **authoritative inputs and external artifacts**
required for Step Into CDSi to function correctly.

The CDSi forecasting engine does not operate in isolation.  
Its correctness depends on alignment with:

- the **CDC CDSi Logic Specification**,  
- structured **supporting data artifacts** (XML, XSD, XLSX, and related resources), and  
- the **FITS test suite** used for validation and conformance measurement.

Understanding the role of each dependency is essential for:

- debugging unexpected behavior,
- validating implementation accuracy,
- and maintaining long-term correctness as specifications evolve.

---

## Overview of Dependency Categories

Step Into CDSi relies on three primary categories of external input:

1. **Normative Logic Definition** — what the system *should do*  
2. **Structured Configuration Data** — the machine-readable form of schedule logic  
3. **Validation Test Cases** — the objective measure of correctness  

Each plays a distinct and non-interchangeable role.

---

## CDC CDSi Logic Specification

### What It Provides

The CDC logic specification defines:

- the **conceptual CDSi model**,  
- orchestration across evaluation, forecasting, and selection phases,  
- business rules governing age, interval, skip, and contraindication logic,  
- and the intended **clinical meaning** of forecasting outcomes.

It is the **primary normative authority** for behavior.

---

### How Step Into CDSi Uses It

Step Into CDSi maps specification content into code through:

- **LogicStep classes** aligned to specification chapters or sub-chapters,  
- embedded **business rule logic** reflecting narrative definitions,  
- replicated **logic tables** used during evaluation and forecasting,  
- and orchestration flow derived from Chapter 4 process descriptions.

When discrepancies occur, the specification is treated as the
**source of truth**, unless ambiguity requires documented interpretation.

---

## Supporting Data Artifacts (XML, XSD, XLSX, etc.)

### What They Provide

Supporting artifacts encode the **structured schedule configuration** required
for execution, including:

- vaccine series definitions,  
- dose timing constraints,  
- interval rules,  
- contraindications and skip conditions,  
- and other parameterized logic components.

These artifacts transform the specification from **narrative guidance**
into **machine-processable data**.

---

### How Step Into CDSi Uses Them

Step Into CDSi consumes these artifacts to:

- populate internal **domain structures** used during evaluation,  
- drive **logic table execution** without hard-coding schedule content,  
- and maintain alignment with **externally maintained schedule updates**.

Incorrect or outdated supporting data can therefore produce:

- clinically incorrect forecasts,
- FITS failures unrelated to code defects,
- or silent divergence from current recommendations.

For this reason, debugging must always consider **data currency and integrity**.

---

## FITS Test Suite

### What It Provides

The FITS (Forecasting Integrated Test Suite) supplies:

- standardized **patient scenarios**,  
- expected **dose evaluation outcomes**,  
- authoritative **forecast dates and statuses**,  
- and a reproducible mechanism for **conformance verification**.

Unlike the specification or supporting data, FITS functions as the
**objective measurement tool** for correctness.

---

### How Step Into CDSi Uses It

Step Into CDSi integrates FITS to:

- execute **single-case debugging** during development,  
- run **full-suite regression testing** after code changes,  
- compute **execution metrics** such as pass/fail percentages, and  
- confirm progress toward **full CDSi conformance**.

A behavioral change is not considered valid unless it:

- resolves a known failure **and**
- preserves or improves overall FITS performance.

---

## Dependency Interactions

Correct forecasting requires **simultaneous alignment** across all inputs:

- The **logic specification** defines intended behavior.  
- The **supporting data** parameterizes that behavior.  
- The **FITS suite** verifies that implementation and data together
  produce the expected clinical outcome.

Failure in any one layer can manifest as:

- rule misinterpretation (spec issue),
- configuration mismatch (data issue),
- or orchestration defect (code issue).

Effective debugging therefore requires checking **all three domains**.

---

## Versioning and Drift Risks

Because these dependencies evolve independently:

- specification revisions may introduce **new rules or clarifications**,  
- supporting data updates may change **timing or eligibility constraints**,  
- FITS updates may add **new validation expectations**.

If Step Into CDSi does not track versions carefully,  
the system may appear incorrect even when code is unchanged.

Maintainers must therefore:

- document artifact versions in use,
- verify compatibility after updates,
- and re-run FITS validation whenever dependencies change.

---

## Implications for AI-Assisted Debugging

For AI reasoning to be reliable:

- the **specification interpretation** must be explicit in documentation,  
- the **supporting data snapshot** must be known,  
- and the **FITS expectations** must be treated as ground truth for behavior.

Providing these inputs alongside execution logs enables AI tools to:

- distinguish configuration issues from code defects,
- propose targeted fixes,
- and validate improvements systematically.

---

## Relationship to Other Documentation

This document complements:

- **02-spec-to-code-mapping.md** — translation of specification into logic steps  
- **05-test-execution-metrics.md** — validation and regression monitoring  
- **09-project-history-and-technical-debt.md** — context for stabilization work  

Together, they define the **external foundation** on which
Step Into CDSi correctness depends.

---

## Design Intent

The intent of this document is to ensure that maintainers recognize:

> Step Into CDSi correctness is not determined by code alone,
> but by continuous alignment between **specification, data, and validation**.

Maintaining that alignment is essential for achieving and preserving
**trustworthy CDSi forecasting behavior**.
