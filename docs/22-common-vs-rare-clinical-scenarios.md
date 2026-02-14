# Common vs. Rare Clinical Scenarios

## Purpose

This document distinguishes between **common (routine)** and **rare (anomalous)**
vaccination scenarios and explains why this distinction is essential for:

- effective logging design,
- high-quality AI debugging and hypothesis formation,
- and rapid identification of clinically meaningful defects.

In CDSi execution, most patient histories are **predictable and uneventful**.
The true diagnostic value lies in the **uncommon edges** where:

- timing rules are violated,
- products interact unexpectedly,
- or orchestration errors surface.

Making this distinction explicit enables **noise reduction** and
**signal amplification** across the entire debugging workflow.

---

## The Shape of Common Clinical Reality

Most real-world vaccination histories follow a **small number of predictable
temporal and structural patterns**.

### Routine Childhood Timing Landmarks

Childhood visits are commonly organized around memorable ages:

- birth  
- 2 months  
- 4 months  
- 6 months  
- 12 months  
- 18 months  
- later childhood milestones  

These landmarks exist primarily for **operational convenience**:

- easy for parents to remember,
- easy for clinics to schedule,
- aligned with routine well-child visits.

They are **not biologically magical dates**.

In reality:

- doses are often valid **before** the landmark age,
- and usually valid **after** it as well.
  
For example:

- a “2-month” vaccination may be valid as early as **6 weeks**.

---

### Grace Periods and Operational Flexibility

Clinical schedules include small allowances such as:

- the **4-day grace period** for minimum age or interval validity.

This grace period is:

- operational rather than biological,
- designed to tolerate real-world variation,
- frequently exercised in conformance testing.

Because of this, test cases often probe:

- the exact boundary between **valid and invalid** timing.

These boundary cases are **rare clinically** but **critical logically**.

---

### Routine Completion and Boosters

Common, low-signal scenarios include:

- valid dose spacing,
- straightforward series completion,
- routine long-interval boosters,
- extra doses after completion that simply increase protection.

In CDSi:

- extra post-completion doses are usually marked **extraneous**,  
  not invalid.

These events rarely explain:

- FITS failures,
- orchestration defects,
- or incorrect forecasts.

---

## Rare but High-Value Clinical Scenarios

Rare scenarios carry **disproportionate diagnostic importance** because they
exercise the precise logic CDSi was created to protect.

### Minimum Age Violations

- Dose given **too early** to be effective.
- Must not be counted toward protection.

Clinically uncommon, but **critical to detect**.

---

### Interval Violations

- Reinforcing dose given **too soon**.
- May create **false appearance of immunity** if simply counted.

This is one of the most important failure modes CDSi prevents.

---

### Mixed Product Series

- Historical vaccines coexist with newer formulations.
- Components may count differently across schedules.

These histories are:

- legitimate,
- complex,
- and prone to misinterpretation.

---

### Off-Label or Atypical Administration

Includes:

- unusual timing,
- uncommon product usage,
- or atypical clinical pathways.

Rare in routine care, but **highly revealing** during debugging.

---

### Adult Catch-Up Anomalies

Adult immunization histories often include:

- long gaps,
- uncertain childhood records,
- compressed catch-up schedules.

These differ structurally from childhood series and can expose:

- orchestration assumptions,
- incorrect interval handling,
- or faulty completion logic.

---

## Additional Sources of Complexity

### Live Vaccine Spacing Constraints

Live vaccines:

- actively stimulate immune response,
- must not be given too close together  
  (unless administered on the same day).

Violations are uncommon clinically but **important for correctness**.

---

### Combination Vaccines

Single injections may:

- satisfy multiple antigen schedules simultaneously.

Usually:

- each component counts toward its respective schedule.

But sometimes:

- one component is valid,
- another is not yet valid due to interval timing.

This creates **counter-intuitive but valid CDSi outcomes**.

---

### Extra Doses After Completion

After a patient is fully protected:

- additional doses may still be given.

These are:

- not invalid,
- simply **extra**.

Reasons include:

- uncertain immune response,
- clinical caution,
- or incomplete history.

CDSi correctly treats these as:

> **non-contributory but harmless**.

---

## Why Rare Scenarios Matter Most for Debugging

From an engineering and AI perspective:

- **common paths confirm stability**,  
- **rare paths reveal correctness**.

Most CDS defects surface only when:

- timing boundaries are crossed,
- histories are irregular,
- or logic branches are stressed.

Therefore:

> Rare scenarios carry the **highest informational value**.

---

## Implications for Logging Strategy

Effective observability should reflect clinical rarity.

### Low-Signal (Compress or Suppress)

- routine valid spacing  
- predictable completion  
- standard boosters  
- harmless extra doses  

Verbose logging here creates **noise without insight**.

---

### High-Signal (Expand and Highlight)

- invalid timing  
- grace-period boundary decisions  
- mixed product interpretation  
- live-vaccine spacing conflicts  
- catch-up irregularities  

These deserve:

- higher log levels,
- clearer trace visibility,
- and potential alert semantics.

---

## Implications for AI Reasoning

AI debugging quality improves when attention follows **clinical rarity**.

### AI Should Compress

- nominal valid histories,
- predictable schedule flow,
- routine booster logic.

---

### AI Should Prioritize

- invalidity determinations,
- temporal boundary crossings,
- unusual product combinations,
- adult catch-up logic,
- disagreement with FITS expectations.

This prioritization produces:

- faster root-cause discovery,
- fewer hallucinated explanations,
- and higher-confidence fixes.

---

## Relationship to Other Documentation

This document extends:

- `18-fundamental-clinical-patterns-in-immunization-schedules.md`
- `19-dose-validity-intuition-vs-strict-rule-validity.md`
- `20-temporal-logic-intuition-in-vaccination.md`

Together, these define:

- what is **normal**,  
- what is **meaningful**, and  
- where debugging attention should focus.

---

## Design Intent

The intent of this document is to encode a practical debugging truth:

> **Common scenarios confirm stability.  
> Rare scenarios reveal correctness.**

By aligning:

- logging visibility,
- AI attention,
- and developer focus  

with **clinical rarity**, Step Into CDSi gains:

- clearer diagnostics,
- reduced noise,
- and faster convergence toward correct forecasting behavior.
