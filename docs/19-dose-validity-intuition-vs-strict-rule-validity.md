# Dose Validity Intuition vs. Strict Rule Validity

## Purpose

This document explains the difference between:

- **clinical intuition about dose validity**, and  
- **strict CDSi rule-based validity determination**.  

It also describes why **invalid doses carry far more diagnostic value**
than valid doses, and how this principle should guide:

- logging severity design, and  
- AI debugging and triage behavior.  

Understanding this distinction is essential for building a CDS engine that is:

- clinically trustworthy,  
- observable during debugging, and  
- efficient in signal detection.

---

## The Clinical Meaning of Dose Validity

In normal clinical practice:

- **valid doses are expected**.  
- **invalid doses are rare**.  

An invalid dose usually means:

> A vaccination was administered at the **wrong time**.

This represents a **clinical failure condition**, not merely a technical detail.

Because invalid doses may lead to:

- missed protection,
- false assumptions of immunity,
- or delayed corrective care,

their detection is **critical to patient safety**.

CDSi therefore treats dose validity as a **core clinical safeguard**.

---

## Why Invalid Doses Are Rare but Important

Most invalid doses arise from a small set of causes:

### Age Too Young

- The immune system may not respond adequately.  
- Protection cannot be assumed.  

This is one of the most clinically significant timing errors.

---

### Interval Too Short

- The immune system has not had sufficient time to mature its response.  
- A subsequent dose may not function as a true reinforcement.  

If unrecognized, simple **dose counting** may falsely conclude protection.

This is a major real-world risk.

---

### Wrong Vaccine Product or Formulation

- A dose may not match the intended series logic.  
- Protection may differ from expectation.  

Correct interpretation is necessary to avoid:

- overestimating immunity, or  
- recommending unnecessary doses.

---

## Counting Doses vs. Evaluating Validity

In many real-world systems, vaccination status is determined by:

> **Counting the number of doses given**.

This approach works in **most routine cases**, because:

- most doses are valid,
- schedules are usually followed correctly.

However, counting fails when:

- doses are too early,
- intervals are too short,
- or products are mismatched.

In those situations:

> A patient may be assumed protected  
> **when they are not**.

CDSi exists specifically to prevent this failure by:

- evaluating **validity**, not just quantity.

---

## Why Valid Doses Are “Boring”

From a debugging and observability perspective:

- Valid doses follow **expected clinical timing**.  
- They rarely explain CDS engine failures.  
- They provide **low diagnostic signal**.  

Excessive logging of valid behavior leads to:

- noise,
- cognitive overload,
- and reduced ability to detect true problems.

Therefore:

> Valid doses should produce **minimal logging**  
> unless needed for trace reconstruction.

---

## Why Invalid Doses Are “Interesting”

Invalid doses represent:

- rare clinical anomalies,  
- high-impact decision points, and  
- potential patient safety risks.  

Within CDSi logic, they often mark:

- **branching control flow**,  
- **status transitions**, or  
- **forecast timing corrections**.  

Because of this, invalid-dose handling is frequently where:

- orchestration defects surface,  
- rule misinterpretations appear, and  
- FITS failures originate.

Thus:

> Invalid doses are **high-value debugging signals**.

---

## The Special Case of Testing CDS Engines

In clinical reality:

- invalid doses should be **uncommon**.

In CDS engine testing:

- invalid scenarios are **deliberately common**.

This is intentional because a trustworthy CDS engine must:

- detect timing errors reliably,  
- prevent false assumptions of protection, and  
- guide corrective vaccination.

FITS and other validation suites therefore emphasize:

- edge timing conditions,  
- minimum interval violations, and  
- age boundary scenarios.

These are not routine medicine—  
they are **critical correctness tests**.

---

## Implications for Logging Severity Design

Clinical intuition suggests a clear logging hierarchy:

### Low Priority Logging

- Routine valid doses  
- Expected schedule progression  
- Nominal forecast outcomes  

These should remain quiet unless detailed tracing is requested.

---

### High Priority Logging

- Invalid dose detection  
- Minimum age violations  
- Interval violations  
- Product mismatches  
- Status reversals or corrections  

These events deserve:

- elevated log levels,  
- structured trace visibility, and  
- possible alert semantics during debugging.

This aligns observability with **clinical importance**.

---

## Implications for AI Triage Heuristics

AI-assisted debugging should allocate attention according to:

### Low Diagnostic Value

- fully valid histories  
- expected completion patterns  
- routine booster timing  

These should be **compressed or summarized**.

---

### High Diagnostic Value

- invalid dose determinations  
- unexpected validity reversals  
- interval edge cases  
- disagreement with FITS expectations  

These should be **expanded and analyzed first**.

This prioritization mirrors **clinical reasoning**.

---

## Relationship to Other Documentation

This document builds on:

- `18-fundamental-clinical-patterns-in-immunization-schedules.md`  
- `13-target-dose-progression-and-series-state-transitions.md`  
- `17-ai-assisted-debugging-workflow-for-deterministic-cds-engines.md`  

Together, they connect:

- **clinical rarity**,  
- **logical branching**, and  
- **AI attention strategy**.

---

## Design Intent

The intent of this document is to formalize an expert intuition:

> **Invalid doses carry far more meaning than valid ones.**

By embedding this principle into:

- logging design,  
- debugging workflow, and  
- AI reasoning heuristics,  

Step Into CDSi improves its ability to:

- detect real clinical risk,  
- isolate logic defects quickly, and  
- converge safely toward full CDSi correctness.
