# Mapping Clinical Intuition to Logging Strategy

## Purpose

This document defines how **clinical meaning** should inform  
**observability design** inside Step Into CDSi.

CDSi is deterministic, but not all execution paths carry equal value  
for debugging or AI reasoning. Clinical intuition helps determine:

- what execution behavior is **expected**,  
- what behavior is **informative**,  
- and what indicates the **engine itself is malfunctioning**.

This chapter forms the bridge between:

> **clinical reality → execution semantics → logging architecture → AI signal prioritization**

---

## The Normal Clinical Path

In routine care, vaccination follows a predictable flow:

1. A dose is administered.  
2. The dose is evaluated.  
3. The dose may be **valid or invalid**.  
4. Valid doses accumulate toward protection.  
5. A **forecast** is generated for the next need.  

Importantly:

> **Invalid clinical events are normal engine behavior.**

Detecting:

- minimum-age violations,  
- short intervals,  
- wrong products,  
- off-schedule doses  

is **correct CDS engine operation**, not a failure.

Therefore:

> The normal execution path — including detection of invalid doses —  
> should produce **minimal logging noise**.

---

## Core Principle: Engine Health, Not Clinical Rarity, Determines Alerts

Logging severity must follow a strict rule:

> **ALERT indicates an engine invariant violation, not a clinical anomaly.**

This separates:

- **clinical correctness** (what happened to the patient)  
from  
- **computational correctness** (whether CDSi executed properly).

### Clinical Anomalies Are Normal Operation

Events such as:

- invalid doses,  
- extra doses,  
- mixed products,  
- catch-up schedules,  
- substitutions,  
- conditional skips  

are **expected CDS engine findings**.

These should be logged only when they:

- change execution **state**, or  
- redirect **control flow**.

They are **not ALERT conditions**.

---

## State Change vs. Control Change vs. Nominal Flow

Logging value comes from **execution significance**, not clinical rarity.

### STATE — Meaningful Data Mutation

Log when the DataModel changes in a way that affects downstream logic:

- dose evaluation becomes VALID / NOT_VALID / EXTRANEOUS  
- PRDD is established  
- target dose status changes  
- forecast object is created or updated  

Nominal confirmations that **do not change state** should not be logged.

---

### CONTROL — Execution Path Transition

Log when execution direction changes:

- advancing to next target dose  
- moving to next AAR  
- switching patient series  
- entering FORECAST neighborhood  
- terminating evaluation loop  

These define the **narrative of computation** and are always high value.

---

### REASONING — Explanation of a State or Control Change

Log the specific rule outcome that **caused** the change:

- interval violation details  
- minimum-age comparison  
- conflict resolution  
- conditional-skip trigger  

If no state or control change occurs, reasoning is usually **low value**.

---

### TRACE — Mechanistic Detail

Includes:

- table row scans  
- CVX comparisons  
- iterator increments  
- intermediate arithmetic  

These are useful only for **deep debugging** and should normally be hidden.

---

## ALERT Semantics: Engine Invariant Violations Only

ALERT is reserved strictly for conditions where:

> **The CDS engine cannot trust its own computation.**

Examples:

- required data missing where the algorithm guarantees presence  
- forecast not produced after forecast-generation phase  
- impossible temporal relationships  
  - recommended date < earliest date  
  - date before DOB  
- iterator or index inconsistency  
- contradictory internal state  
- null reference in a logically mandatory structure  

These indicate:

> **engine malfunction**, not clinical reality.

ALERTs are therefore:

- always visible,  
- independent of verbosity level,  
- primary signals for AI root-cause analysis.

---

## Forecast Generation as the Primary Engine Invariant

After evaluation completes:

> **A forecast must exist.**

Failure to produce one is:

- never a clinical scenario,  
- always an orchestration defect.

Therefore:

> **Missing forecast ⇒ ALERT**, regardless of recovery.

---

## AI Signal Prioritization Derived from Execution Semantics

When logging reflects **engine meaning instead of clinical rarity**,  
AI systems can:

- ignore TRACE noise,  
- follow CONTROL transitions as execution narrative,  
- use STATE mutations as causal anchors,  
- treat ALERTs as high-confidence defects.  

This dramatically improves:

- hypothesis quality,  
- convergence speed,  
- resistance to hallucinated explanations.

---

## Observability Design Principle

The governing rule becomes:

> **Log what changes computation.  
> Alert when computation cannot be trusted.  
> Stay quiet during correct execution — even if the clinic was wrong.**

This keeps CDSi observability aligned with:

- deterministic correctness,  
- debugging efficiency,  
- and safe AI-assisted reasoning.

---

## Relationship to Other Documents

This chapter synthesizes:

- `19-dose-validity-intuition-vs-strict-rule-validity.md`  
- `22-common-vs-rare-clinical-scenarios.md`  
- `23-clinical-plausibility-as-debugging-heuristic.md`  
- `17-ai-assisted-debugging-workflow-for-deterministic-cds-engines.md`  

Together they define the bridge from:

> **clinical truth → deterministic execution → trustworthy observability.**

---

## Design Intent

The central insight is:

> **Effective logging in CDSi is not about clinical rarity.  
> It is about computational trust.**

By grounding observability in **engine invariants and execution meaning**:

- debugging becomes precise,  
- AI reasoning becomes reliable,  
- and deterministic correctness becomes achievable.
