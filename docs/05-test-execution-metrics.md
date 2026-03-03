# Test Execution Metrics in Step Into CDSi

## Purpose

This document defines the **standard execution summary and metrics** that must be
produced whenever the FITS test suite is run against the Step Into CDSi
implementation.

Because CDSi behavior emerges from **deeply interconnected orchestration and rule
logic**, even a small code change can:

- fix one failing scenario,
- silently break previously passing scenarios,
- or shift forecast timing across multiple antigens.

For this reason, **quantitative execution metrics** are essential to guide
debugging, validate progress, and prevent regression.

---

## The Role of the FITS Test Suite

The FITS (Forecasting Integrated Test Suite) provides:

- standardized patient scenarios,
- expected evaluation outcomes,
- and authoritative forecast dates.

Running the full FITS suite is the primary mechanism for measuring:

- **clinical correctness**
- **specification conformance**
- **regression impact after code changes**

Individual test debugging is useful for **root-cause discovery**,  
but only **suite-wide metrics** reveal whether the system is truly improving.

---

## Required Summary Block for Every Run

Each execution of the FITS suite should produce a **consistent summary block**
containing, at minimum:

- Total number of test cases executed  
- Number of test cases that **passed**  
- Number of test cases that **failed**  
- Number of test cases that produced **errors or incomplete results**  
- Overall **pass percentage**  

These values provide a **single-glance indicator** of system health and progress.

---

## Why Standardized Metrics Matter

### Detecting Regression

A fix that improves one scenario may:

- alter loop orchestration,
- change constraint reconciliation,
- or affect series prioritization.

Without full-suite metrics, such regressions may go unnoticed until much later.

Execution summaries ensure that:

> **No change is considered successful unless global correctness improves
> or remains stable.**

---

### Guiding Debugging Priorities

Execution metrics help determine:

- whether failures are **isolated** or **systemic**
- whether a recent change affected **many antigens** or only one
- whether the system is **approaching conformance** or drifting further away

This prevents wasted effort debugging:

- edge cases before core orchestration is correct, or
- low-impact failures while major regressions remain.

---

### Supporting Iterative Conformance Improvement

Step Into CDSi progresses toward correctness through **small, verified steps**:

1. Identify a failing FITS case  
2. Diagnose and implement a fix  
3. Re-run the **entire FITS suite**  
4. Confirm:
   - the original failure is resolved  
   - **no new regressions** were introduced  
   - overall **pass percentage improves or holds steady**

Execution metrics are therefore the **primary feedback loop** for development.

---

## Recommended Extended Metrics

In addition to the required summary, more detailed metrics may include:

- Pass/fail counts **per antigen**
- Distribution of failures by:
  - **status mismatch**
  - **earliest date mismatch**
  - **recommended date mismatch**
- Count of runs producing:
  - **alerts**
  - **fallback logic**
  - **missing forecasts**

These extended metrics help isolate:

- orchestration defects affecting many series,
- rule-specific defects affecting one antigen,
- or systemic date-calculation issues.

---

## Stability Expectations

As the implementation matures:

- **Pass percentage should trend upward**
- **Alert frequency should trend toward zero**
- **Regression frequency should decline**

Persistent instability typically indicates:

- incorrect loop control in Chapter 4 orchestration
- incomplete rule implementation
- incorrect DataModel state propagation
- misunderstanding of specification timing rules

Execution metrics therefore act as an **early warning system**
for deeper architectural issues.

---

## Relationship to Debugging Workflow

Execution metrics complement:

- **03-debugging-playbook.md** — root-cause analysis of individual failures  
- **04-what-to-log-debug-tables.md** — visibility into internal state  

Together, they provide both:

- **micro-level insight** (why a test fails), and  
- **macro-level validation** (whether the system is improving).

Both perspectives are required to achieve **reliable CDSi conformance**.

---

## Design Intent

The execution summary is not merely informational.  
It is a **governance mechanism** for correctness.

No behavioral change in Step Into CDSi should be accepted unless:

- it improves a known failure **and**
- preserves or improves **overall FITS performance**.

In this way, execution metrics provide the **objective foundation**
for systematic, trustworthy progress toward full CDSi implementation.
