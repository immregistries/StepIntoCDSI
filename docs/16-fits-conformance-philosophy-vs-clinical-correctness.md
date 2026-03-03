# FITS Conformance Philosophy vs. Clinical Correctness

## Purpose

This document explains the relationship—and important distinction—between:

- **FITS conformance**, and  
- **true clinical correctness**  

within the Step Into CDSi project.

While the FITS test suite is the **primary operational measure of correctness**
for implementation work, FITS itself is:

- a validation framework,
- not the clinical specification,
- and not a substitute for CDSi rule intent.

Understanding this distinction is essential for:

- safe debugging,
- responsible refactoring,
- and AI-assisted defect resolution.

---

## What FITS Represents

The FITS (Forecasting Integrated Test Suite) provides:

- standardized patient scenarios,
- expected evaluation outcomes,
- authoritative forecast dates and statuses,
- and measurable pass/fail results.

Operationally, FITS serves as the project’s:

> **behavioral oracle for CDSi conformance**.

If Step Into CDSi passes FITS, the implementation is behaving
**consistently with the reference expectation**.

---

## What FITS Does *Not* Represent

Despite its authority, FITS is **not**:

- the CDC CDSi specification itself,
- a complete encoding of all clinical nuance,
- or proof of universal clinical correctness.

Important limitations include:

- finite scenario coverage,
- dependency on specific supporting-data versions,
- and inability to represent every real-world edge case.

Therefore:

> Passing FITS is necessary for conformance,  
> but not sufficient to prove universal clinical truth.

---

## Why FITS Is Still the Primary Target

Even with limitations, FITS remains the **correct operational goal**
for Step Into CDSi because it provides:

- objective measurement,
- reproducible regression detection,
- shared expectations across implementations,
- and deterministic validation for AI-assisted fixes.

Without FITS:

- correctness becomes subjective,
- debugging lacks closure,
- and regressions become invisible.

Thus, FITS is treated as:

> **the authoritative implementation benchmark**.

---

## Common Misinterpretations

### 1. “If FITS fails, the code must be wrong.”

Not always.  
Failure can result from:

- outdated supporting data,
- incorrect test interpretation,
- or environmental mismatch.

Root cause must be confirmed before changing logic.

---

### 2. “If FITS passes, the system is clinically perfect.”

Also false.  
Passing FITS means:

- behavior matches expected reference outcomes,  
not that every possible patient scenario is correct.

---

### 3. “Fix the failing test directly.”

Dangerous.  
Targeted fixes that ignore:

- orchestration invariants,
- decision-table semantics,
- or supporting-data interpretation  

often create **silent regressions elsewhere**.

---

## Proper Role of FITS in Development

FITS should guide development in three ways:

### Regression Detection

Any behavioral change must be evaluated across:

- the **entire FITS suite**,  
not just a single failing case.

A fix that reduces total pass percentage is not acceptable.

---

### Convergence Measurement

Progress toward correctness is measured by:

- increasing pass rate,
- stabilizing previously fixed cases,
- and eliminating broad regression patterns.

This provides an **objective convergence signal**.

---

### Debugging Anchor

When investigating a defect:

- FITS defines the **expected truth**,  
- execution logs reveal the **actual path**,  
- and comparison between them exposes the **root cause**.

---

## Relationship to Clinical Specification

The hierarchy of authority is:

1. **CDC CDSi Logic Specification** → defines clinical intent  
2. **Supporting Data Artifacts** → encode schedule configuration  
3. **FITS Test Suite** → validates implementation behavior  

FITS therefore operates as:

> **evidence of correct interpretation**,  
not the origin of correctness itself.

---

## Implications for AI-Assisted Debugging

AI must treat FITS as:

- the **ground truth for expected behavior**,  
while still respecting:

- orchestration invariants,
- rule semantics,
- and supporting-data structure.

Safe AI reasoning therefore follows:

1. Use FITS to identify failure.  
2. Trace execution to locate causal defect.  
3. Propose **minimal structural fix**.  
4. Re-run full FITS to confirm improvement.  

Any AI suggestion that:

- bypasses specification logic, or  
- forces output to match FITS artificially  

must be rejected as **structurally unsafe**.

---

## Relationship to Other Documentation

This document complements:

- `05-test-execution-metrics.md` — measuring regression and progress  
- `11-processing-model-orchestration.md` — common systemic failure source  
- `15-separation-of-concerns-in-cdsi-architecture.md` — locating defect layer  

Together, they define the **safe conformance strategy** for Step Into CDSi.

---

## Design Intent

The intent of this document is to establish a disciplined principle:

> FITS is the **operational definition of correctness**  
> for implementation work—  
> but **clinical truth originates in the CDSi specification**.

Maintaining this distinction enables:

- trustworthy debugging,
- regression-safe improvement,
- and reliable AI-assisted convergence toward full CDSi conformance.
