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

## 2026-09-05 - Three units share one `EvaluateConditionalSkip`, and 7.6's whole remedy lands inside it

**Discovered while testing:** 7.6 Validate Recommendation
(`ValidateRecommendationTest`)

**Affected component:**
`cdsi-engine/src/main/java/org/openimmunizationsoftware/cdsi/core/logic/EvaluateConditionalSkip.java`
- the shared base class units 6.2, 7.1 and 7.6 all instantiate, specifically its
`caEarliestDate` field and the `VALIDATING` arm of the CONDSKIP-2 `switch` in
its constructor. Not something confined to `ValidateRecommendation`.

**What's wrong:** `ValidateRecommendation` itself is nine lines - a constructor
and a `process()` override - and the specification's whole section 7.6 is
delegated to the shared base class. Two of the three things 7.6 needs from that
base class are stubs, and both are in the base class rather than in 7.6's own
file:

1. `caEarliestDate` is declared, added to `conditionAttributesList`, and never
   constructed - so `getConditionAttributeList()` carries a literal `null` where
   Table 6-4's Earliest Date should be, in all three contexts.
2. The CONDSKIP-2 `switch` reads
   `case VALIDATING: lt.caConditionalSkipReferenceDate.setInitialValue(PAST);`,
   where 7.6.1 says "In CONDSKIP-2, the Earliest Date is used". `PAST` is
   01/01/1900, so every age window and every interval condition answers "No" on
   the date rather than on the merits, for every patient. The `EVALUATE` and
   `FORECAST` arms two lines above it are both correct and both read a real
   date; only the third is a placeholder.

The third thing - `process()` bypassing `evaluateLogicTables()` entirely - *is*
in 7.6's own class and is already recorded in 07-06's Review Findings. The point
of this entry is the other two: they are invisible today because the override
means the tables never run, so fixing the override alone would take 7.6 from
"never checks" to "checks against 01/01/1900 and always answers No", which is the
same behaviour by a longer route.

**Confirmed live in 7.6:** `condskipTwoUsesTheForecastedEarliestDateAsThe
ReferenceDateWhenValidating` (red) reads 01/01/1900 where the fixture's forecast
earliest date is 09/01/2023; `theEarliestDateIsATableSixFourAttributeTheStep
Registers` (red) finds no attribute named "Earliest Date" registered at all; and
`theAgeConditionIsAnsweredAgainstTheForecastedEarliestDate` /
`theIntervalConditionIsAnsweredAgainstTheForecastedEarliestDateToo` (both red)
show the consequence with the tables driven directly, independently of the
`process()` override. Not observable via FITS, which asserts the forecast dates
returned but never re-interrogates them.

**Known affected units:** 7.6 (confirmed, 4 of its 9 red tests). 6.2 and 7.1 are
affected only by (1), and only cosmetically - the null in the attribute list is
already present in their runs, but neither context reads the Earliest Date, so
neither has a red test for it.

**Suggested handling:** this is a sequencing note more than a defect report. 7.6
has no code of its own to fix beyond deleting a `process()` override; everything
else it needs is in a class 6.2 and 7.1 own too, which is exactly the situation
`cdsi-engine/AGENTS.md` tells a Role B session not to resolve unilaterally. The
changes are confined to a branch and a field that only the VALIDATING context
exercises, so they cannot regress 6.2 or 7.1 - but they are still edits to a
shared class, so 7.6's Role B session should be scheduled knowing that, rather
than discovering mid-session that its unit's fix is out of its own unit's scope.
Note the ordering constraint within 7.6 itself: restoring the override without
also fixing the CONDSKIP-2 arm produces a step that runs its tables and still
never skips anything.

**Status:** open, not yet fixed, not yet a formal finding.

---

## 2026-09-05 - FORECASTDTCAN-1 is implemented twice, in two classes, with different candidate dates

**Discovered while testing:** 7.5 Generate Forecast Dates and Recommended
Vaccines (`GenerateForecastDatesAndRecommendedVaccinesTest`)

**Affected component:** `DetermineForecastNeed.computeEarliestDate()` (private)
and `GenerateForecastDatesAndRecommendedVaccines.computeEarliestDate()`
(public) - two independent implementations of the same business rule, in two
different step classes, neither reading the other's result.

