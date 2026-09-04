# Cross-cutting notes (Phase 21)

This file exists for exactly one thing `step-tests/status.yaml` and the
`logic-spec/versions/<v>/findings/` system don't cover: a Role A or Role B
session sometimes discovers something that isn't really about the one unit
it was assigned - a shared framework class, a domain object several steps
depend on, or a fact (like an accumulating value nothing ever resets) whose
consequences could show up in units nobody has tested yet. Filing that only
in the unit's own `status.yaml` notes buries it where only whoever
eventually works on *that* unit would ever see it.

This is not a finding (no classification, no confirmed/open lifecycle - see
`cdsi-reference/README.md`'s "Reporting an ambiguity or a suspected
mismatch") and it is not a substitute for a unit's own `status.yaml` notes -
record the full detail there first, then add a short entry here **only**
when the issue's blast radius plausibly extends beyond the one unit that
found it. Add a new dated entry rather than editing an old one; update an
entry's **Status** line in place once it's resolved instead of deleting it -
the history of what was known and when is worth keeping.

**Why this file matters for planning:** Phase 21's design runs Role A
across every unit before any Role B fixing starts, precisely so fixes can
be sequenced with the full picture in hand rather than in unit-number
order. A framework-level defect fixed early can retroactively resolve red
tests in units nobody has even written yet; fixing units narrowly in order
first risks re-discovering the same root cause repeatedly, or - worse -
papering over it per-unit instead of at its source. Before Role B execution
begins in earnest, the project owner reviews this file together with every
unit whose `status.yaml` shows `blocked_category: upstream_step_defect`, to
decide a deliberate order: shared/foundational fixes first, narrow
single-unit fixes after. That sequencing decision itself isn't written yet
- this file is the evidence base it will draw from, not the plan itself.

---

## 2026-09-04 - `LogicTable.evaluate()` does not stop at the first matching rule column

**Discovered while testing:** 6.10 Satisfy Target Dose (`SatisfyTargetDoseTest`, commit `e7d88bd`)

**Affected component:** `cdsi-engine/src/main/java/org/openimmunizationsoftware/cdsi/core/logic/items/LogicTable.java`, `evaluate()` - the shared decision-table engine every step's `LogicTable`/`LTInnerSet`/etc. subclass uses, not something specific to 6.10.

**What's wrong:** `evaluate()` loops over every rule column and calls `perform()` on every column that validates, rather than stopping at the first (or otherwise picking exactly one). The source carries its own commented-out safety net - `// throw new IllegalStateException("Can only have 1 valid column in a logic table found: " + validColumnCount);` - immediately after counting how many columns validated, so more-than-one-column-matches was noticed at some point and silenced rather than fixed. When more than one column validates, whichever comes last in table order wins, silently overwriting whatever the earlier matching column's outcome had just set.

**Confirmed live in 6.10:** Table 6-31's age condition is `ANY` for the interval/conflict/vaccine outcome columns, so a dose already `EXTRANEOUS` on age can also independently satisfy one of those other columns. Rule 2's `EXTRANEOUS` outcome runs, then whichever of Rules 4/5/6 also matched overwrites it with `NOT_VALID`. Checked against the resolved 4.65-508 release: 92 of 506 series doses can reach 6.4's Extraneous outcome at all, and 91 of those 92 also carry an allowable vaccine - so most extraneous-eligible doses are exposed. Not observable via FITS, which records no per-dose expected evaluation statuses.

**Known affected units:** 6.10 (confirmed, 3 red tests).

**Suspected but not checked:** any other `LogicTable` whose rule columns are not fully mutually exclusive (i.e. use `ANY` generously across more than one outcome column for the same input). Nothing about this pattern is unique to Table 6-31; every completed unit's own decision table would need a specific re-check for overlapping columns to rule this in or out for it, not just this note. Worth checking during Role B for every unit *before* assuming its own tests fully characterize its behavior.

**Suggested handling:** a fix at `LogicTable.evaluate()` (stop at the first valid column, or make the "more than one column validated" case an enforced error) is a single change with wide reach across the engine - it's high-leverage precisely because it can't be fixed per-unit. Recommend the project owner decide whether to fix this before or alongside the first per-unit Role B session that depends on it, rather than working around it inside `SatisfyTargetDose` alone.

**Status:** open, not yet fixed, not yet a formal finding.

---

## 2026-09-03 - `ConditionalSkip` has no way to represent a "context", and the loader keeps only the last one

**Discovered while testing:** 6.2 Evaluate Conditional Skip (`EvaluateConditionalSkipForEvaluationTest`, commit `b01dd37`)

**Affected component:** `cdsi-engine/src/main/java/org/openimmunizationsoftware/cdsi/core/domain/ConditionalSkip.java` (no context field at all) and `SeriesDose` (holds exactly one `ConditionalSkip`, `setConditionalSkip` overwrites); `DataModelLoader`'s `<conditionalSkip>` parsing calls `setConditionalSkip` once per element with no accumulation.

**What's wrong:** the specification requires 6.2 to use only conditional-skip instances whose context is "Evaluation or Both" - but there's no context to check. In the bundled Supporting Data, 67 series doses define two `<conditionalSkip>` elements (an Evaluation-or-Both one followed by a Forecast one); the loader keeps only the last one parsed, so the Forecast-only instance wins for all 67 and 6.2 never sees the one it's required to use.

**Known affected units:** 6.2 (confirmed, part of its 8 red tests). **7.1** ("Evaluate Conditional Skip" for the forecast context) shares the same `EvaluateConditionalSkip` base class and the same domain gap - not yet tested as of this note, but the gap will very likely reproduce there too, from the Forecast side instead of the Evaluation side.

**Suggested handling:** the fix belongs in the domain model (`ConditionalSkip` needs a context field) and the loader (accumulate rather than overwrite), not in either `EvaluateConditionalSkipForEvaluation` or `EvaluateConditionalSkipForForecast` individually. Worth fixing once, before or alongside whichever of 6.2/7.1 is tackled first in Role B, rather than twice.

**Status:** open, not yet fixed, not yet a formal finding.

---

## 2026-09-02 - Patient series scores accumulate across a whole assessment and are never reset

**Discovered while testing:** 8.6 No Valid Doses (`NoValidDosesCompletableTest`, commit `9fd975c`, during the SPEC-4.6-0007 investigation - not a Phase 21 Role A session, but the same kind of cross-cutting observation belongs here regardless of which workflow surfaced it)

**Affected component:** `PatientSeries.addScore`/`setScorePatientSeriesScore` and every step that calls `incPatientScoreSeries`/`descPatientScoreSeries` on it (at least `NoValidDoses`, 8.6) - the score field itself, not any one step's logic.

**What's wrong:** nothing in `cdsi-engine` ever resets a patient series' score between selections. It accumulates monotonically across all ~24,000 `NoValidDoses` invocations observed in one full-suite run, though Table 8-11 reads as describing a per-selection score. Flagged by the SPEC-4.6-0007 investigation as possibly mattering more than the always-increments defect it was actually sent to fix, but not itself investigated further at the time.

**Known affected units:** none yet directly tested - Chapter 8 (8.1-8.9) hasn't had its Role A pass yet as of this note. Flagging now so whoever does 8.6, 8.7 (`SelectPrioritizedPatientSeries`, which reads the score to pick a winner) or 8.9 (`DetermineBestPatientSeries`) checks this deliberately rather than re-discovering it piecemeal.

**Suggested handling:** needs its own investigation to confirm materiality (does the accumulation actually change which patient series wins a selection in practice, the way SPEC-4.6-0007's own fix turned out not to) before deciding whether it's worth fixing at all. Not yet confirmed as a real defect - recorded here as a flagged risk to check when Chapter 8's units are reached, not as an established fact the way the two entries above are.

**Status:** open, unconfirmed, not yet investigated further.
