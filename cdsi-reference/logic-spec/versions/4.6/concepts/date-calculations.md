# Date Calculations

> **Review status:** draft.

## What this covers

The one calculation pattern underlying nearly every business rule in Chapters 5-9: a reference date (usually date of birth, sometimes another dose's administration date) plus a defined time period yields a calculated date. Source: Logic Specification for ACIP Recommendations v4.6, section 3.4 "Date Calculations" (pages 27-29), Table 3-6 (General Date Rules), Table 3-7 (Logical Component Date Rules), Figure 3-3.

## Explanation

**[SPEC]** Table 3-6 defines six general, component-independent rules for date arithmetic (CALCDT-1 through CALCDT-6): add years by incrementing the year and holding month/day constant; add months by incrementing month (and year, if it rolls over) holding day constant; add weeks/days as total elapsed days; subtract days the same way; when a computed date isn't a real calendar date (e.g. "September 31"), move forward to the first of the next month; and when combining year+month+week/day adjustments in one calculation, apply them in that order - years first, then months, then weeks/days (CALCDT-6). That ordering rule matters: `01/31/2000 + 6 months - 4 days` is defined to mean "add 6 months to 01/31/2000 first, then subtract 4 days from the result" (= 07/27/2000), not some other order that could land on a different date.

**[SPEC]** Table 3-7 is then a large catalog - not a single rule - of every "calculate a specific date from a specific Supporting Data component" business rule in the whole specification: patient age dates (CALCDTAGE-1 through 5), interval reference/threshold dates (CALCDTINT-1 through 9), vaccine conflict dates (CALCDTCONFLICT-1 through 3), contraindication age dates (CALCDTCI-1/2), indication age dates (CALCDTIND-1/2), conditional-skip dates (CALCDTSKIP-3 through 5), preferable/allowable vaccine age dates (CALCDTPREF-1/2, CALCDTALLOW-1/2), and lot-expiration handling (CALCDTLOTEXP-1). Each of these is documented in full where it's actually used (see Where it applies) rather than reproduced here - this concept file exists to explain that they're all instances of the *same* underlying pattern (reference date + `TimePeriod` = calculated date), not to restate ~25 individual rule definitions a second time.

**[IMPLEMENTATION]** `cdsi-engine` implements this pattern generically. `org.openimmunizationsoftware.cdsi.core.domain.datatypes.TimePeriod` is the "amount + type" value (e.g. "6 months") every Age/Interval/Conflict definition is built from; its `getDateFrom(referenceDate)` method (used throughout, e.g. `age.getAbsoluteMinimumAge().getDateFrom(dateOfBirth)` in `EvaluateAge`) is the single place CALCDT-1 through CALCDT-6's general arithmetic actually lives - a step-specific business rule (like CALCDTAGE-1) is then just "call `getDateFrom` with the right `TimePeriod` and the right reference date," not a separate calculation from scratch. `org.openimmunizationsoftware.cdsi.core.logic.concepts.DateRules` is where most of Table 3-7's per-component rules are wired up as named `DateRule<T>` instances (e.g. `DateRules.CALCDTAGE_1`, `CALCDTSKIP_3`) - each one is a small class carrying both its business-rule ID/text (for the structured trace) and the actual `evaluateInternal(dataModel, logicStep, component)` logic, called from the constructor of whichever `LogicStep` needs it.

## Where it applies

Date-calculation business rules appear directly inside dozens of step packages rather than as their own steps - representative examples, not an exhaustive list:

- [6.4 Evaluate Age](../steps/06-04-evaluate-age/index.md) - CALCDTAGE-1/4/5, the clearest single worked example of the whole pattern (reference date = date of birth, `TimePeriod` = the series dose's Age definition).
- [6.5 Evaluate Preferable Interval](../steps/06-05-evaluate-preferable-interval/index.md)/[6.6 Evaluate Allowable Interval](../steps/06-06-evaluate-allowable-interval/index.md) - CALCDTINT-1 through 9, where the *reference* date itself is sometimes another dose's administration date rather than date of birth (CALCDTINT-1/2/8/9 each define a different rule for picking that reference dose).
- [6.7 Evaluate Vaccine Conflict](../steps/06-07-evaluate-vaccine-conflict/index.md) - CALCDTCONFLICT-1/2/3.
- [7.3 Determine Contraindications](../steps/07-03-determine-contraindications/index.md) - CALCDTCI-1/2 (**note**: this step package documents that these two calculations are, unusually, the *only* part of section 7.3 that actually runs - the rest of the step is unimplemented; not this concept's finding to relitigate, just flagged so a reader isn't confused about why 7.3 is cited here despite being otherwise dormant).
- [5.1 Select Relevant Patient Series](../steps/05-01-select-relevant-patient-series/index.md) - CALCDTIND-1/2.
- [6.2 Evaluate Conditional Skip](../steps/06-02-evaluate-conditional-skip/index.md) - CALCDTSKIP-3/4/5.

## Open questions

- CALCDT-1 through CALCDT-6 (the general arithmetic rules) weren't traced to a single specific method by business-rule ID the way the per-component rules in `DateRules.java` are - `TimePeriod.getDateFrom` is clearly where this logic lives functionally, but whether it's labeled/traceable as "CALCDT-1" etc. anywhere (for the structured-trace purposes later phases of this plan care about) wasn't confirmed in this pass.
- CALCDTAGE-2 and CALCDTAGE-3 (latest/earliest recommended age date) are defined in Table 3-7 but were already noted in [6.4](../steps/06-04-evaluate-age/index.md) as absent from Table 6-16 - worth checking Chapter 7 (forecasting, which is where "recommended" as opposed to "minimum/maximum" age would plausibly be used) rather than assuming they're unused.