**What's wrong:** FORECASTDTCAN-1 defines *one* value, the candidate earliest
date, as "the latest of the following dates" over six candidates. 7.4 holds it
as a real Table 7-9 attribute ("Calculated date (FORECASTDTCAN-1) / Candidate
Earliest Date") and its Table 7-10 Rule 8 gates the whole forecast on it. 7.5's
FORECASTDT-1 then says the forecast's earliest date "must be the candidate
earliest date" - the same date. Instead each class computes its own:

| candidate (FORECASTDTCAN-1) | 7.4 | 7.5 |
| --- | --- | --- |
| minimum age date | yes | yes |
| latest of all minimum interval dates | yes | yes |
| latest of all forecast conflict end dates | no (commented out) | yes |
| seasonal recommendation start date | no (commented out) | yes |
| latest date administered of any inadvertent administration | no | folded into the row below, not distinguished |
| date administered of the most recent vaccine dose administered | no | yes |

So the divergence is not "one is behind the other": 7.4 implements two of six,
7.5 implements four of six plus an undifferentiated version of a fifth, and the
two commented-out lines in 7.4's copy are exactly the two 7.5's copy has. The
consequence is that the gate 7.4 applies ("is the candidate earliest date before
the maximum age date?", Table 7-10 Rule 8) is applied to a *different, earlier*
date than the earliest date the patient is ultimately told. A series dose whose
season opens after the patient ages out passes 7.4's gate and is then forecast by
7.5 for a date past the maximum age - which is the precise outcome Rule 8 exists
to prevent.

**Confirmed live in 7.5:**
`forecastdtOneTheEarliestDateIsTheSameCandidateEarliestDateSevenFourTested`
(red) builds both steps from one `DataModel` and compares their two values
directly: with a season opening 09/01/2030, 7.4's Candidate Earliest Date
attribute reads 01/15/2015 and 7.5's `computeEarliestDate()` returns
09/01/2030. Not observable via FITS, which asserts the reported forecast dates
but never 7.4's gate input.

**Known affected units:** 7.5 (confirmed, 1 red test) and 7.4 (already
confirmed from its own side - `forecastdtcanOneIncludesTheSeasonalRecommendation
StartDate` and `forecastdtcanOneIncludesTheMostRecentDateAdministered`, and see
07-04's Review Findings). What is new here is not that either copy is
incomplete, which both step packages already record, but that there are two
copies at all and that they disagree with each other for the same patient.

**Suggested handling:** the two red tests on the 7.4 side and the one on the 7.5
side are the same fix, and fixing them independently in two Role B sessions would
leave two implementations that merely happen to agree. FORECASTDTCAN-1 belongs in
one place, and the place is already built: `DateRules` declares and constructs a
`FORECASTDTCAN_1` rule object carrying the rule's full six-bullet text verbatim -
but typed `DateRule<Contraindication>` and with `setLogicalComponent
("Contraindication")`, evidently copied from the neighbouring `CALCDTCI_*`
entries, its body a `return null` under the comment `// logic not correct`, and
never invoked from anywhere (verified by grep: the only references are its own
declaration and initialisation). So there are three artefacts for this one rule -
two divergent working copies inside step classes and one correctly-documented,
mistyped, dead stub in the shared rule registry. Recommend deciding 7.4's and 7.5's
Role B sessions together, with the shared rule written once and both classes
reading it, rather than in unit-number order. Note also that 7.5 does not
distinguish the fifth candidate (inadvertent administrations) from the sixth; a
single shared implementation would have to, and nothing in `cdsi-engine`
currently computes that set of dates as a set.

**Status:** open, not yet fixed, not yet a formal finding.

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

**Updated 2026-09-04, from 7.4's side (`DetermineForecastNeedTest`).** 7.4 is the
step that consumes both outcomes, and its two sides turn out **not** to be
symmetric either - which changes what "fix the routing" has to mean.

- **Immunity: 7.4's read is correct.** Table 7-10's condition 3 ("does the
  patient have evidence of immunity?") reads
  `dataModel.getPatientSeriesStepper().getCurrent().getPatientSeriesStatus()
  .equals(PatientSeriesStatus.IMMUNE)` - exactly the per-series status 7.2's
  Table 7-3 state change sets. `ruleFourEvidenceOfImmunityStopsTheForecast` is
  green: hand an `IMMUNE` patient series to 7.4 and it produces the Immune
  outcome, the forecast reason and the loop back to 4.4, all correctly. So the
  immunity half of the gap is **entirely upstream** - fixing 7.2's data routing
  would make 7.4's Rule 5 counterpart work with no change to 7.4 at all.
- **Contraindication: 7.4's read is at the wrong scope, in the wrong place.**
  Table 7-10's condition 4 ("is the relevant patient series a contraindicated
  patient series?") does *not* read the patient-series status; it reads
  `dataModel.getPatient().getMedicalHistory().getContraindicationSet()
  .isEmpty()`. That set is `Set<Contraindication_TO_BE_REMOVED>` and **nothing
  anywhere in `cdsi-engine` or `cdsi-web` ever adds to it** (verified by grep:
  the only readers are this condition and `LogicStepRenderer`), so condition 4
  answers `NO` for every patient in every run and Rule 5 is unreachable -
  independently of, and in addition to, 7.3's own two defects. It is also
  patient-scoped where the specification is series-scoped, so populating it
  naively would make one antigen's contraindication silence every other
  antigen's series, contradicting 7.3's own "an antigen contraindication
  prevents all relevant patient series *for that antigen*".

Sequencing consequence: fixing the loader/type migration alone leaves 7.4's Rule
5 dead, because 7.3 writes `PatientSeriesStatus.CONTRAINDICATED` (per its Table
7-7) while 7.4 reads a different, patient-level structure. The contraindication
side therefore needs a third change beyond the two already recorded above -
7.4's condition 4 has to read the patient series status the way its condition 3
already reads it - and that is a change to `DetermineForecastNeed`, i.e. to a
different unit's class than the loader fix. Downstream matters too:
`PatientSeriesStatus.CONTRAINDICATED` is set nowhere in the engine except 7.4's
own Rule 5 outcome, and is read by 8.1 `PreFilterPatientSeries` (which excludes
contraindicated series) and by `MultipleAntigenVaccineGroup`, so those Chapter 8
behaviours are dead too until this chain is closed end to end.

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
`tableSevenFoursContraindicationElementsAttributeIsFilledFromSupportingData`)
and **7.4** (confirmed, 2 of its 6 red tests -
`ruleFiveAContraindicatedPatientSeriesStopsTheForecast` and
`theContraindicationConditionAsksAboutThisPatientSeriesNotThePatientAsAWhole`;
7.4's immunity-side read is green and needs no change).

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
immunity (2026-09-04) and contraindication (2026-09-04) sides and from the
consuming side in 7.4 (2026-09-04); the two are the same routing cause with
materially different remedies, and the contraindication remedy additionally
reaches into `DetermineForecastNeed`.

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

**Updated 2026-09-05, from 7.6's side (`ValidateRecommendationTest`).** The
sentence below that said `ValidateRecommendation` "is not in any numbered unit
and has not been checked" was wrong on both counts and is corrected here rather
than left standing: it is unit **7.6 Validate Recommendation**, and it has now
been checked. 7.6 is the third subclass of the same base class, and 7.6.1 gives
it the *same* context filter as 7.1 - "Only Conditional Skip Instances with a
context of Forecast or Both should be used" - so it inherits 7.1's version of
the consequence exactly, not 6.2's: in the bundled 4.65-508 release the 67
two-instance series doses retain the Forecast instance 7.6 is also supposed to
use (right instance, by document order rather than by filtering), and the 3
Evaluation-only series doses are applied here too even though their context
excludes them. Nothing about the counts changes; what changes is that the
over-application half of this defect now has two consumers, not one, so the
"accumulate and filter at use time" remedy has to be reachable from three call
sites rather than two. This is the smallest of 7.6's problems - see the separate
2026-09-05 entry on `EvaluateConditionalSkip`'s VALIDATING arm - and it is the
only one of them that is this entry's cause.

**Known affected units:** 6.2 (confirmed, part of its 8 red tests), **7.1**
(confirmed, its 1 red test -
`aConditionalSkipInstanceCarriesTheContextThatDecidesWhetherSevenOneMayUseIt`,
the same "can the entry condition even be expressed?" question asked from the
Forecast side) and **7.6** (confirmed, 1 of its 9 red tests -
`aConditionalSkipInstanceCarriesTheContextThatDecidesWhetherSevenSixMayUseIt`,
the same question asked from the Validating side, with 7.1's filter).

**Suggested handling:** the fix belongs in the domain model (`ConditionalSkip` needs a context field) and the loader (accumulate rather than overwrite, then filter by context at use time), not in `EvaluateConditionalSkipForEvaluation`, `EvaluateConditionalSkipForForecast` or `ValidateRecommendation` individually - and, per the asymmetry above, it cannot be done safely as a loader-only "keep the other one" change. Worth fixing once, before or alongside whichever of 6.2/7.1/7.6 is tackled first in Role B, rather than three times.

**Status:** open, not yet fixed, not yet a formal finding. Confirmed from the Evaluation (2026-09-03), Forecast (2026-09-04) and Validating (2026-09-05) sides.

---

## 2026-09-02 - Patient series scores accumulate across a whole assessment and are never reset

**Discovered while testing:** 8.6 No Valid Doses (`NoValidDosesCompletableTest`, commit `9fd975c`, during the SPEC-4.6-0007 investigation - not a Phase 21 Role A session, but the same kind of cross-cutting observation belongs here regardless of which workflow surfaced it)

**Affected component:** `PatientSeries.addScore`/`setScorePatientSeriesScore` and every step that calls `incPatientScoreSeries`/`descPatientScoreSeries` on it (at least `NoValidDoses`, 8.6) - the score field itself, not any one step's logic.

**What's wrong:** nothing in `cdsi-engine` ever resets a patient series' score between selections. It accumulates monotonically across all ~24,000 `NoValidDoses` invocations observed in one full-suite run, though Table 8-11 reads as describing a per-selection score. Flagged by the SPEC-4.6-0007 investigation as possibly mattering more than the always-increments defect it was actually sent to fix, but not itself investigated further at the time.

**Known affected units:** none yet directly tested - Chapter 8 (8.1-8.9) hasn't had its Role A pass yet as of this note. Flagging now so whoever does 8.6, 8.7 (`SelectPrioritizedPatientSeries`, which reads the score to pick a winner) or 8.9 (`DetermineBestPatientSeries`) checks this deliberately rather than re-discovering it piecemeal.

**Suggested handling:** needs its own investigation to confirm materiality (does the accumulation actually change which patient series wins a selection in practice, the way SPEC-4.6-0007's own fix turned out not to) before deciding whether it's worth fixing at all. Not yet confirmed as a real defect - recorded here as a flagged risk to check when Chapter 8's units are reached, not as an established fact the way the two entries above are.

**Status:** open, unconfirmed, not yet investigated further.
