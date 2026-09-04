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

## 2026-09-04 - Schedule-level Supporting Data is parsed onto `Schedule`, which no logic step reads

**Discovered while testing:** 7.2 Determine Evidence of Immunity
(`DetermineEvidenceOfImmunityTest`)

**Affected component:**
`cdsi-engine/src/main/java/org/openimmunizationsoftware/cdsi/core/data/DataModelLoader.java`
(`readImmunity`, `readContraindications`), `domain/Schedule.java`, and the
never-populated parallel fields `DataModel.immunityList` and
`DataModel.contraindicationList` (plus `Antigen.immunityList`). Not something
specific to 7.2.

**What's wrong:** the loader creates one `Schedule` per
`AntigenSupportingData-*.xml` file and parses each file's `<immunity>` and
`<contraindications>` elements onto it (`schedule.setImmunity(...)`,
`schedule.getContraindicationList().add(...)`). Nothing in
`cdsi-engine/.../logic/` ever reads a `Schedule` - the only readers of
`getScheduleList()`, `Schedule.getImmunity()` and
`Schedule.getContraindicationList()` are `cdsi-web`'s data-model viewer
servlets (`AntigenServlet`, `ScheduleServlet`). Meanwhile `DataModel` exposes
its own `immunityList` and `contraindicationList`, and `Antigen` its own
`immunityList`; `setImmunityList`/`setContraindicationList` are never called by
anything, so all three are permanently empty. The steps that need this data
read the empty ones.

**Confirmed live in 7.2:** all three implemented conditions of Table 7-3 read
`dataModel.getImmunityList().get(0)` and guard on `size() == 0`, so every
condition answers `NO` for every patient in every run, only Rule 5's column can
match, and 7.2 returns "not immune" universally - the birth-date half of the
section is as inert as the clinical-history half that is hardcoded to `NO`. The
data is genuinely present in the release and genuinely parsed: six antigens in
the bundled 4.65-508 release ship a populated `<immunity>` element (HepA and
HepB clinical-history only; Measles, Mumps, Rubella at 01/01/1957 and Varicella
at 01/01/1980 with a birth date as well, each with one to three exclusions).
`DetermineEvidenceOfImmunityTest.theReleasesImmunityElementIsParsedByTheLoader`
(green) invokes `readImmunity` reflectively on the real Measles markup and shows
it parses correctly;
`theParsedImmunityElementReachesWhereSevenTwoLooksForIt` (red) shows it lands
nowhere 7.2 can see. Not observable via FITS, which asserts forecast output
rather than per-series immunity status.

**Updated 2026-09-04, from 7.3's side (`DetermineContraindicationsTest`).** The
prediction that 7.3 "has the same shape" **holds for the routing and only for
the routing** - and where it differs, it differs in the direction of being
worse, so the two sides should not be treated as one symmetric problem the way
6.2/7.1's ConditionalSkip entry can be. What is the same: `readContraindications`
writes each parsed `Contraindication` onto `schedule.getContraindicationList()`
and nowhere else, `DataModel.setContraindicationList` is never called by
anything, and `DetermineContraindications` carries the commented-out
`caContraindicationElements.setInitialValue(dataModel.getContraindicationList().get(0));`
exactly as quoted. Three ways it is **not** the same:

1. **It would not even compile.** `DataModel.contraindicationList` is declared
   `List<Contraindication_TO_BE_REMOVED>`, a different class from the
   `domain.Contraindication` the loader instantiates. That is what the source
   comment means by "cannot be set correctly until 'Contraindication_TO_BE_REMOVED'
   get[s] replaced with 'Contraindication'". So on the contraindication side the
   two parallel fields are not merely one populated and one empty, as with
   immunity - they hold incompatible types, and an unfinished migration sits
   between them. `MedicalHistory.contraindicationSet` (which 7.4's condition 3
   reads) is the same `_TO_BE_REMOVED` type and is likewise never populated.
2. **The loader is lossy, not just misrouted.** `readImmunity` parses its element
   faithfully and only puts it in the wrong place; `readContraindications` reads
   exactly two fields per contraindication - `observationCode` and
   `observationTitle` - and discards `contraindicationText`,
   `contraindicationGuidance`, `beginAge`, `endAge` and the entire
   `<contraindicatedVaccine>` subtree. It also flattens the Supporting Data's
   own `<vaccineGroup>`/`<vaccine>` split into one undifferentiated
   `List<Contraindication>`; the `AntigenContraindication` and
   `VaccineContraindication` subclasses exist in the domain model but are empty
   and never instantiated. Table 7-7 has to tell the two levels apart, Table 7-6's
   fourth condition needs the contraindicated CVX list, and Tables 7-5/7-6's
   undetermined outcomes need the Contraindication Text Description - none of
   which survive loading. So fixing the routing alone would not give 7.3 usable
   data.
