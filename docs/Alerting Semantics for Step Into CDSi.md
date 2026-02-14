# Alerting Semantics for Step Into CDSi

## Purpose

This document defines the **alerting model** used by the Step Into CDSi logging system.

Alerts are **non-fatal diagnostic signals** that indicate unexpected, missing, or inconsistent
conditions during CDSi evaluation and forecasting.  
Execution **must continue**, but the alert highlights a likely defect, specification divergence,
or incomplete implementation that may affect correctness.

This system is designed for a **demonstration and conformance-debugging environment**,  
not production runtime monitoring.

---

## Core Principles

1. **No silent failure**
   - Any condition that “should not happen” must emit an alert.

2. **Execution continues**
   - Alerts never stop orchestration or throw runtime exceptions.

3. **Spec expectations drive alerts**
   - Alerts represent divergence from:
     - CDC CDSi logic specification
     - Expected orchestration state
     - Required forecast outputs
     - Internal invariants of the Step Into CDSi model

4. **High signal, low noise**
   - Emit alerts **once at the root cause**, not repeatedly downstream.

5. **Always visible**
   - Alerts must be printed regardless of log verbosity level.

---

## Alert Definition

An **alert** is emitted when:

- The algorithm **expected data or state to exist** but it is missing.
- The system enters an **impossible or inconsistent state**.
- The engine must take a **fallback path** due to missing inputs.
- A **spec-required output** cannot be produced normally.
- A **recovered exception scenario** would otherwise have failed execution.

Alerts indicate:

> “The system continued, but the result may be wrong.”

---

## Alert Categories

### `ALERT.MISSING`

**Definition:**  
Required or expected data/state is absent.

**Examples:**

- Patient Reference Dose Date (PRDD) is null when interval logic requires it.
- No candidate patient series found after filtering.
- Forecast list empty at the end of forecasting.
- Expected AAR, target dose, or evaluation result missing.

**Typical cause:**

- Upstream logic step failed to populate required DataModel fields.
- Incorrect orchestration ordering.
- Table logic not executed or mis-evaluated.

---

### `ALERT.FALLBACK`

**Definition:**  
A non-standard fallback path is used to allow execution to continue.

**Examples:**

- Using **minimum age only** because interval constraints are null.
- Selecting earliest date from **administered dose history** due to missing rule outputs.
- Continuing forecasting with **partial constraint set**.

**Impact:**

- Forecast dates may be **earlier or later than expected**.
- Conformance failures likely.

---

### `ALERT.INVARIANT`

**Definition:**  
Internal state violates a logical invariant but recovery is possible.

**Examples:**

- Iterator position exceeds collection bounds but is corrected.
- Target dose status transition not allowed by orchestration model.
- Multiple mutually exclusive states detected simultaneously.

**Meaning:**

- Indicates **orchestration defect** or **state corruption risk**.

---

### `ALERT.SPECGAP`

**Definition:**  
Specification-required behavior cannot be completed due to missing inputs
or incomplete implementation.

**Examples:**

- Forecast cannot be generated for a patient series that should produce one.
- Required rule/table evaluation not implemented.
- Evidence of immunity or contraindication logic unavailable when needed.

**Meaning:**

- Implementation is **incomplete** or **misaligned with CDC logic specification**.

---

## When to Emit an Alert

Emit an alert **immediately when detected**, especially at:

- End of **Evaluate phase** when required evaluation artifacts are missing.
- Start of **Forecast phase** when prerequisite state is absent.
- End of **Forecast generation** if no valid forecast produced.
- Transition between **major orchestration neighborhoods**:
  - SETUP → EVALUATE  
  - EVALUATE → FORECAST  
  - FORECAST → SERIES SELECTION

Do **not** emit alerts:

- For expected optional nulls.
- For conditions already explained by an earlier alert.
- Inside tight loops where the root cause is unchanged.

---

## Alert Message Structure

Alerts must be **machine-parsable and human readable**.

### Standard Format

ALERT.<TYPE>: <short description>
context:
step=<LogicStepName>
series=<PatientSeriesName | null>
targetDose=<number | null>
aarDate=<date | null>
rule=<RuleId | null>
fallback=<description | none>
impact=<expected effect on forecast or evaluation>


### Example

ALERT.MISSING: Patient Reference Dose Date is null
context:
step=EvaluatePreferableInterval
series=Polio 5-dose series
targetDose=2
aarDate=2019-07-23
rule=CALCDTINT-3
fallback=minimum-age-only constraint used
impact=earliest/recommended dates may be too early


---

## Relationship to Log Levels

Alerts are **independent of verbosity levels**.

- Alerts must always appear in output.
- Alerts may occur alongside any log level:
  - CONTROL
  - STATE
  - REASONING
  - TRACE
  - DUMP

Filtering log verbosity **must never hide alerts**.

---

## Expected Alert Frequency

During development and conformance debugging:

- Alerts are **expected** and guide defect discovery.

As implementation approaches correctness:

- Alerts should trend toward **zero** for valid test cases.
- Persistent alerts indicate:
  - orchestration bug
  - missing rule logic
  - incorrect data propagation
  - spec misunderstanding

---

## Design Intent

The alerting system exists to:

- Expose **silent logical failures** in CDSi orchestration.
- Provide **precise diagnostic anchors** for AI-assisted debugging.
- Enable **systematic convergence toward full CDSi conformance**.

Alerts are therefore a **core correctness signal**,  
not merely a logging convenience.

## Non-Alert Conditions

The following are **not alerts**, even if clinically rare or undesirable:

- Invalid vaccine doses  
- Extra doses beyond completion  
- Catch-up schedules  
- Mixed product histories  
- Off-schedule administration  

These represent **correct CDS engine detection of real-world events**,  
not engine malfunction.

They may generate:

- STATE logs  
- CONTROL transitions  
- REASONING explanations  

but must **never generate ALERTs** unless an internal invariant is violated.
