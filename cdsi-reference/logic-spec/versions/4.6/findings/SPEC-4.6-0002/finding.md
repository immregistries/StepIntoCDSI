# SPEC-4.6-0002: LogicTable.evaluate() never enforces exactly-one-match

**Status:** open
**Category:** IMPLEMENTATION_MISMATCH

## Evidence

Section 2.11 "Decision Table Overview" (pages 20-22) describes every "Table N-M" decision table as a set of mutually-exclusive rule columns plus an optional default outcome that fires only when none of the explicit rules match - the notation assumes at most one rule column ever matches for a given set of condition answers.

`org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable.evaluate()` contains a safety check for exactly this: it would raise `IllegalStateException` when `validColumnCount != 1 && logicOutcomeDefault == null` - more than one column matched, and there's no default to fall back on. That `throw` is **commented out** in the current source.

As written, if two rule columns both match, `evaluate()` calls `perform()` on both matching outcomes in table order. The later outcome's state changes win, silently, with nothing logged to indicate two rules fired.

## Interpretation

This is a property of the one shared mechanism every decision table in every `LogicStep` is built from ([see concepts/decision-tables.md](../../concepts/decision-tables.md)) - not a one-off gap in a single step's table. It could silently affect any step package whose specific conditions aren't perfectly mutually exclusive for some input combination, and no step package documented so far has been checked against this specifically.

Per the project owner: this check was very likely commented out deliberately to get the processes executing, with the intent to fix it later - consistent with standing direction not to fix logic bugs until the documentation and testing system is complete. Given how many step packages depend on this one class, it's worth prioritizing early in any future fix pass rather than treating it as just one bug among many.

## Affected

- Spec sections: none specific - cross-cutting, applies to every decision table in Chapters 4-9
- Code locations: `org.openimmunizationsoftware.cdsi.core.logic.items.LogicTable`
- FITS cases: none identified in this pass