3. **The routing is not currently 7.3's binding constraint.** 7.2's decision
   table exists and is starved; 7.3 has no decision table at all
   (`logicTableList` is empty, the class carries a "Write the logic for logic
   tables 7-5 to 7-7" note), so nothing in 7.3 would consume the data even if it
   arrived. Consequently only 6 of `DetermineContraindicationsTest`'s 16 reds are
   attributable to this entry; the rest are the missing decision logic and a
   separate Table 7-4 defect (the assumed Contraindication Begin/End Age Date
   values are swapped - `FUTURE` on begin, `PAST` on end, against the
   specification's 01/01/1900 and 12/31/2999 - which makes the age window empty
   rather than universal for the 387 of 392 release contraindications that
   define no age).

Volume, for sequencing: contraindication data is far more abundant than immunity
data. All 30 antigen files in the bundled 4.65-508 release ship a
`<contraindications>` element, totalling 392 contraindications (250 antigen-level
under `<vaccineGroup>`, 142 vaccine-level under `<vaccine>`, with 329
`<contraindicatedVaccine>` entries between them, every one carrying a `<cvx>`),
against 6 antigens with an `<immunity>` element. Whether 7.4 or any Chapter 8
step depends on schedule-level data has still not been looked at.

**Known affected units:** 7.2 (confirmed, 2 of its 6 red tests -
`theParsedImmunityElementReachesWhereSevenTwoLooksForIt` and, downstream of the
same wiring, `theImmunityElementUsedIsTheOneForThisPatientSeriesTargetDisease`)
and **7.3** (confirmed, 6 of its 16 red tests -
`theParsedContraindicationsReachWhereSevenThreeLooksForThem`,
`aParsedContraindicationCarriesTheTextDescriptionShownToTheClinician`,
`aParsedAntigenContraindicationCarriesTheAgesCalcdtciNeeds`,
`aParsedVaccineContraindicationCarriesItsContraindicatedVaccineTypes`,
`antigenAndVaccineContraindicationsStayDistinguishableAfterLoading`, and
`tableSevenFoursContraindicationElementsAttributeIsFilledFromSupportingData`).

**Suggested handling:** the fix belongs in the loader and the domain model, not
in `DetermineEvidenceOfImmunity` - and the routing choice matters, because 7.2's
own Table 7-2 declares the immunity element as *per target disease* ("for the
given target disease"), which `Antigen.immunityList` models correctly and
`DataModel.immunityList` does not. Since each `Schedule` is already named after
its antigen, the data needed to attach each `Immunity` to its `Antigen` is
present at load time. Fixing only the immunity side would leave 7.3 to
rediscover the same thing, so it is worth deciding the routing once for both.
Note that a narrow "populate `DataModel.immunityList`" fix would make 7.2's
schedule-wide reads start working while cementing the wrong scoping - one
antigen's cutoff applied to every series. On the contraindication side, per the
2026-09-04 update above, a routing fix alone would not be enough and cannot be
done without also deciding what happens to `Contraindication_TO_BE_REMOVED`:
`readContraindications` has to stop discarding most of each element, and it has
to preserve the antigen/vaccine distinction the Supporting Data already encodes.
Sequencing consequence: the immunity side is a routing fix, the contraindication
side is a routing fix plus a loader rewrite plus a type migration - so they are
worth deciding together but are not the same size of job, and 7.3's own missing
decision tables would still have to be written before any of it changes 7.3's
behaviour.

**Status:** open, not yet fixed, not yet a formal finding. Confirmed from the
immunity (2026-09-04) and contraindication (2026-09-04) sides; the two are the
same routing cause with materially different remedies.

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

**What's wrong:** the specification requires 6.2 to use only conditional-skip instances whose context is "Evaluation or Both", and 7.1 to use only those whose context is "Forecast or Both" - but there's no context to check. In the bundled Supporting Data, 67 series doses define two `<conditionalSkip>` elements (an Evaluation-or-Both one followed by a Forecast one); the loader keeps only the last one parsed, so the Forecast-only instance wins for all 67 and 6.2 never sees the one it's required to use.

**Updated 2026-09-04, from 7.1's side (`EvaluateConditionalSkipForForecastTest`).** Full breakdown of the bundled 4.65-508 release, so both directions are on record: 484 `<seriesDose>` elements carry a `<conditionalSkip>` element, but 287 of those are empty placeholders that `DataModelLoader`'s `populated` guard correctly discards, leaving 264 real instances (140 "Both", 57 "Evaluation", 67 "Forecast") spread over 197 series doses. Those 197 break down as: 127 with a single "Both" instance (both units get a usable instance); 54 Evaluation-then-Forecast and 13 Both-then-Forecast (the Forecast instance is retained); and 3 with a single "Evaluation" instance. So the failure is **asymmetric**, not a mirror image. In all 67 two-instance cases the retained instance is the Forecast one, which is exactly the instance 7.1 is supposed to use - 7.1 gets the right instance for the wrong reason, purely by document order, with no filtering involved. What 7.1 loses instead is smaller and of the opposite kind: the 3 Evaluation-only series doses, where 7.1 *applies* a conditional skip whose context excludes it (over-application), rather than 6.2's 67 cases of *losing* one it was required to use (under-application). Both symptoms have the same single cause. Practical consequence for sequencing: the loader's document-order accident currently masks the problem on the Forecast side almost entirely, so a partial fix - e.g. making the loader keep the *first* instance instead of the last - would fix 6.2's 67 cases and break 7.1's 67 at the same time. Only accumulating both instances and filtering by context at use time fixes both.

**Known affected units:** 6.2 (confirmed, part of its 8 red tests) and **7.1** (confirmed, its 1 red test - `aConditionalSkipInstanceCarriesTheContextThatDecidesWhetherSevenOneMayUseIt`, the same "can the entry condition even be expressed?" question asked from the Forecast side). `ValidateRecommendation`, the third subclass of the same base class (VALIDATING context), is not in any numbered unit and has not been checked.

**Suggested handling:** the fix belongs in the domain model (`ConditionalSkip` needs a context field) and the loader (accumulate rather than overwrite, then filter by context at use time), not in either `EvaluateConditionalSkipForEvaluation` or `EvaluateConditionalSkipForForecast` individually - and, per the asymmetry above, it cannot be done safely as a loader-only "keep the other one" change. Worth fixing once, before or alongside whichever of 6.2/7.1 is tackled first in Role B, rather than twice.

**Status:** open, not yet fixed, not yet a formal finding. Confirmed from both the Evaluation (2026-09-03) and Forecast (2026-09-04) sides.

---

## 2026-09-02 - Patient series scores accumulate across a whole assessment and are never reset

**Discovered while testing:** 8.6 No Valid Doses (`NoValidDosesCompletableTest`, commit `9fd975c`, during the SPEC-4.6-0007 investigation - not a Phase 21 Role A session, but the same kind of cross-cutting observation belongs here regardless of which workflow surfaced it)

**Affected component:** `PatientSeries.addScore`/`setScorePatientSeriesScore` and every step that calls `incPatientScoreSeries`/`descPatientScoreSeries` on it (at least `NoValidDoses`, 8.6) - the score field itself, not any one step's logic.

**What's wrong:** nothing in `cdsi-engine` ever resets a patient series' score between selections. It accumulates monotonically across all ~24,000 `NoValidDoses` invocations observed in one full-suite run, though Table 8-11 reads as describing a per-selection score. Flagged by the SPEC-4.6-0007 investigation as possibly mattering more than the always-increments defect it was actually sent to fix, but not itself investigated further at the time.

**Known affected units:** none yet directly tested - Chapter 8 (8.1-8.9) hasn't had its Role A pass yet as of this note. Flagging now so whoever does 8.6, 8.7 (`SelectPrioritizedPatientSeries`, which reads the score to pick a winner) or 8.9 (`DetermineBestPatientSeries`) checks this deliberately rather than re-discovering it piecemeal.

**Suggested handling:** needs its own investigation to confirm materiality (does the accumulation actually change which patient series wins a selection in practice, the way SPEC-4.6-0007's own fix turned out not to) before deciding whether it's worth fixing at all. Not yet confirmed as a real defect - recorded here as a flagged risk to check when Chapter 8's units are reached, not as an established fact the way the two entries above are.

**Status:** open, unconfirmed, not yet investigated further.
