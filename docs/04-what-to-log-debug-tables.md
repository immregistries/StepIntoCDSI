# What to Log: Debug Tables and Tracked Variables in Step Into CDSi

## Purpose

This document defines the **core variables and artifacts that must be observable**
when debugging, validating, or demonstrating CDSi behavior in Step Into CDSi.

Because CDSi forecasting is driven by **stateful orchestration across multiple nested loops**,
failures are rarely understandable from final outputs alone.  
Effective diagnosis depends on visibility into a small set of **high-value tracked variables**
that explain how the algorithm reached its result.

This guide identifies those variables and explains **why each one matters**.

---

## The Principle of Targeted Visibility

Debug logging in Step Into CDSi is not intended to capture everything.
Instead, it should surface the **minimal set of state transitions** that allow a developer
(or AI assistant) to reconstruct:

- which patient series was evaluated,
- which dose was considered,
- which administered record was used,
- how forecast constraints were derived,
- and why a final recommendation was chosen.

If these elements are visible, most CDSi defects become diagnosable.

---

## Relevant Patient Series

### What to Track

- All **candidate patient series** identified for the antigen  
- The **active patient series** during evaluation  
- The **final selected patient series** after prioritization  

### Why It Matters

CDSi correctness depends heavily on **series selection logic**.
A forecast may appear clinically incorrect simply because:

- the wrong series was evaluated,
- a valid series was filtered out,
- or prioritization rules selected an unintended default.

Without visibility into patient series progression,  
many downstream errors are impossible to interpret.

---

## Target Doses Within a Series

### What to Track

- Current **target dose number**
- **Status transitions** for each target dose  
  (e.g., NOT_SATISFIED → SATISFIED → SKIPPED)
- Whether evaluation advanced:
  - to the **next AAR**
  - to the **next target dose**
  - or to **forecasting**

### Why It Matters

Most orchestration defects occur at the **target-dose iteration boundary**.

Incorrect advancement can cause:

- doses to be double-counted,
- valid doses to be ignored,
- or forecasting to begin prematurely.

Tracking target-dose movement reveals whether  
the **loop control defined in Chapter 4** is functioning correctly.

---

## Administered Dose Records (AARs)

### What to Track

- Filtered **AAR list** relevant to the antigen  
- Current **AAR position** during evaluation  
- Evaluation outcome for each AAR against each target dose:
  - VALID
  - NOT_VALID
  - SKIPPED
  - or ignored

### Why It Matters

CDSi reasoning is fundamentally a **comparison between administered history and schedule expectations**.

Errors in:

- filtering,
- ordering,
- reuse of AARs across doses,
- or evaluation result propagation

can silently corrupt the entire forecast.

Observing AAR handling is therefore essential.

---

## Patient Birth Date and Derived Age Constraints

### What to Track

- **Patient date of birth**
- Derived **minimum age**, **absolute minimum age**, and **maximum age** dates
- Any **age-based constraint** used in evaluation or forecasting

### Why It Matters

Nearly all CDSi timing logic ultimately traces back to **age-based calculations**.

If birth-date-derived constraints are wrong or missing:

- interval rules may evaluate incorrectly,
- skip logic may misfire,
- forecast dates may drift significantly.

Because these calculations propagate forward,  
early visibility prevents late confusion.

---

## Forecast Constraint Components

### What to Track

For each forecasted dose:

- **Minimum age date**
- **Minimum interval date(s)**
- **Conflict end dates**
- **Seasonal recommendation dates**
- Any **fallback values** used when constraints are missing

### Why It Matters

Forecast dates are not computed from a single rule.  
They are the result of **constraint reconciliation** across multiple sources.

When earliest or recommended dates are wrong,  
the key debugging question is:

> **Which constraint dominated, and why?**

Without logging the candidate constraint set,  
forecast mismatches are opaque.

---

## Generated Forecasts

### What to Track

- Forecast **status** for the series  
- Computed **earliest**, **recommended**, and **past-due** dates  
- Target dose associated with the forecast  
- Whether forecast generation required **fallback behavior**

### Why It Matters

This is the **primary observable output** compared against FITS expectations.

However, final forecasts alone cannot explain failure.  
They must always be interpreted alongside:

- constraint inputs,
- target-dose state,
- and patient-series context.

---

## Best Forecast Selection

### What to Track

- All **forecast results** from evaluated patient series  
- **Prioritization logic** used to choose the best series  
- The **final forecast** surfaced to the user or test harness  

### Why It Matters

Even when individual series forecasts are correct,  
the **wrong final recommendation** may be presented due to:

- prioritization errors,
- default-series handling,
- or filtering mistakes.

Logging only the final forecast hides these defects.  
Visibility into **competing series outcomes** is required.

---

## Debug Tables as a Diagnostic Tool

Collectively, the tracked variables described above form a conceptual **debug table**:

| Domain | Key Questions Answered |
|--------|------------------------|
| Patient Series | Which clinical pathway is active? |
| Target Dose | What dose is being evaluated? |
| AARs | Which historical record is used? |
| Age & Intervals | What timing constraints apply? |
| Forecast Constraints | Why was this date chosen? |
| Final Selection | Why is this the recommendation? |

When these domains are observable,  
most CDSi failures become **mechanically explainable** rather than mysterious.

---

## Relationship to Logging Levels

These tracked variables map naturally onto the **logging level hierarchy**:

- **CONTROL** – series transitions, dose advancement, forecast selection  
- **STATE** – constraint values, evaluation results, forecast dates  
- **REASONING** – rule/table decisions producing those values  

Proper logging therefore enables:

- deterministic debugging,
- reproducible AI analysis,
- and systematic conformance improvement.

---

## Relationship to Other Documentation

This guide complements:

- **03-debugging-playbook.md** – overall debugging workflow  
- **alerting semantics documentation** – handling unexpected or missing state  

Together, they define **what must be visible** in order to achieve  
reliable CDSi debugging and validation in Step Into CDSi.
