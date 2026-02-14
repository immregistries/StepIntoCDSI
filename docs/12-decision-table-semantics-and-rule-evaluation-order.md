# Decision Table Semantics and Rule Evaluation Order in Step Into CDSi

## Purpose

This document explains how **decision tables** in the CDC CDSi specification are
interpreted and executed within Step Into CDSi, with particular attention to:

- rule semantics,
- evaluation order,
- precedence and default behavior,
- and how table outcomes influence orchestration.

Correct understanding of decision tables is essential because many CDSi clinical
determinations are encoded **entirely within table logic** rather than prose.

Misinterpreting table semantics can therefore produce **clinically incorrect
forecasts even when code appears structurally correct**.

---

## Decision Tables as the Core Logic Mechanism

Within CDSi, decision tables express:

- conditional clinical logic,
- mutually exclusive rule outcomes,
- and deterministic mappings from **inputs → results**.

Each table typically contains:

- **conditions** (age, interval, vaccine type, status, etc.),
- **answers or states** derived from those conditions,
- **rules** combining multiple answers,
- and **outcomes** that affect:
  - dose validity,
  - skip behavior,
  - contraindication handling,
  - or forecast timing.

Because of this structure:

> Decision tables represent the **primary executable meaning** of CDSi logic.

---

## Determinism and Exclusivity

CDSi decision tables are intended to be:

- **deterministic** — the same inputs must always yield the same outcome.
- **logically exclusive** — only one rule path should apply for a given state.

If multiple rules appear to match simultaneously, this usually indicates:

- incorrect condition evaluation,
- missing precedence handling,
- or misinterpretation of default behavior.

Such ambiguity is almost always an **implementation defect**, not a valid CDSi state.

---

## Rule Evaluation Order

Although tables appear static, **evaluation order matters** in practice.

Correct execution requires:

1. **All prerequisite inputs must be available**  
   Tables must not execute before:
   - required dates are computed,
   - target dose context is known,
   - administered record context is selected.

2. **Conditions must be evaluated consistently**  
   Partial or null inputs must not silently satisfy conditions.

3. **Rules must be checked in defined precedence order**  
   When tables imply priority (explicitly or structurally),
   evaluation must preserve that ordering.

Failure in any of these steps leads to:

- incorrect dose validity,
- wrong skip determination,
- or invalid forecast timing.

---

## Default Outcomes and Fallback Logic

Many CDSi tables include **implicit defaults**, such as:

- “no rule matched,”
- “condition not applicable,”
- or “treat as not satisfied.”

Step Into CDSi interprets defaults using the following principles:

- Defaults must be **explicitly represented in code**, not assumed.
- A default path must be **traceable in logs**.
- Defaults must never silently override:
  - a valid rule match,
  - or a required clinical constraint.

Unexpected reliance on defaults is frequently a signal of:

- missing prerequisite computation,
- incorrect rule ordering,
- or null propagation from earlier logic.

---

## Multi-Condition Rule Composition

Many CDSi rules depend on **combinations of conditions**, such as:

- age **and** interval,
- vaccine type **and** series context,
- prior evaluation status **and** contraindication state.

Correct handling requires:

- evaluating **all contributing conditions**,
- ensuring consistent truth evaluation,
- and avoiding short-circuit logic that bypasses required checks.

Incorrect composition is a common source of:

- false dose validity,
- premature skips,
- or incorrect forecast eligibility.

---

## Relationship Between Tables and Orchestration

Decision tables do not control execution flow directly.  
Instead:

- tables determine **clinical outcomes**, while
- orchestration determines **when those outcomes matter**.

For example:

- A table may correctly mark a dose as valid,
- but if executed against the **wrong target dose** or **wrong patient series**,
  the final forecast remains incorrect.

Therefore:

> Correct CDSi behavior requires **both**  
> accurate table semantics **and** correct orchestration context.

---

## Common Implementation Pitfalls

### 1. Evaluating Tables Too Early

Running a table before:

- PRDD is known,
- interval dates are calculated,
- or contraindications are determined,

can produce **false defaults** or null outcomes.

---

### 2. Ignoring Rule Precedence

Treating rules as unordered conditions may:

- allow multiple matches,
- select the wrong outcome,
- or bypass exclusion logic.

---

### 3. Silent Null Propagation

If null values:

- satisfy conditions unintentionally, or
- skip required comparisons,

tables may appear to function while producing **incorrect clinical meaning**.

---

### 4. Recomputing Table Logic in Multiple Locations

Duplicating rule logic across LogicSteps can lead to:

- inconsistent outcomes,
- divergence during maintenance,
- and regression after fixes.

Each table should have **one authoritative execution location**.

---

## Observability Requirements

To support deterministic debugging, logs should capture:

- which decision table executed,
- evaluated condition values,
- matched rule identifier,
- and resulting outcome.

Alerts should occur when:

- no rule matches where one is expected,
- multiple rules match simultaneously,
- or required inputs are null.

Without this visibility, FITS failures appear only as:

- date mismatches,
- incorrect statuses,
- or unexplained regressions.

---

## Implications for AI-Assisted Debugging

Decision tables are especially suitable for AI reasoning because they are:

- structured,
- deterministic,
- and traceable.

Given:

- execution logs,
- rule definitions,
- and expected FITS outcomes,

AI can:

- identify incorrect rule selection,
- detect missing prerequisite inputs,
- and propose minimal corrective logic changes.

However, this depends on **faithful table semantics** and
**complete execution observability**.

---

## Relationship to Other Documentation

This document connects closely with:

- **11-processing-model-orchestration.md** — when tables execute  
- **08-business-rule-grouping-strategy.md** — where rules are computed  
- **03-debugging-playbook.md** — tracing failures to rule outcomes  

Together, these describe how CDSi transforms:

- structured rule logic  
→ into deterministic clinical forecasts.

---

## Design Intent

The intent of this document is to reinforce a core CDSi principle:

> Decision tables define the **clinical truth**,  
> but only when evaluated with correct **inputs, order, and context**.

By preserving precise semantics and observability,
Step Into CDSi enables:

- trustworthy rule execution,
- regression-resistant maintenance,
- and reliable AI-assisted conformance improvement.
