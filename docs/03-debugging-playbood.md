# CDSi Debugging Playbook for Step Into CDSi

## Purpose

This document describes the **practical, repeatable debugging method** used to identify and resolve conformance failures in the Step Into CDSi implementation.

Rather than attempting to reason about the entire CDSi algorithm at once, effective debugging focuses on **one concrete failing scenario**, traces execution through the logic steps, and works backward from the observed failure to the underlying cause.

This playbook is intended for:

- developers new to the project,
- maintainers investigating regressions,
- and AI-assisted debugging workflows.

---

## Core Debugging Strategy

The most reliable approach to debugging CDSi behavior is:

1. **Select a single FITS test case** that is failing.
2. **Execute that test in Step Into CDSi** using detailed logging or step-through mode.
3. **Compare expected vs. actual outcomes**, focusing on:
   - evaluation status,
   - earliest and recommended forecast dates,
   - selected patient series.
4. **Trace execution backward** from the incorrect result to the first point where
   system behavior diverges from the specification.

This method avoids speculation and keeps debugging grounded in **observable evidence**.

---

## Why Single-Test Debugging Works

CDSi forecasting is governed by:

- nested looping across series, doses, and administered records,
- layered rule evaluation,
- and orchestration defined primarily in Chapter 4.

Because of this complexity:

- fixing multiple failures at once is unreliable,
- broad code changes often introduce regressions,
- and the **root cause of a failure is usually localized** to a specific logic step.

Working one FITS case at a time allows:

- precise identification of divergence,
- controlled verification of fixes,
- and safe progression toward higher conformance.

---

## Using Step Into CDSi for Diagnosis

Step Into CDSi provides two key debugging capabilities:

### Full Forecast Execution

Running a FITS test through the normal forecasting path reveals:

- final evaluation status,
- computed forecast dates,
- and pass/fail comparison against expected results.

This establishes **what is wrong**, but not yet **why**.

---

### Step-Through Logic Execution

The step-through interface allows a human (or AI) to:

- observe each **LogicStep transition**,
- inspect **DataModel state changes**,
- and follow **rule evaluation outcomes** in sequence.

This is the primary mechanism for identifying:

- incorrect iterator movement,
- premature forecasting,
- missing constraint calculations,
- or wrong patient-series selection.

---

## Role of Logic Tables in Debugging

The CDSi specification encodes many clinical decisions in **logic tables**.  
These tables are deterministic and therefore ideal anchors for debugging.

When a failure involves:

- dose validity,
- skip logic,
- contraindications,
- or interval handling,

the first question should be:

> **Did the correct logic table execute, and did it return the expected result?**

If the table output is correct, the defect likely lies in:

- orchestration timing,
- state propagation,
- or loop control.

---

## Special Attention: Date-Heavy Rule Sections

Certain parts of the CDSi specification rely heavily on **date calculations**,  
especially those involving:

- minimum age,
- minimum interval,
- absolute minimum interval,
- and recommended timing.

These areas are particularly prone to:

- null reference propagation,
- incorrect Patient Reference Dose Date (PRDD) handling,
- or fallback logic masking upstream defects.

For this reason, it is often necessary to:

- print the **relevant business rules**,  
- inspect **intermediate date calculations**,  
- and verify **constraint selection logic**.

Section **7.5** of the specification is a notable example where
**explicit visibility into business rule evaluation** is frequently required.

---

## Working Backward from Failure

When the final forecast is incorrect:

1. Identify **which value is wrong**:
   - status,
   - earliest date,
   - recommended date,
   - or selected series.

2. Locate the **logic step that produced that value**.

3. Move backward to determine:
   - which rule supplied the incorrect input,
   - whether required data was missing,
   - and whether orchestration advanced incorrectly.

4. Continue tracing backward until reaching the **first divergence from expected behavior**.  
   That location is the true defect.

---

## Verifying a Fix

After implementing a change:

1. **Re-run the same FITS test** to confirm the original failure is resolved.
2. **Execute the broader FITS suite** to ensure:
   - no regressions were introduced,
   - overall pass rate improves or remains stable.
3. If regressions appear, repeat the debugging process for the new failure.

This iterative loop is the primary mechanism for achieving **near-complete conformance**.

---

## Relationship to Other Documentation

This playbook depends on concepts introduced in:

- **01-overview-subchapter-loops.md** – structural orchestration model  
- **02-spec-to-code-mapping.md** – how specification logic appears in code  

Subsequent documents describe:

- logging expectations,
- execution metrics,
- and domain interpretation challenges.

Together, these form a **systematic methodology for CDSi correctness debugging**.
