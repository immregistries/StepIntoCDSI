# Temporal Logic Intuition in Vaccination

## Purpose

This document summarizes a small set of **temporal clinical invariants**
that govern how immunization schedules behave across age and time.

These principles are already encoded within CDSi rules, but experienced
clinicians recognize them intuitively. Making them explicit helps:

- AI systems detect implausible forecasts,
- developers identify orchestration defects quickly,
- and debugging focus on true temporal inconsistencies rather than noise.

This is not a replacement for CDSi rule logic.  
It is a **sanity-check layer** that sits above deterministic evaluation.

---

## Core Temporal Patterns

Across nearly all vaccination schedules, several consistent truths appear.

### Younger Children Receive More Doses with Shorter Spacing

Early immune systems:

- require repeated reinforcement,
- benefit from closely spaced primary series,
- and must achieve protection quickly due to vulnerability.

Therefore, infant schedules typically show:

- multiple doses,
- short minimum intervals,
- rapid early progression.

If a CDS engine produces:

- very long spacing in infancy, or  
- very few early doses,

the result is likely **clinically implausible**.

---

### Older Patients Require Fewer Doses with Longer Spacing

As immune systems mature:

- fewer reinforcing doses are needed,
- protection lasts longer,
- and spacing between doses increases.

Adolescent and adult schedules therefore trend toward:

- reduced dose counts,
- wider intervals,
- and simplified completion paths.

Forecasts that require:

- dense rapid dosing in adults, or  
- infant-like spacing later in life,

often indicate **temporal logic errors**.

---

### Boosters Occur After Long Periods of Stability

For vaccines requiring lifelong maintenance:

- a primary series establishes protection,
- immunity wanes gradually,
- boosters are spaced years apart.

Thus:

> Long quiet periods followed by a single booster  
> are **normal temporal behavior**.

Frequent short-interval boosters in stable adults are usually **incorrect**.

---

### Minimum Intervals Define Validity Boundaries

Clinically and within CDSi:

- **minimum intervals** determine whether a dose is valid.  
- **recommended intervals** guide optimal timing but do not control validity.

This creates an important hierarchy:

> Minimum timing protects correctness.  
> Recommended timing protects quality.

From a debugging standpoint:

- violations of **minimum** intervals are critical,
- deviations from **recommended** intervals are usually benign.

---

## Why These Temporal Invariants Matter

These patterns allow rapid detection of impossible outcomes.

### Detecting Implausible Forecasts

AI or developers can question results when:

- infants require very few doses,
- adults require tightly spaced primary series,
- boosters appear too frequently,
- or minimum-interval violations are ignored.

Such outcomes often signal:

- iterator errors,
- incorrect PRDD handling,
- or premature phase transitions.

---

### Exposing Orchestration Bugs Quickly

Temporal contradictions frequently arise from:

- incorrect target-dose progression,
- skipped evaluation steps,
- or forecast entry at the wrong time.

Because temporal patterns are **globally visible**,  
they can reveal defects even when:

- individual rule evaluations appear correct.

---

### Supporting Sanity Checks of Execution Traces

During debugging, temporal intuition provides a fast filter:

- Does the sequence of dates “look like” a real schedule?
- Are spacing patterns age-appropriate?
- Is booster timing plausible?

If not, investigation should begin with:

- orchestration,
- interval computation,
- or validity determination.

---

## Relationship to Other Documentation

This document complements:

- `18-fundamental-clinical-patterns-in-immunization-schedules.md`
- `19-dose-validity-intuition-vs-strict-rule-validity.md`
- `13-target-dose-progression-and-series-state-transitions.md`

Together, these describe how:

- **clinical biology**,  
- **dose validity**, and  
- **time progression**  

combine to form trustworthy CDSi behavior.

---

## Design Intent

The intent of this document is to capture a practical insight:

> A small number of **temporal clinical truths** can quickly reveal  
> large classes of CDSi implementation errors.

By elevating these invariants into explicit guidance,  
Step Into CDSi enables:

- faster debugging,
- smarter AI reasoning,
- and more reliable convergence toward correct forecasting.
