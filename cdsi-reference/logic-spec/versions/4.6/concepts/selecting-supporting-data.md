# Selecting Supporting Data

> **Review status:** draft.

## What this covers

How the specification decides *which version* of a Supporting Data rule applies to a given evaluation or forecast, when the underlying clinical recommendation has changed over time. This is a distinct question from "how is a rule evaluated" (Chapters 4-9's job) - it's "which instance of the rule is even in effect for this patient's dose." Source: Logic Specification for ACIP Recommendations v4.6, section 3.3 "Selecting Supporting Data" (pages 25-26), Tables 3-4/3-5, Figure 3-2.

## Explanation

**[SPEC]** When a clinical recommendation changes, older administered doses aren't always automatically re-evaluated against the new rule - "if a recommendation changed the minimum interval from 6 months to 4 months, previously administered doses that met the 6 month interval requirement are still considered valid when the new 4 month interval is applied." Most changes are simple replacements with no history retained. But some changes are explicitly *not* retroactive: the spec's own worked example is the 12/16/2016 ACIP HPV change that tightened the absolute minimum interval between doses 1 and 3 from 16 weeks to "5 months minus 4 days" - a dose given before that date only had to meet the old 16-week rule; a dose given on or after had to meet the new one.

**[SPEC]** The mechanism for this is Effective Date / Cessation Date on individual Supporting Data "Logical Component" instances (only some component types use them - Age, Preferable Interval, Allowable Interval, and Conditional Skip do; Preferable Vaccine, Allowable Vaccine, Inadvertent Vaccine, Recurring Dose, and Seasonal Recommendation do not and are simply always selected). Table 3-4/3-5's rule: a component instance is relevant if the **anchor date** (the administration date for an evaluation, or the assessment date for a forecast) falls between its Effective and Cessation Dates - defaulting to 01/01/1900 and 12/31/2999 respectively when unvalued, so an instance with no dates set is always relevant. This is exactly the same "assumed value" pattern seen throughout Chapters 6-9's own attribute tables (see e.g. [6.4 Evaluate Age](../steps/06-04-evaluate-age/index.md)'s Table 6-14) - not a coincidence, the same general convention applies here one level up, to selecting *which* Supporting Data to read in the first place.

**[SPEC]** A second, orthogonal kind of selection applies specifically to Conditional Skip logical components: they're also tagged with a **context** - Evaluation (used only in [6.2](../steps/06-02-evaluate-conditional-skip/index.md)), Forecast (used only in [7.1](../steps/07-01-evaluate-conditional-skip/index.md)), Both, or n/a (never applies). Since 6.2 and 7.1 share one implementation class (documented in both those step packages), this context tag is presumably how a single `ConditionalSkip` supporting-data instance can be told apart for its two different callers - see Open Questions.

## Where it applies

- Every step whose business rules read a Supporting Data component with Effective/Cessation Dates - the Age-based rules in [6.4](../steps/06-04-evaluate-age/index.md), the interval rules in [6.5](../steps/06-05-evaluate-preferable-interval/index.md)/[6.6](../steps/06-06-evaluate-allowable-interval/index.md), and the conditional-skip logic in [6.2](../steps/06-02-evaluate-conditional-skip/index.md)/[7.1](../steps/07-01-evaluate-conditional-skip/index.md) all implicitly depend on this selection having already happened correctly before their own attribute tables get populated.
- [date-calculations.md](date-calculations.md) - a related but separate concept: once the *right* Supporting Data instance is selected, its own values still need date arithmetic applied to them.

## Open questions

- **[IMPLEMENTATION]**, unresolved: `cdsi-engine`'s `Age`, `AllowableInterval`, and `Interval` domain classes (`core/domain/`) do expose `getEffectiveDate()`/`getCessationDate()`-style accessors (confirmed by direct search), so the *data* these rules need is present. However, no literal `RELEVANT-1` or `RELEVANT-2` string (Table 3-5's own business rule IDs) was found anywhere in `cdsi-engine`'s source - the same "rule ID isn't traceable as a literal string in code" situation already seen for 9.1's `FORECASTVG-*` rules. Whether the effective/cessation filtering described here is actually implemented (perhaps inline, without carrying the rule ID as a label) or genuinely missing wasn't determined in this pass - flagged as **found, not fixed, not resolved**, for a future pass to trace properly rather than guessed at here.
- Whether the Conditional Skip "context" tag (Evaluation/Forecast/Both/n/a) is read anywhere in `EvaluateConditionalSkip`/`ConditionalSkipType` to filter which supporting-data instances apply to 6.2 versus 7.1 wasn't traced in this pass either - worth checking when 6.2/7.1's shared-class relationship gets a closer look.
