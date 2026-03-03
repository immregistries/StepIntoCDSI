# Overview of Sub-Chapter Looping Structure in CDSi

## Purpose

This document explains the **looping architecture** used by the CDC Clinical Decision Support for Immunizations (CDSi) logic specification and how those loops shape the behavior of the Step Into CDSi implementation.

Understanding these loops is essential for anyone debugging orchestration, evaluation flow, or forecasting correctness. Most defects in Step Into CDSi are not caused by incorrect rule math, but by **incorrect traversal of these looping structures** or improper transitions between them.

---

## The Central Role of Chapter 4

Within the CDSi specification, **Chapter 4 functions as the orchestration layer**.  
It connects the detailed rule logic contained in later chapters and determines:

- which patient series are evaluated,
- how evaluation and forecasting iterate across doses and administered records,
- when forecasting begins and ends,
- and how a final series recommendation is selected.

Because Chapter 4 governs movement between logical phases, even small routing mistakes can invalidate otherwise correct rule evaluations.

---

## The Five Major Looping Domains

Beyond Chapter 4, the CDSi logic is organized into **five repeating logical domains** that operate as structured loops across the patient’s data and vaccination schedule.

These domains correspond to the detailed processing described in later chapters of the specification.  
Each domain focuses on a different dimension of reasoning:

- evaluating administered doses,
- applying conditional logic and skips,
- determining contraindications or immunity,
- computing forecast timing,
- and selecting final recommendations.

Rather than executing once, these domains are revisited repeatedly as the algorithm:

1. Iterates across **relevant patient series**  
2. Steps through **target doses within a series**  
3. Compares against **administered vaccination records (AARs)**  
4. Transitions from **evaluation** into **forecasting**  
5. Repeats until all candidate series are resolved  

This nested looping behavior is the defining structural characteristic of CDSi.

---

## Why Looping Matters for Implementation

In Step Into CDSi, the code mirrors this structure through chained **LogicStep classes** and controlled iteration across the shared **DataModel**.

Correct behavior depends on:

- advancing the **right iterator at the right time**  
  (target dose vs. AAR vs. patient series),
- entering and exiting **evaluation and forecast phases** precisely,
- and ensuring each loop terminates under the same conditions described in the specification.

When looping control is wrong, the system may still produce output, but:

- doses may be counted incorrectly,
- forecasts may be generated from incomplete state,
- or the wrong patient series may be selected as final.

These failures often appear as **date mismatches or incorrect recommendations**, even though individual rule calculations look correct.

---

## Practical Implications for Debugging

When diagnosing failures in Step Into CDSi:

- First examine **loop transitions**, not rule math.
- Verify that:
  - the correct **patient series** is active,
  - the correct **target dose** is being evaluated,
  - the correct **AAR position** is used,
  - and the system moves to **forecasting** only when evaluation is complete.

Most conformance issues trace back to **misaligned loop control** rather than incorrect clinical rules.

---

## Relationship to the Rest of the Documentation

This overview provides the structural foundation for the remaining technical documents in the `docs/` directory.

Subsequent documents describe:

- how specification chapters map to code,
- how debugging should proceed,
- what internal state must be logged,
- and how forecasting correctness is validated.

Together, these documents translate the **narrative CDC specification** into an **operational mental model** suitable for implementation, debugging, and AI-assisted reasoning.
