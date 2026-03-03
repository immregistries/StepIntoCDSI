# AI-Assisted Debugging Workflow for Deterministic CDS Engines

## Purpose

This document defines the **AI-assisted debugging workflow** used in Step Into CDSi
to achieve reliable convergence toward full CDSi conformance.

Unlike probabilistic or heuristic clinical systems, CDSi is:

- **rule-based**,  
- **deterministic**, and  
- **fully traceable** through execution state.  

These characteristics make CDSi uniquely well-suited for structured reasoning
with Large Language Models (LLMs), provided that:

- execution traces are complete,
- architectural boundaries are respected,
- and guardrails prevent unsafe rule modification.

This workflow establishes the bridge between:

> **CDSi deterministic logic**  
> and  
> **AI-guided defect discovery and repair**.

---

## Why Deterministic CDS Engines Pair Well with LLM Reasoning

CDSi debugging involves:

- comparing **expected vs. actual outcomes**,
- tracing failures through **explicit rule execution**, and
- identifying **minimal causal defects**.

These tasks align closely with LLM strengths:

- structured reasoning over symbolic traces,
- hypothesis generation from incomplete information,
- and iterative refinement based on feedback.

Because CDSi behavior is deterministic:

- the same input always yields the same trace,
- regression detection is objective (via FITS),
- and convergence can be measured quantitatively.

This makes AI assistance **reliable and testable**, not speculative.

---

## The Convergence Loop

AI-assisted debugging in Step Into CDSi follows a strict iterative cycle:

### 1. Trace

Capture:

- failing FITS case,
- full execution log,
- orchestration state,
- rule outcomes,
- and forecast result.

The trace must be:

- deterministic,
- complete,
- and reproducible.

Without this, AI reasoning is unreliable.

---

### 2. Hypothesis

AI analyzes the trace to propose:

- **where execution diverged** from expectation,
- which layer is responsible:
  - supporting data,
  - rule semantics,
  - or orchestration,
- and the **minimal conceptual defect** explaining the mismatch.

The hypothesis must remain:

- specification-consistent,
- FITS-compatible,
- and architecturally localized.

---

### 3. Minimal Fix

Apply the **smallest possible change** that:

- corrects the identified defect,
- preserves architectural separation,
- and avoids speculative rule rewriting.

Examples:

- correcting iterator advancement,
- fixing null-handling in PRDD calculation,
- restoring correct rule precedence.

Non-minimal fixes are dangerous because they:

- obscure root cause,
- introduce regression risk,
- and reduce explainability.

---

### 4. Regression Validation

Re-execute:

- the **single failing case** (local confirmation),
- then the **entire FITS suite** (global validation).

Outcomes:

- **Pass increase, no regressions** → fix accepted.  
- **Mixed results** → hypothesis incomplete.  
- **Widespread regression** → fix rejected; return to trace.  

Regression validation is the **objective safety boundary**.

---

### 5. Convergence

Repeat the loop until:

- FITS pass rate approaches 100%,
- regression oscillation disappears,
- and remaining failures are:

  - specification ambiguity, or  
  - supporting-data mismatch.  

This defines **practical convergence**.

---

## Guardrails Against Hallucinated Rule Changes

LLMs can generate plausible but **incorrect clinical logic**.
Strict guardrails are therefore required.

### Never Allow AI To:

- invent new CDSi rules,
- reinterpret decision tables without evidence,
- bypass orchestration invariants,
- or “force” outputs to match FITS.

Any such change is **structurally unsafe**.

---

### Always Require AI To:

- reference execution trace evidence,
- respect CDC specification semantics,
- preserve separation of concerns,
- and propose **minimal deterministic fixes**.

If a fix cannot be justified through:

- trace → rule → orchestration reasoning,

it must be rejected.

---

## Architectural Safety Model for AI Changes

All AI-suggested fixes must be classifiable as:

### Supporting-Data Alignment

- correcting configuration interpretation,
- updating effective-date handling,
- resolving schedule mismatch.

---

### Rule-Semantics Correction

- fixing condition evaluation,
- restoring rule precedence,
- correcting status propagation.

---

### Orchestration Repair

- iterator advancement,
- phase transition timing,
- best-series selection logic.

If a change does not clearly belong to **one layer**,  
it is likely unsafe.

---

## Observability Requirements for AI Debugging

Reliable AI assistance requires:

- structured logging levels,
- explicit state transitions,
- rule evaluation visibility,
- PRDD and interval traceability,
- and FITS comparison output.

Without these, AI reasoning devolves into:

- speculation,
- pattern guessing,
- or hallucinated fixes.

Observability is therefore a **precondition for AI safety**.

---

## Human–AI Collaboration Model

In Step Into CDSi:

- **AI proposes hypotheses and minimal fixes**,  
- **human maintainer validates architectural correctness**,  
- **FITS provides objective arbitration**.

This creates a **closed-loop reasoning system** where:

- neither AI nor human acts alone,
- correctness is externally measured,
- and convergence is measurable.

---

## Relationship to Other Documentation

This workflow integrates concepts from:

- `03-debugging-playbook.md` — human debugging method  
- `05-test-execution-metrics.md` — regression measurement  
- `11-processing-model-orchestration.md` — systemic defect source  
- `15-separation-of-concerns-in-cdsi-architecture.md` — safe change boundaries  
- `16-fits-conformance-philosophy-vs-clinical-correctness.md` — validation authority  

Together, they define the **AI-safe evolution path** for Step Into CDSi.

---

## Design Intent

The intent of this document is to formalize a disciplined principle:

> AI should **accelerate deterministic debugging**,  
> not replace specification-driven reasoning.

By enforcing:

- trace-based hypotheses,
- minimal structural fixes,
- regression-verified validation,
- and strict architectural guardrails,

Step Into CDSi enables **safe, measurable convergence**
toward full CDSi conformance using AI assistance.
