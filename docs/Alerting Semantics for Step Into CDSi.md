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
   - Alerts **must never** throw exceptions, stop orchestration, or trigger recovery logic
   - The CDSi engine must always complete forecast generation
   - Missing data triggers safe defaults, not error states
   - Process failures are captured as alerts, not exceptions

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

## Technical Architecture of Alerts

### Alert Emission Layer (Logical vs. Domain)

**Alerts MUST only be emitted from LogicStep subclasses**, never from domain objects.

**Reasoning:**
- The `alert()` method is defined in `LogicStep` and inherited by all subclasses (`SingleAntigenVaccineGroup`, `EvaluateAndForecastAllPatientSeries`, etc.)
- Domain objects (`VaccineGroupForecast`, `PatientSeries`, `Forecast`, etc.) have no `alert()` method
- Domain objects should **silently handle invalid states** by applying sensible defaults
- **Business logic layer** (LogicStep subclasses) is responsible for observing these conditions and alerting upstream consumers

### Pattern: Defensive Handling + Alerting

When a required field is null:

1. **Domain Layer** (e.g., `VaccineGroupForecast.setVaccineGroupStatus()`):
   - Check for null
   - Apply safe default (e.g., `NOT_COMPLETE`)
   - Return/continue silently

2. **Logic Layer** (e.g., `SingleAntigenVaccineGroup.process()`):
   - Before passing data to domain object, check for null
   - If null is found, call `alert(LogLevel.CONTROL, "ALERT.MISSING: ...")` with full context
   - Then pass the value (null or otherwise) to domain object
   - Domain object handles it safely

### Example

```java
// Domain layer - silent fallback
public void setVaccineGroupStatus(PatientSeriesStatus status) {
  if (status == null) {
    // No alert here - just apply default
    setVaccineGroupStatus(VaccineGroupStatus.NOT_COMPLETE);
    return;
  }
  // ... process normally
}

// Logic layer - alert with context, then call domain
PatientSeriesStatus pss = patientSeries.getPatientSeriesStatus();
if (pss == null) {
  alert(LogLevel.CONTROL, 
    "ALERT.MISSING: PatientSeriesStatus is null; " +
    "context: step=SINGLE_ANTIGEN_VACCINE_GROUP, series=" + seriesName + "; " +
    "fallback: defaulting to NOT_COMPLETE");
}
// Domain object handles the null or correct value safely
vgf.setVaccineGroupStatus(pss);
```

### Alert Method Availability

| Layer | Has `alert()` Method | Emits Alerts | Notes |
|-------|---------------------|--------------|-------|
| LogicStep subclasses | ✅ YES | ✅ YES | Direct access to `alert()` method |
| Domain objects | ❌ NO | ❌ NO | No method, should not emit |
| Servlets | ❌ NO | ❌ NO | Consume logs/alerts from LogicStep |
| Utilities | ❌ NO | ❌ NO | Cannot emit alerts; pass context upward |

### Forbidden Patterns

❌ **DO NOT use these patterns:**

```java
// WRONG: System.err.println instead of alert()
System.err.println("ALERT.MISSING: PatientSeriesStatus is null");
dataModel.setStatus(DEFAULT_STATUS);

// WRONG: Throwing exception on missing data
if (patientSeriesStatus == null) {
  throw new NullPointerException("PatientSeriesStatus cannot be null");
}

// WRONG: Try-catch-rethrow that stops execution
try {
  vgf.setVaccineGroupStatus(p.getPatientSeriesStatus());
} catch (NullPointerException e) {
  System.err.println("ALERT: " + e.getMessage());
  throw e;  // STOPS EXECUTION - VIOLATION
}
```

✅ **DO use this pattern:**

```java
// CORRECT: Check, alert, proceed with safe default
PatientSeriesStatus pss = p.getPatientSeriesStatus();
if (pss == null) {
  alert(LogLevel.CONTROL, "ALERT.MISSING: PatientSeriesStatus is null; " +
    "context: step=SINGLE_ANTIGEN_VACCINE_GROUP; " +
    "fallback: defaulting to NOT_COMPLETE; " +
    "impact: vaccine group status may be incorrect");
}
// Pass value (null or valid) to domain object, which applies safe default
vgf.setVaccineGroupStatus(pss);  // Domain layer handles null safely
```

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

Alerts are **independent of verbosity level filtering**.

### Alert Method Signature

```java
public void alert(String message);           // Uses default log level CONTROL
public void alert(LogLevel level, String message);  // Explicit log level
```

### Technical Behavior

- `alert()` methods in `LogicStep` bypass the configured minimum log level
- Alerts are **always emitted to output**, regardless of verbosity filter
- The `LogLevel` parameter is for **categorization** (CONTROL, STATE, etc.), not filtering
- Even if minimum verbosity is set to DUMP, alerts will still appear
- Alerts may be interleaved with STATE, CONTROL, REASONING, TRACE, or DUMP level logs

### Usage Pattern

When emitting alerts from logic steps:

```java
// Alert with explicit log level (typically CONTROL for problem identification)
alert(LogLevel.CONTROL, "ALERT.MISSING: PatientSeriesStatus is null; fallback: ...");

// Alert with method overload (implicit CONTROL level)
alert("ALERT.MISSING: PatientSeriesStatus is null; fallback: ...");
```

### Design Principle

Alerts represent **correctness violations** that must always be visible for debugging.  
They are not subject to normal log level filtering because they indicate the output may be wrong.

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
