# Clinical Plausibility as a Debugging Heuristic

## Purpose

This document describes how **clinical intuition** can be used as a rapid,
high-value debugging heuristic when evaluating CDSi execution traces.

While CDSi is a **deterministic rule engine**, many incorrect outcomes can be
identified immediately by asking a simpler question:

> *“Does this forecast make clinical sense?”*

Clinical plausibility acts as a **first-pass filter** that can:

- expose orchestration defects quickly,
- detect impossible temporal relationships,
- and guide AI or developer attention toward the true root cause.

This is not a replacement for strict rule validation.  
It is an **early warning system** for logical impossibility.

---

## Core Principle

Correct CDSi output should always be:

> **Temporally consistent, biologically reasonable, and clinically interpretable.**

When any of these fail, a defect is highly likely.

---

## Impossible or Highly Suspicious Forecast Patterns

### Recommendations Before Date of Birth

A recommendation date **before the patient’s DOB** is always invalid.

This indicates:

- corrupted temporal arithmetic,
- incorrect reference-date selection,
- or broken orchestration flow.

This is a **hard impossibility**, not a clinical edge case.

---

### Recommendations Before an Already-Given Dose

A forecast recommending a dose:

- **earlier than the most recent administered dose**, or  
- **on the exact same date as a prior dose**  

is almost always incorrect.

In normal clinical flow:

- the next dose should appear **weeks or months later**.

#### Rare Exception

Certain substitutions (e.g., **Td → Tdap replacement need**) may justify:

- **immediate recommendation**.

These are:

- rare,
- explicit in rules,
- and should be clearly explainable in logs.

If not explainable → treat as a defect.

---

### Earliest Date After Recommended Date

Clinical meaning requires:

- **Earliest Date ≤ Recommended Date**

If the recommended date occurs **before** the earliest date:

- the forecast is logically inconsistent,
- and cannot be communicated to clinicians or patients.

This is a strong debugging signal.

---

### Forecasts Before Birth or Before History Exists

Forecast dates must never occur:

- before **DOB**,  
- before **first relevant vaccination**,  
- or before **reference temporal anchors** used by the series.

Violations indicate:

- broken date propagation,
- null reference handling errors,
- or incorrect target-dose progression.

---

## Expected Temporal Shape of Correct Forecasts

Clinical plausibility is often visible in the **pattern** of dates.

### Even Spacing from Birth or Prior Dose

Typical behavior:

- **Recommended dates** align to **months or years** from DOB or series logic.
- **Earliest dates** align to **weeks or days** from the **last valid dose**.

This creates recognizable structure:

- recommendations feel **calendar-based**,  
- earliest dates feel **interval-based**.

Loss of this structure suggests:

- incorrect reference dose,
- broken interval computation,
- or mis-selected patient series.

---

### Separation Between Last Dose and Next Recommendation

Normal expectation:

- measurable **time gap** between doses,
- reflecting immune response development.

Therefore:

> A next recommendation should rarely appear  
> **immediately after** a valid administered dose.

If it does:

- verify substitution rules,
- confirm catch-up logic,
- otherwise suspect defect.

---

## Communication Semantics of Forecast Dates

Understanding **who each date is for** clarifies debugging expectations.

### Recommended Date — Patient-Facing Guidance

Represents:

- the **clinically appropriate return time**,
- aligned to recognizable calendar intervals.

Used for:

- scheduling,
- reminders,
- patient communication.

Therefore it should feel:

> **predictable, explainable, and human-readable.**

---

### Earliest Date — Clinician Flexibility Boundary

Represents:

- the **minimum safe moment** a dose can count as valid.

Often:

- earlier than the recommended date,
- tied to strict interval math.

Used by:

- clinicians making real-time decisions,
- catch-up situations,
- scheduling constraints.

---

### Grace Period — Invisible Operational Tolerance

The grace period:

- is **never communicated**,
- exists only for **retrospective validation**,
- smooths real-world timing variation.

Therefore:

- it should **not appear** in patient-visible reasoning,
- but must be **respected in validity logic**.

Unexpected visibility suggests:

- logging leakage,
- or conceptual confusion in orchestration.

---

## Fast Plausibility Checks for Debugging

Before deep trace analysis, ask:

1. **Any date before DOB?** → immediate defect.
2. **Recommendation before last dose?** → almost always wrong.
3. **Recommended < Earliest?** → logical impossibility.
4. **No temporal gap after valid dose?** → suspicious unless explicit rule.
5. **Dates lack month/year structure?** → wrong reference anchor.
6. **Intervals not tied to last valid dose?** → target-dose progression error.

These checks provide:

- **rapid triage**,  
- **high defect detection**,  
- **minimal cognitive cost**.

---

## Role in AI-Assisted Debugging

Clinical plausibility is especially powerful for AI systems because it:

- requires **pattern recognition**, not rule memorization,
- detects **impossible states** quickly,
- narrows search space for deterministic root causes.

### AI Should Treat as High-Confidence Signals

- date before DOB  
- reversed earliest/recommended ordering  
- zero interval after valid dose  
- structurally implausible spacing  

These strongly imply:

> **orchestration or temporal computation defects**,  
> not minor rule interpretation differences.

---

## Relationship to Other Documents

This heuristic builds directly on:

- `20-temporal-logic-intuition-in-vaccination.md`
- `22-common-vs-rare-clinical-scenarios.md`
- `19-dose-validity-intuition-vs-strict-rule-validity.md`

Together they define:

- **what is temporally normal**,  
- **what is clinically rare**,  
- **what is logically impossible**.

---

## Design Intent

The goal of this document is to encode a simple but powerful truth:

> **Many CDS defects are clinically obvious  
> before they are logically understood.**

By formalizing **clinical plausibility** as a debugging heuristic:

- developers detect failures faster,
- AI systems reason more safely,
- and deterministic correctness becomes easier to reach.
