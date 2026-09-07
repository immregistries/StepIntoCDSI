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

## 2026-09-07 - Two of Table 9-2's rules have no output to write to, and the Supporting Data flag one of them switches on is parsed and read nowhere

**Discovered while testing:** 9.1 Apply General Vaccine Group Rules
(`ApplyGeneralVaccineGroupRulesTest`)

**Affected component:** `domain/Forecast.java` and the
`domain/VaccineGroupForecast.java` that extends it (neither carries a forecast
dose number or a recommended-vaccine list), `domain/VaccineGroup.java`'s
`administerFullVaccineGroup` field together with
`DataModelLoader.readVaccineGroups()` which populates it, and the three Chapter 9
classes that would have to consume all of this - `ApplyGeneralVaccineGroupRules`
(9.1), `SingleAntigenVaccineGroup` (9.2) and `MultipleAntigenVaccineGroup` (9.3).
Not confined to 9.1, which computes none of it.

**What's wrong:** 09-01's Review Findings already record that none of Table 9-2's
`FORECASTVG-*`/`FORECASTDN-2` rule IDs appear anywhere in the codebase (still
true - grep finds zero hits in `cdsi-engine`/`cdsi-web` main source), while
saying that the *behavior* each rule describes "is implemented, in
`MultipleAntigenVaccineGroup`" and, for the single-antigen case, in
`SingleAntigenVaccineGroup` under `SINGLEANTVG-*` labels. That is right for
FORECASTVG-1 through FORECASTVG-8 and needs one correction: **two of the twelve
rules are implemented in neither branch class, because the domain model has
nowhere to put their output.**

1. **FORECASTDN-2** - "the forecast dose number for a vaccine group forecast must
   be ... the minimum of the forecast dose numbers of the patient series
   forecasts contained in the vaccine group forecast if the administer full
   vaccine group flag is 'Y' ... the maximum ... if [it] is 'N'".
   `VaccineGroupForecast` has no dose number at all (its full accessor list is
   dates, a forecast reason, an antigen, a target dose, a status and two lists),
   so neither the minimum nor the maximum has anywhere to be written.
2. **FORECASTVG-9** - the union of the contained forecasts' recommended series
   dose vaccines. Same shape: no recommended-vaccine list on `Forecast` or
   `VaccineGroupForecast`. 9.2's code carries a `SINGLEANTVG-10` comment naming
   exactly this copy with no statement under it; 9.3's `MULTIANTVG_1()` through
   `MULTIANTVG_8()` never mention it.

**The flag, separately.** `administerFullVaccineGroup` - the only input
FORECASTDN-2 has other than the contained dose numbers - *is* parsed
(`readVaccineGroups`, lines ~872-880, into `YesNo`) and is read by nothing: the
only references to `getAdministerFullVaccineGroup()` in `cdsi-engine`/`cdsi-web`
are its own declaration. This is the `seriesGroup`/`seriesGroupName` shape from
the Chapter 8 entry rather than the `<equivalentSeriesGroups>` shape - the value
arrives correctly and is then never consulted. What makes it worth recording is
*where* it is populated: of the bundled 4.65-508 release's 26 vaccine groups,
exactly two carry a populated element - MMR is 'Yes' and DTaP/Tdap/Td is 'No' -
and those two are precisely the two groups VACCINEGROUP-2 classifies as multiple
antigen, i.e. the only groups where a minimum and a maximum over several
contained forecasts could differ at all. The data is populated exactly where the
rule bites, and read nowhere.

**Confirmed live in 9.1:** two of `ApplyGeneralVaccineGroupRulesTest`'s three red
tests are this entry - `forecastdnTwoAVaccineGroupForecastCanCarryAForecastDose
Number` and `forecastvgNineAVaccineGroupForecastCanCarryItsRecommendedSeriesDose
Vaccines`, both the "can the rule even be expressed?" probe used in 6.2, 7.1, 7.6
and 8.8, and both reporting that no such accessor exists on
`VaccineGroupForecast`. `forecastdnTwoTheAdministerFullVaccineGroupFlagIs
PopulatedForTheMultipleAntigenGroups` (green) is the companion evidence that the
flag really is in the release and really is populated only for the two
multiple-antigen groups. Not observable via FITS on the FORECASTVG-9 side (no
recommended-vaccine list is reported at all); the dose number side would be, if
anything produced one.

**Relationship to the 7.5 reds:** this is the vaccine group end of a gap already
red at the patient series end. `GenerateForecastDatesAndRecommendedVaccinesTest`
(7.5) has `forecastdnOneIsTheCountOfSatisfiedTargetDosesPlusOne` (FORECASTDN-1)
and `forecastrecvacOneIdentifiesTheRecommendedSeriesDoseVaccines`
(FORECASTRECVAC-1) failing on the *same two missing fields* of the same
`Forecast` class. That matters for sequencing rather than for blame: FORECASTDN-2
takes the minimum or maximum of the numbers FORECASTDN-1 produces, and
FORECASTVG-9 takes the union of the lists FORECASTRECVAC-1 produces, so the
vaccine group half cannot be fixed before the patient series half and there is no
reason to fix them in two passes.

**Updated 2026-09-07, from 9.2's side (`SingleAntigenVaccineGroupTest`).** 9.2
confirms the `SINGLEANTVG-10`/FORECASTVG-9 half directly - its
`singleantvgTenTheVaccineGroupForecastCanCarryItsRecommendedSeriesDoseVaccines`
is 9.1's probe re-pointed at 9.2 and reports the same missing accessor - and it
confirms 9.1's characterisation of `SINGLEANTVG-3` through `SINGLEANTVG-8`:
they really are six plain single-field copies (adjusted recommended, adjusted
past due, latest, unadjusted recommended, unadjusted past due, forecast reason),
one statement each, and all six are green. But it also **corrects this entry's
central count in two ways, and both corrections make the gap wider than "two
rules have nowhere to write":**

1. **A third rule of Table 9-2 is written by nobody, and this one is not for
   want of a field.** `FORECASTVG-1` defines what it means for a patient series
   forecast to be *contained in* a vaccine group forecast - and it is the input
   clause of eight other rules, including **both** of Table 9-3's
   (`SINGLEANTVG-1` and `SINGLEANTVG-2` are each phrased over "the patient
   series forecast[s] contained in the vaccine group forecast").
   `VaccineGroupForecast` has a `forecastList` for exactly this, with a getter
   and a setter, and grep finds no writer anywhere in `cdsi-engine` or
   `cdsi-web`: neither 9.2 nor 9.3 records which patient series forecasts its
   vaccine group forecast was assembled from. So the containment relation the
   chapter is written over exists in the domain model and is empty at runtime,
   which also means nothing downstream (the step viewer, `ForecastServlet`, a
   future FORECASTDN-2) can recover it. Confirmed by
   `theContainedPatientSeriesForecastIsRecordedOnTheVaccineGroupForecast` (red,
   expected the one contributing `Forecast`, actual `[]`).
2. **FORECASTVG-8 is implemented on one branch and commented out on the other.**
   The entry above says the `FORECASTVG-1..8` behaviour "is implemented" in both
   branch classes. That holds for 1 through 7; FORECASTVG-8 (an antigen is a
   recommended antigen if its best patient series is the basis of a contained
   forecast with status 'Not Complete') is `MULTIANTVG_8()` in
   `MultipleAntigenVaccineGroup` - which builds the list and calls
   `vgf.setAntigensNeededList(...)` at line 145 - and in
   `SingleAntigenVaccineGroup` it is the `SINGLEANTVG-9` comment with its one
   statement **commented out** (`// vgf.setAntigensNeededList(forecast.getAntigen());`,
   line 131), apparently abandoned over the type mismatch between the single
   `Antigen` to hand and the `List<Antigen>` the setter takes. This is a
   different shape from the other two and should be sequenced differently: it
   needs no domain-model change at all, only the one-line list wrap, so it is
   fixable inside 9.2's own Role B session without waiting on 7.5. Confirmed by
   `singleantvgNineTheAntigensNeededAreTheContainedPatientSeriesTargetDisease`
   (red, expected `[Hepatitis B]`, actual `[]`). Worth noting the asymmetry it
   causes today: a Not Complete MMR group reports the antigens it needs and a
   Not Complete HepB group reports none.

So of Table 9-2's twelve rules the tally is now: two with nowhere to write
(FORECASTDN-2, FORECASTVG-9), one with somewhere to write that nobody writes
(FORECASTVG-1), one written on the multiple-antigen branch only (FORECASTVG-8),
and eight implemented on both branches (FORECASTVG-2..7 plus VACCINEGROUP-1/2).
9.2's other two reds are its own class's defects, not this entry, and are
recorded in that unit's `status.yaml` notes.

**Updated 2026-09-07, from 9.3's side (`MultipleAntigenVaccineGroupTest`) - the
last of the three Chapter 9 units, so this update closes the entry's open
predictions out.** All four predictions above hold exactly as stated, and 9.3
adds one qualification and one new detail:

1. **FORECASTVG-1: red, as predicted.** `MultipleAntigenVaccineGroup` assembles
   its own `selectedList` of `PatientSeries` in the constructor and never
   records which forecasts the group forecast was built from, so
   `VaccineGroupForecast.forecastList` is empty on this branch too.
   `theContainedPatientSeriesForecastsAreRecordedOnTheVaccineGroupForecast`
   (expected the two contributing `Forecast`s, actual `[]`). With both branch
   classes now tested, the claim "grep finds no writer anywhere" is confirmed by
   test from both sides: nothing in the engine ever populates the containment
   relation the whole chapter is written over.
2. **FORECASTVG-9: red, as predicted - and it was attempted here too.** The
   probe (`forecastvgNineAVaccineGroupForecastCanCarryItsRecommendedSeriesDose
   Vaccines`) reports the same missing accessor as 9.1's and 9.2's. What is new
   is that 9.3 is not silent about the rule the way the entry above implies
   ("9.3's `MULTIANTVG_1()` through `MULTIANTVG_8()` never mention it"): the
   `MULTIANTVG-9` block in `process()` (lines ~85-90) builds a local
   `List<VaccineGroup> recommendedVaccines` from each contained forecast's own
   `getVaccineGroupForecast().getVaccineGroup()` and then never reads or assigns
   it. So both branch classes carry an abandoned FORECASTVG-9 - 9.2 as a comment
   with the statement commented out, 9.3 as live dead code - and 9.3's version
   additionally collects the wrong type (vaccine groups, not series dose
   vaccines), which is worth knowing before anyone treats it as a partial
   implementation to finish rather than delete.
3. **FORECASTDN-2: red, as predicted**
   (`forecastdnTwoAVaccineGroupForecastCanCarryAForecastDoseNumber`). Worth
   recording that 9.3 is the step where this rule actually bites: both of the
   flag's populated values in the release belong to multiple antigen groups, so
   `SingleAntigenVaccineGroup` could never need either branch and
   `MultipleAntigenVaccineGroup` needs both.
4. **FORECASTVG-8: green, as predicted - but only for one of Table 9-4's six
   outcomes.** `MULTIANTVG_8()` does exactly what the rule says
   (`forecastvgEightTheAntigensNeededAreTheNotCompleteContainedForecastsTarget
   Diseases`, green: a Not Complete Measles and a Not Complete Rubella in a
   Complete-Mumps MMR group produce `[Measles, Rubella]`). The qualification is
   that `process()` calls `MULTIANTVG_1()` through `MULTIANTVG_8()` **only**
   inside `if (vgf.getVaccineGroupStatus() == VaccineGroupStatus.NOT_COMPLETE)`;
   every other Table 9-4 outcome takes an `else` branch that builds the antigen
   list and adds the forecast but runs no aggregation at all. FORECASTVG-8 is
   defined over each *contained forecast's* status, not the group's, so an MMR
   group that Table 9-4 Rule 1 makes Contraindicated because one component is
   contraindicated reports no recommended antigens even when another component
   is still Not Complete - which is precisely the case where a monovalent
   recommendation would be based on it
   (`forecastvgEightTheAntigensNeededAreComputedWhateverTheVaccineGroupStatusIs`,
   expected `[Mumps]`, actual `[]`). The same gate suppresses MULTIANTVG-1 and
   FORECASTVG-2..7, none of which carry a status precondition in their own text
   either.

That gate is why this belongs here rather than only in 9.3's notes: **the two
branch classes disagree about it.** `SingleAntigenVaccineGroup` copies all of
SINGLEANTVG-1..8 unconditionally, for every one of the six statuses (9.2's
`singleantvgOneTheVaccineGroupStatusIsThePatientSeriesStatusOfTheContained
Forecast` drives all six and is green); `MultipleAntigenVaccineGroup` runs its
eight equivalents for one status only. So a Contraindicated HepB group reports
its dates and reason and a Contraindicated MMR group reports neither, from the
same Table 9-2 rules. Whichever way that is settled it has to be settled for
both classes at once, which no single unit's Role B session can do - the same
shape as the FORECASTVG-1 containment fix, and unlike it, not blocked on 7.5.

**Known affected units:** 9.1 (confirmed, 2 of its 3 red tests), 7.5
(confirmed from its own side earlier, 2 of its reds, recorded in that unit's
notes), **9.2** (confirmed 2026-09-07, 3 of its 6 red tests - SINGLEANTVG-10
for FORECASTVG-9, plus the two corrections above for FORECASTVG-1 and
FORECASTVG-8) and **9.3** (confirmed 2026-09-07, 4 of its 9 red tests -
FORECASTVG-1, FORECASTVG-9, FORECASTDN-2 and the status-gated half of
FORECASTVG-8). All three Chapter 9 units have now had a Role A pass; 9.3's other
five reds are MULTIANTVG-1 and FORECASTPRIORITY-1 defects in its own class and
are recorded in that unit's `status.yaml` notes.

**Suggested handling:** one domain-model change, sequenced with 7.5 rather than
with 9.1. `Forecast` needs a forecast dose number and a recommended series dose
vaccine list; once it has them, FORECASTDN-1/FORECASTRECVAC-1 can fill them in
7.5 and FORECASTDN-2/FORECASTVG-9 can aggregate them in 9.2/9.3, with
`getAdministerFullVaccineGroup()` finally read at the point FORECASTDN-2 chooses
between minimum and maximum. Nothing about this can be fixed inside
`ApplyGeneralVaccineGroupRules`: 9.1's whole implementation is the
VACCINEGROUP-1/2 classification, and the rules it nominally owns have no code in
it to correct. Worth deciding once, with 7.5's, 9.2's and 9.3's Role B sessions
in view. Per the 9.2 update above, two of the four affected rules are **not**
part of that domain-model change and should not be sequenced behind it:
FORECASTVG-8's single-antigen half is a one-line fix inside
`SingleAntigenVaccineGroup`, and FORECASTVG-1's containment needs only that 9.2
and 9.3 each add their contributing forecasts to the `forecastList` that is
already on `VaccineGroupForecast`. Both are unit-local and can be done in 9.2's
and 9.3's own Role B sessions. Per the 9.3 update above, a third piece now sits
between the two sizes: the `NOT_COMPLETE` gate on 9.3's aggregation is a
one-line change in `MultipleAntigenVaccineGroup`, but deciding *whether* to make
it is a two-class decision, because `SingleAntigenVaccineGroup` already does the
opposite. Sequence it with the FORECASTVG-8 one-liner - both are about the same
rule, on the two halves of the same chapter - rather than with the 7.5 domain
model work. Delete rather than complete 9.3's dead `MULTIANTVG-9` block when
FORECASTVG-9 is finally implemented; it collects vaccine groups, not series dose
vaccines.

**Status:** open, not yet fixed, not yet a formal finding. Confirmed from 9.1's
side (2026-09-07), 9.2's side (2026-09-07, which corrects the rule count - see
that update) and 9.3's side (2026-09-07, which closes out the entry's four open
predictions - all held - and adds the `NOT_COMPLETE` gate as a two-class
decision). All three Chapter 9 units have had a Role A pass, so no further
confirmation of this entry is pending from Chapter 9; what remains open is
7.5's half of the domain-model change.

---

## 2026-09-05 - `<equivalentSeriesGroups>` is in every antigen series in the release, is parsed nowhere, and two of Table 8-14's five conditions are defined entirely over it

**Discovered while testing:** 8.8 Determine Best Patient Series
(`DetermineBestPatientSeriesTest`)

**Affected component:** `DataModelLoader`'s `<series>` child loop (around lines
447-481 - it handles `seriesName`, `targetDisease`, `seriesType`,
`requiredGender` and `selectSeries`, and silently skips `equivalentSeriesGroups`),
`domain/AntigenSeries.java` (the class that element belongs on - it is a direct
child of `<series>`, a sibling of `<seriesType>`, **not** inside `<selectSeries>`
where `seriesGroup`/`seriesGroupName` live), and the consumers of the concept:
`DetermineBestPatientSeries` (8.8) and, per 4.6's own text, the vaccine group
blending in 4.6/Chapter 9.

**What's wrong:** the specification defines the term in 4.5 - "A best patient
series will be selected for each Series Group, however, some antigen series
define **Equivalent Series Groups** which allow a single best series to be
selected from across multiple Series Groups" - and Chapter 8's overview repeats
it ("the prioritized patient series from one series group may negate the need for
the prioritized patient series from another **equivalent** series group"). It is
not an inference to be made at runtime: it is a declared Supporting Data value.
Every one of the 143 `<series>` elements in the bundled 4.65-508 release carries
an `<equivalentSeriesGroups>` element naming the series group(s) its own group is
interchangeable with. 54 are populated (37 hold "2", 15 hold "1", 2 hold "3") and
89 are self-closing, i.e. equivalent to nothing. HepA is the clean illustration:
the Standard group's series (series group 1) declares "2", the Increased Risk
group's series (series group 2) declare "1" - each pointing at the other, so the
two groups are equivalent - and the single Increased Risk - Pediatric Travel
series (series group 3) declares an empty element and is equivalent to nothing.

Nothing in `cdsi-engine` or `cdsi-web` mentions the name (verified by grep: zero
hits for `equivalentSeriesGroups` in any `.java` file, against 1419 occurrences
across the XML resources), and no domain class has a field for it. So the value
is not merely unread, as `seriesGroup`/`seriesGroupName` are - it is never
parsed, and there is nowhere to put it if it were.

**Why that matters more than it looks:** two of Table 8-14's five conditions are
defined *entirely* over this value - "Is there a prioritized patient series that
is a complete patient series **in an equivalent series group**?" and "Is there a
prioritized patient series with a series type of 'Risk' **in an equivalent series
group**?" - and they are the only two conditions in the table that look outside
the series being judged. `DetermineBestPatientSeries` answers them over the whole
antigen (condition 2) or over the whole assessment (condition 5) instead, so for
the 89 series that declare no equivalent series group at all - the majority of
the release - both conditions should answer No unconditionally and neither ever
does. This is the *suppression* direction, which is the damaging one: a series
that no other group is equivalent to is dropped from the best patient series list
because some unrelated group's series is complete, or is a Risk series. 4.6 needs
the same value from the other end ("For vaccine groups which contain
non-equivalent series groups, it is important to only blend best patient series
of the same series type"), so this is not confined to 8.8.

**Confirmed live in 8.8:** three of `DetermineBestPatientSeriesTest`'s six red
tests. `theEquivalentSeriesGroupsTableEightFourteenTurnsOnAreCarriedByTheDomain
Model` is the "can the condition even be expressed?" test (the same shape as
6.2/7.1/7.6's conditional skip context): reflecting over `AntigenSeries` and
`SelectPatientSeries` finds no accessor mentioning "equivalent" at all.
`theCompleteSeriesConditionAnswersNoForASeriesThatDeclaresNoEquivalentSeries
Group` and `theRiskElsewhereConditionAnswersNoForASeriesThatDeclaresNoEquivalent
SeriesGroup` show the consequence with HepA's real group numbers: a prioritized
patient series in the pediatric travel group (which is equivalent to nothing) is
excluded from the best patient series list because the Standard group has a
complete series, and again because the Increased Risk group has a Risk series.
`theReleaseDeclaresTheEquivalentSeriesGroupsTableEightFourteenNeeds` (green) is
the companion evidence that the data really is in the bundled release. Not
observable via FITS, which asserts the final forecast rather than which series
were selected as best.

**Known affected units:** 8.8 (confirmed, 3 of its 6 red tests). 4.6 Identify and
Evaluate Vaccine Group is a second consumer by its own section text and has not
been checked against this.

**Suggested handling:** this is a loader plus domain-model change (parse the
element onto `AntigenSeries` as a list of series group identifiers, then have
8.8's two conditions ask "is this other series' series group in my series'
equivalent series groups?"), not a fix inside `DetermineBestPatientSeries`. It is
closely related to, but **not the same as**, the "Chapter 8 has no series group"
entry below: that one is about a missing loop and about `seriesGroup` being
loaded and never read, while this is about a value that never arrives at all.
They should be sequenced together, because the equivalence relation is meaningless
without the series group it relates - but note the ordering, which is the useful
part: introducing the series group loop *without* this value would make Chapter 8
per-group and then leave 8.8 unable to ask either of its two cross-group
questions, which is a worse state than today for the 10 of the release's 30
antigens whose groups genuinely are equivalent (HepB with 16 populated elements,
Pneumococcal 7, HepA/HPV/Hib 6 each, RSV 4, Meningococcal B and Zoster 3 each,
COVID-19 2, Meningococcal 1). Both halves of the data are needed before either of
Table 8-14's last two conditions can be right.

**Status:** open, not yet fixed, not yet a formal finding.

---

## 2026-09-05 - CVX-to-antigen association age is parsed nowhere, so one administered dose can be misassigned to the wrong antigen

**Discovered while testing:** 4.2 Organize Immunization History
(`OrganizeImmunizationHistoryTest`), confirmed against real Supporting Data
while reviewing an outside strategy note (`docs/25-phase-b-strategy-second-opinion.md`)
that independently flagged this as a likely foundational cluster.

**Affected component:** `DataModelLoader.readCvx()` (the `cvxToAntigenMap`
parser) and every downstream step that reads `VaccineType.getAntigenList()`/
`Antigen.getCvxList()` without an age check - starting with
`OrganizeImmunizationHistory` (4.2), which is the first place an administered
dose is expanded into per-antigen records.

**What's wrong:** the real Supporting Data (`ScheduleSupportingData.xml`)
carries `<associationBeginAge>`/`<associationEndAge>` on each `<association>`
inside a `<cvxMap>` - for example CVX 121 ("Zoster live") associates with
Varicella for ages 0 days-50 years and with Zoster for ages 50 years and up.
`readCvx()`'s loop over an `<association>`'s children checks only for the
`antigen` child node; `associationBeginAge`/`associationEndAge` are never read
by any code in `cdsi-engine` (confirmed by grep: zero references to either
name anywhere in `src/main/java`). So `VaccineType.getAntigenList()` for CVX
121 unconditionally contains both Varicella and Zoster, for every patient at
every age, and `OrganizeImmunizationHistory` (4.2's own class, `line 22`:
`for (Antigen antigen : vda.getVaccine().getVaccineType().getAntigenList())`)
expands one Zoster-live administration into one antigen-administered record
per antigen in that unfiltered list, regardless of the patient's age at
administration.

**Confirmed live in 4.2:** `OrganizeImmunizationHistoryTest`'s two red tests -
`zosterLiveGivenBelowFiftyYearsIsAssociatedWithVaricellaOnly` (expected
`[Varicella]`, actual `[Varicella, Zoster]`) and
`zosterLiveGivenAtOrAboveFiftyYearsIsAssociatedWithZosterOnly` (expected
`[Zoster]`, actual `[Varicella, Zoster]`) - both fail for this one reason.

**Known affected units:** 4.2 (confirmed, both its red tests). Upstream of
almost everything else: any step reasoning about a patient's Varicella or
Zoster series (6.x evaluation, 7.x forecasting, 8.x series selection) sees an
administered-record list containing a dose that shouldn't count toward that
antigen at all, for the patient's actual age. Not yet checked against other
CVX codes with more than one age-bounded association in the bundled release.

**Suggested handling:** this is a loader/domain-model change (parse the two
ages onto the association, and make the association age-aware at the point
`AntigenAdministeredRecord`s are created), not a narrow 4.2 fix - flagged here
so it's weighed as an early cluster during Role B sequencing rather than
patched locally in `OrganizeImmunizationHistory` alone.

**Status:** open, not yet fixed, not yet a formal finding.

---

## 2026-09-05 - Chapter 8 has no series group, and its eight steps read three different "series in scope" lists

**Discovered while testing:** 8.1 Pre-filter Patient Series
(`PreFilterPatientSeriesTest`)

**Affected component:** `SelectPatientSeries.getSeriesGroup()` /
`getSeriesGroupName()` (loaded, never read) and the scope each Chapter 8 step
picks for "the patient series I am working on" - `PreFilterPatientSeries` (8.1),
`InProcessPatientSeries` (8.5), `NoValidDoses` (8.6) and
`DetermineBestPatientSeries` (8.8) read
`dataModel.getPatientSeriesStepper().getList()`; `CompletePatientSeries` (8.4)
and `SelectPrioritizedPatientSeries` (8.7) read
`dataModel.getSelectedPatientSeriesList()`; `IdentifyOnePrioritizedPatient
Series` (8.2) and `ClassifyScorablePatientSeries` (8.3) read
`dataModel.getScorablePatientSeriesList()`. Not something confined to 8.1.

**What's wrong:** the specification partitions Chapter 8 twice. Chapter 8's own
overview says "Process steps 8.1 through 8.7 are repeated **for each series
group** to identify one prioritized patient series per series group. Process
step 8.8 is then used to determine which prioritized patient series are selected
as a best patient series", and 4.5 / Figure 4-7 wrap all of that in a
**per-antigen** loop. Neither partition exists in the implementation:

1. **No series group loop, and no series group read.** `DataModelLoader` parses
   `<seriesGroup>` and `<seriesGroupName>` onto `SelectPatientSeries`, and
   grepping `cdsi-engine`/`cdsi-web` for `getSeriesGroup()` /
   `getSeriesGroupName()` returns only their own declarations - nothing anywhere
   reads either. So 8.1-8.7 run once per antigen rather than once per series
   group, and every rule in Chapter 8 phrased "for the same series group" is
   silently evaluated antigen-wide. This is the normal shape of the data, not a
   corner case: 15 of the 30 antigens in the bundled 4.65-508 release define
   more than one series group (Pneumococcal has three: Standard, Standard 50+,
   Increased Risk; HepA has Standard, Increased Risk, and Increased Risk -
   Pediatric Travel).
2. **Three different lists for one concept, one of which is not even
   antigen-scoped.** 4.5 does the per-antigen narrowing correctly, into
   `selectedPatientSeriesList` (`SelectBestPatientSeriesTest` covers this and is
   fully green). Only two of the eight Chapter 8 steps read it. Four read the
   patient series stepper, which is 5.1's unfiltered list of *every* relevant
   patient series for *every* antigen and is never re-scoped between antigens -
   so on the HepB pass those steps also see Measles' series.

**Confirmed live in 8.1:** four of `PreFilterPatientSeriesTest`'s eight red tests
are this entry. `theStepExaminesOnlyThePatientSeriesOfTheAntigenBeingProcessed`
builds a HepB series and a Measles series, sets `antigen`/
`selectedPatientSeriesList` exactly as 4.5 leaves them, and gets both back in
`scorablePatientSeriesList`. `theStepExaminesThePatientSeriesOfOnlyOneSeries
Group` gets a scorable list spanning two series groups of one antigen.
`selectscoreTwoRiskPrioritiesAreComparedWithinOneSeriesGroupNotAcrossGroups`
shows the concrete clinical consequence: SELECTSCORE-2's Risk bullet keeps only
the highest-priority Risk series "that belongs to the same series group", but
`PreFilterPatientSeries` computes a single `highestRiskPriority` over everything
it can see, so a priority-B Risk series that is the top of its own group is
discarded because some other group (or, per the previous point, some other
antigen entirely) has a priority-A Risk series.
`selectbTwentyFourTheAllContraindicatedFallbackIsDecidedPerSeriesGroup` shows
the same thing on SELECTB-24: the "keep the contraindicated series if they are
*all* contraindicated" escape hatch is decided globally, so one healthy series
anywhere suppresses a wholly-contraindicated group. Not observable via FITS,
which asserts the final forecast rather than which series were in scope when it
was chosen.

**Updated 2026-09-05, from 8.2's side
(`IdentifyOnePrioritizedPatientSeriesTest`).** 8.2 confirms both halves, and
adds one thing 8.1's vantage point could not show: **the antigen scope is
inconsistent *within a single class*, not just between classes.**
`IdentifyOnePrioritizedPatientSeries$LT` has four conditions and five outcome
bodies. Three of the four conditions (default, complete, in-process counts) and
four of the five outcome bodies open with
`if (!patientSeries.getTrackedAntigenSeries().getTargetDisease().equals(dataModel.getAntigen())) continue;`
- i.e. they already assume Chapter 8 is per-antigen, agreeing with Figure 4-7
and disagreeing with the all-antigen list 8.1 hands them. Condition 0, the
scorable count that gates every rule column of Table 8-3, has no such filter,
and neither does outcome 0. So a Measles series left in
`scorablePatientSeriesList` on the HepB pass makes the scorable count 2 while
the other three counts report 0, and Table 8-3 matches no rule at all rather
than the Rule 2 it should. This is not inherited from 8.1: whichever way the
scope question is settled globally, one of these four conditions is wrong on its
own terms, so 8.2's Role B session has a small local correction to make in
addition to whatever the chapter-wide decision turns out to be. The series-group
half reproduces in 8.2 exactly as in 8.1 - a Standard-group series and an
Increased Risk-group series counted together make Rule 2's "1 scorable series"
unreachable for either group. Confirmed by 2 of 8.2's 5 red tests
(`theScorableSeriesCountIsScopedToTheAntigenBeingProcessedLikeTheOtherThree
Conditions`, `theStepCountsOnlyThePatientSeriesOfOneSeriesGroup`). 8.2's other
three red tests are its own class's business-rule defects and are not this
entry.

**Updated 2026-09-05, from 8.3's side
(`ClassifyScorablePatientSeriesTest`).** 8.3 reproduces both halves and adds two
things neither 8.1 nor 8.2 could show. First, the "inconsistent *within* a class"
observation from 8.2 does not generalise into an intent that could be read off
the code: `ClassifyScorablePatientSeries` filters **nowhere at all**. None of its
three `LT` conditions, and neither of its two counting helpers
(`calculateCompletePatientSeriesCount`, `calculateCountOfPatientSeriesWithValid
Doses`), reads `dataModel.getAntigen()` or any series group - they walk the whole
of `scorablePatientSeriesList` unconditionally. So across the two consecutive
steps that read the same list, 8.2 filters by antigen in 3 of 4 conditions and
8.3 in 0 of 3; there is no consistent convention to preserve, and a chapter-wide
decision cannot be implemented by "following what the neighbouring class already
does".

Second, and the reason this matters more in 8.3 than in 8.2: **8.3's output is a
control-flow branch, not a count.** 8.2's contamination makes a shortcut rule
fail to match, which merely costs the shortcut - the series still go on to be
scored. 8.3's Table 8-5 decides *which chapter of scoring business rules the
whole group is judged by* (8.4 complete / 8.5 in-process / 8.6 no valid doses),
so two complete series belonging to a different antigen, or to a different series
group of the same antigen, silently route this group's in-process series to the
complete patient series scoring rules. `CompletePatientSeries` then scores every
non-`COMPLETE` series *down* (`descPatientScoreSeries`, lines 83 and 89), so the
group under selection is scored by rules that penalise every series in it. Both
of 8.3's scoping tests (`theStepClassifiesOnlyThePatientSeriesOfOneSeriesGroup`,
`theStepClassifiesOnlyThePatientSeriesOfTheAntigenBeingProcessed`) show exactly
that: expected 8.5, actual 8.4. Note also that this is the first Chapter 8 step
where the two halves of this entry are *equally* damaging - the series-group half
is not a lesser version of the antigen half here, because both reach the branch
through the same unfiltered count.

**Updated 2026-09-05, from 8.4's side (`CompletePatientSeriesTest`).** 8.4 is the
first tested Chapter 8 step that reads `selectedPatientSeriesList` rather than
the stepper, and it changes this entry's *remedy*, not just its evidence. The
description above - and the suggested handling below - treat
`selectedPatientSeriesList` as the list the stepper-reading steps ought to be
switched to, on the grounds that 4.5 narrows it to one antigen correctly. That is
true on the antigen axis and misleading on every other: `selectedPatientSeriesList`
is 4.5's **pre-8.1** list. `SelectBestPatientSeries` fills it with *every*
relevant patient series of the current antigen (`SelectBestPatientSeries.java`,
lines 33-39, an unfiltered copy of the stepper filtered only by
`getTargetDisease().equals(antigen)`), and 8.1 `PreFilterPatientSeries` then
narrows *that* into `scorablePatientSeriesList` - dropping contraindicated series
per SELECTB-24, and all but the highest-priority Risk series of a group per
SELECTSCORE-2. So there are not two lists on one axis (antigen-scoped vs not) but
three on two axes: the stepper (all antigens, unfiltered), the selected list (one
antigen, unfiltered) and the scorable list (all antigens, filtered). Every rule
in 8.4 is phrased over *scorable* patient series - Table 8-7's own title is "How
Many Points Are Awarded to a **Scorable** Patient Series That Is a Complete
Patient Series?", and 8.3 counts the scorable list to decide that 8.4 should run
at all - so 8.4 reads the wrong stage of the pipeline, not merely the wrong
scope. The consequence is sharper than a contaminated count: 8.4's condition is a
*comparison*, so a series 8.1 deliberately dropped from consideration still sets
the maximum valid-dose count that every genuinely scorable series is measured
against, and can push all of them to -1.
`theStepScoresTheScorablePatientSeriesEightOneProducedNotEveryRelevantSeries`
shows exactly that with two Increased Risk series: the priority-A series 8.1 kept
has 2 valid doses, the priority-B series 8.1 dropped has 5, and the scorable
winner is scored -1 instead of +1. The series-group half reproduces unchanged
(`theStepScoresThePatientSeriesOfOneSeriesGroup`, expected +1 for the Standard
group's winner, actual -1 because an Increased Risk series has more valid doses),
and for the same reason it is worse here than in 8.3: a stray series does not
merely join the group being scored, it wins the competition outright. 8.4's other
six red tests are its own class's scoring defects and are not this entry.

**Updated 2026-09-05, from 8.5's side (`InProcessPatientSeriesTest`).** 8.5 is
the first *tested* step confirmed to read the all-antigen stepper (8.1-8.4 were
read from the other two lists), so both halves reproduce here directly - and it
adds a third kind of consequence the earlier four could not show. In 8.1 and 8.2
a stray series contaminates a **count**; in 8.3 it flips a **branch**; in 8.4 it
wins a **comparison**. In 8.5 it can set a **boolean that is then applied to a
different series' own condition**. Table 8-9's first row
(`evaluate_ACandidatePatientSeriesIsAProductPatientSeriesAndHasAllValidDoses`)
declares `productPatientSeries` and `hasAllValidDoses` *outside* the per-series
loop and never resets them, so once any series in the list it can see sets one,
every series scored after it inherits it. That sticky-flag defect belongs to
8.5's own class and is recorded in this unit's `status.yaml` notes as such - what
belongs here is its blast radius, which is set entirely by which list the step
reads: on the stepper, a Measles series with a product path of 'Y' makes every
HepB series scored after it count as a product patient series, and a Measles
series carrying one invalid dose makes every later HepB series count as not
having all valid doses. The same is true of the fifth row: `evaluate_ACandidate
PatientSeriesCanFinishEarliest()` seeds its comparison from
`patientSeriesList.get(0)` and abandons the entire row when that first series has
no forecast, so whichever series happens to sit first in the *all-antigen*
stepper decides whether Table 8-9's last row is scored for anybody
(`theRowIsScoredForEverySeriesEvenWhenTheFirstSeriesHasNoLatestDate`, red). None
of that changes this entry's remedy; it raises how much the stage/scope decision
is worth, because in 8.5 the wrong list does not merely distort a comparison
between well-formed series, it corrupts individual series' own condition answers.

8.5 also confirms 8.4's correction to the remedy from the opposite side. 8.4
argued `scorablePatientSeriesList` is the right target because every rule in
8.2-8.7 is phrased over scorable patient series; 8.5 is the case where reading
the stepper is wrong on **both** axes at once - wrong antigen *and* wrong
pipeline stage - so switching it to `selectedPatientSeriesList` would fix one and
leave the other, exactly the half-fix 8.4 warned against. Three of 8.5's 18 red
tests are this entry
(`theStepScoresTheScorablePatientSeriesEightOneProducedNotEveryRelevantSeries`,
expected +2 actual -2 because a priority-B Risk series 8.1 dropped has more valid
doses; `theStepScoresThePatientSeriesOfTheAntigenBeingProcessed`, expected +2
actual -2 because a Measles series has more; `theStepScoresThePatientSeriesOf
OneSeriesGroup`, expected +2 actual -2 because an Increased Risk series has
more). 8.5's other 14 reds are its own class's defects and are not this entry.

**Updated 2026-09-05, from 8.6's side (`NoValidDosesTest`).** 8.6 is the second
tested step that reads the all-antigen stepper, and both halves reproduce
exactly as in 8.5 - but the failure mode is worse than in any of the five
earlier steps, and worth recording because it changes how much the wrong list
costs rather than only who it costs. In 8.1/8.2 a stray series contaminates a
count; in 8.3 it flips a branch; in 8.4 it wins a comparison; in 8.5 it sets a
sticky boolean applied to other series' conditions. In 8.6 **a stray series
makes the row's positive outcome unawardable to anybody at all.** All three of
8.6's scoping reds fail through Table 8-11's "can start earliest" row, whose
implementation seeds `earliestDate` from `patientSeriesList.get(0)` and, on
finding a strictly earlier date later in the list, resets its tie counter
`numOfEarliestDates` to 0 - a value the loop can never bring back to the 1 the
scoring loop requires before it will award the +1. So a Measles series with an
earlier start date sitting in the stepper does not take HepB's point: it leaves
every HepB series on -1 and awards the +1 to no one, Measles included. The
observed scores in `theStepScoresThePatientSeriesOfTheAntigenBeingProcessed` are
-1 / -1 / 0 for a group whose winner should have had +1. That combination - the
list-choice defect that is this entry's subject and 8.6's own order-dependent
counter defect - is why 8.6's scoping reds cannot be resolved by the
chapter-wide decision alone, unlike 8.3's; 8.6 needs its own row-1 fix as well,
which puts it in the same shape as 8.2 (a unit-local correction *plus* the
chapter-wide one) rather than 8.3.

8.6 also carries the byte-identical sibling of 8.5's sticky-flag defect, in its
product patient series row: `productPatientSeries` is declared outside the
per-series loop and never reset, so once any series the step can see has a
product path of 'Y', every series scored after it counts as a product patient
series
(`theProductRowMustDiscriminateBetweenAProductSeriesAndOneThatIsNot`, red -
the two series scored 1 and 1). That defect belongs to 8.6's own class and is
recorded in this unit's `status.yaml` notes; what belongs here is, as in 8.5,
its blast radius - on the all-antigen stepper a Measles product series silently
makes every HepB series scored after it a product patient series. Three of 8.6's
18 red tests are this entry
(`theStepScoresTheScorablePatientSeriesEightOneProducedNotEveryRelevantSeries`,
`theStepScoresThePatientSeriesOfTheAntigenBeingProcessed`,
`theStepScoresThePatientSeriesOfOneSeriesGroup`, all expected +1 actual -1).
8.6's other 15 reds are its own class's defects and are not this entry.

**Updated 2026-09-05, from 8.7's side
(`SelectPrioritizedPatientSeriesTest`).** 8.7 confirms the list-choice table
above by test rather than by source reading - it does read
`selectedPatientSeriesList` - and it is where the two halves of this entry
diverge most sharply, because it is the step that *names the answer*.

On the **antigen** axis 8.7 is **correct, and confirmed green**
(`theSelectionIsMadeOverThePatientSeriesOfTheAntigenBeingProcessed`): a Measles
series carrying a score of 9 on the all-antigen stepper cannot take the HepB
pass's selection, because 4.5 rebuilds `selectedPatientSeriesList` per antigen.
Together with 8.4 that makes two of the eight Chapter 8 steps already right on
this axis, and it means the chapter-wide fix has two working examples to
converge on, not zero.

On the **stage** axis it is worse here than anywhere else in the chapter. The
escalation this entry has been tracking - 8.1/8.2 a contaminated count, 8.3 a
flipped branch, 8.4 a lost comparison, 8.5 a sticky boolean, 8.6 an unawardable
outcome - ends at 8.7 with the stray series simply *winning*.
`theSelectionIsMadeOverTheScorablePatientSeriesEightOneProduced` (red) puts two
Risk series of one group on 4.5's pre-8.1 list and only the priority-A one on
`scorablePatientSeriesList`, as SELECTSCORE-2 requires; the priority-B series
8.1 deliberately dropped - a series that was never scored by 8.4/8.5/8.6 at all,
and whose score is therefore whatever it happened to hold - is the one 8.7 names
as the prioritized patient series, and 8.8 evaluates that. So on 8.7 the
consequence of reading the wrong pipeline stage is not a distorted number
feeding a later decision; it is the final answer being a series the pipeline had
already excluded. That is the strongest case yet for 8.4's correction to this
entry's remedy (name a *stage*, `scorablePatientSeriesList`, not just a scope).

On the **series group** axis 8.7 is the one step where the gap is visible in the
specification's own sentence about the step: 8.7's Purpose says the rules
"result in the prioritized patient series **for the series group**", so its
output is defined per group. Two reds show both sides of that.
`theStepProducesOnePrioritizedPatientSeriesPerSeriesGroup` expects two entries
on `prioritizedPatientSeriesList` for an antigen with a Standard and an Increased
Risk group and gets one; `theSelectionComparesScoresWithinOneSeriesGroupNot
AcrossGroups` shows why that one is also the wrong one - the Increased Risk
series outscores the Standard group's own winner and takes the whole antigen's
selection. Worth noting for whoever fixes this that the output side has a second
obstacle beyond 8.7 itself: `SelectBestPatientSeries` (4.5) calls
`dataModel.getPrioritizedPatientSeriesList().clear()` on every antigen pass, so
even if 8.7 produced one winner per group the list could still only ever hold one
antigen's worth, and 8.8's `LT` is built one table per entry on that list. The
series-group loop this entry describes therefore has to be introduced between
4.5 and 8.1 *and* 4.5's clear has to move with it; it cannot be retrofitted
inside 8.7 alone.

**Updated 2026-09-05, from 8.8's side (`DetermineBestPatientSeriesTest`) - the
last unit of the chapter, so this update also closes the chapter out.** 8.8
confirms the list-choice table above by test, and it splits this entry's two
halves apart more cleanly than any earlier unit, because in 8.8 they land on
*different parts of the same class*:

- **The step's outer loop is correct on the antigen axis, and green.**
  `theStepEvaluatesOnlyThePrioritizedPatientSeriesOfTheAntigenBeingProcessed`
  passes: the constructor iterates `dataModel.getPrioritizedPatientSeriesList()`
  and skips any entry whose `getTargetDisease()` is not `dataModel.getAntigen()`.
  Worth noting that this filter is dead in practice - 4.5 clears the prioritized
  list on every antigen pass and 8.7 adds exactly one entry to it, so it can never
  have anything to exclude - but it is *correct*, which matters for the remedy
  below. That makes **three** of the eight Chapter 8 steps already right on the
  antigen axis (8.4, 8.7, 8.8), not two.
- **Its conditions are not.** The class's field initializer captures
  `dataModel.getPatientSeriesStepper().getList()`, and both of Table 8-14's
  "equivalent series group" conditions scan it. This is 8.2's "inconsistent
  *within* a class" observation in its sharpest form yet, and inconsistent in a
  new way: 8.2 was inconsistent between conditions of one table, 8.8 is
  inconsistent between the loop that *chooses what to judge* and the conditions
  that *judge it*. Worse, the two conditions do not even agree with each other -
  condition 2 filters `getTargetDisease().equals(dataModel.getAntigen())` before
  looking at completeness, and condition 5 has **no antigen filter of any kind**,
  so it answers Yes if any patient series anywhere in the assessment is a Risk
  series. `theRiskElsewhereConditionAsksOnlyAboutThePatientSeriesOfTheAntigen
  BeingProcessed` (red) is the cleanest single demonstration in the chapter: one
  Measles Risk series on the stepper stops a HepA Standard series being a best
  patient series.

8.8 also adds a **stage** consequence the earlier units could not, because 8.8 is
the only Chapter 8 step whose conditions ask about *prioritized* patient series
by name. Table 8-14's conditions 2 and 5 both begin "Is there a **prioritized**
patient series ..."; the implementation scans the stepper, which is 5.1's
unfiltered list of every relevant patient series for every antigen. So a series
8.1 excluded, or one that simply lost 8.7's selection for its group, still
suppresses the winner - `theCompleteSeriesConditionAsksOnlyAboutPrioritized
PatientSeries` and `theRiskElsewhereConditionAsksOnlyAboutPrioritizedPatient
Series`, both red. This is the same wrong-pipeline-stage error 8.4 and 8.7 have,
but 8.8 is where the specification names the right stage in the condition text
itself, so it needs no interpretation to call it: the correct list here is not
`scorablePatientSeriesList` either, it is `prioritizedPatientSeriesList` - the
list 8.8 already iterates in its own constructor and then does not consult in its
conditions.

**Closing the chapter (all eight units now have a Role A pass).** Three things
are worth recording now that the whole of Chapter 8 has been tested, which no
single unit's update could say:

1. **Every one of the eight steps is wrong about scope, and no two are wrong the
   same way.** Not one of the eight is correct on all three axes. The tally: on
   the *antigen* axis 8.4, 8.7 and 8.8's loop are right and 8.1, 8.2 (partly),
   8.3, 8.5, 8.6 and 8.8's conditions are wrong; on the *stage* axis only 8.1 (the
   step that builds the scorable list) is right by construction, and 8.4, 8.5,
   8.6, 8.7 and 8.8 all read a list from the wrong stage of the pipeline; on the
   *series group* axis all eight are wrong, because the loop does not exist. There
   is no consistent convention anywhere in the chapter to preserve or extend -
   8.2 filters in 3 of 4 conditions, 8.3 in 0 of 3, 8.8 in its loop but in only 1
   of its 2 outward-looking conditions - which retires for good the idea that the
   chapter-wide fix can be implemented by making each class agree with its
   neighbours.
2. **The severity escalates monotonically along the chapter, and the last step is
   the worst place for it.** 8.1/8.2 a contaminated count; 8.3 a flipped branch;
   8.4 a lost comparison; 8.5 a sticky boolean; 8.6 an outcome nobody can win;
   8.7 a stray series winning outright; 8.8 a *correct* winner being suppressed by
   a series that was never in the running. Every earlier step's damage is still
   recoverable by a later step in principle; 8.8's is not, because
   `bestPatientSeriesList` is what 4.6 and Chapter 9 consume and nothing revisits
   it.
3. **The remedy now has three parts, not two.** This entry has been tracking a
   scope (antigen) and a stage (which list), and 8.4's update added that the fix
   has to name a stage rather than only a scope. 8.8 adds the third: the series
   group loop cannot be introduced on its own either, because two of Table 8-14's
   five conditions are defined over *equivalent* series groups, and that value is
   never parsed from the Supporting Data at all. See the separate 2026-09-05
   entry on `<equivalentSeriesGroups>`. Sequencing consequence: introducing the
   series group loop without also loading the equivalence data would make 8.8
   unable to ask either of its cross-group questions, which is worse than today
   for the 10 antigens whose groups genuinely are equivalent.

**Known affected units:** 8.1 (confirmed, 4 of its 8 red tests), 8.2 (confirmed,
2 of its 5 red tests), 8.3 (confirmed, 2 of its 4 red tests), 8.4 (confirmed,
2 of its 8 red tests), 8.5 (confirmed, 3 of its 18 red tests), 8.6
(confirmed, 3 of its 18 red tests), **8.7** (confirmed 2026-09-05, 3 of its 5
red tests - one stage, two series group; its antigen scope is correct and green)
and **8.8** (confirmed 2026-09-05, 3 of its 6 red tests - one antigen, two stage;
its outer loop's antigen scope is correct and green, its conditions' is not; its
other 3 reds are the `<equivalentSeriesGroups>` entry above). All eight units of
Chapter 8 have now had a Role A pass.
4.5 is *not* affected - it does its half correctly, on the antigen axis, which is
the only half it owns.

**Suggested handling:** this is a sequencing note. The two halves are different
sizes: making the stepper-reading steps read one shared list instead is a small,
local change repeated in four classes, and would make Chapter 8 antigen-scoped as
Figure 4-7 already intends; introducing the series group loop
is a structural change to the chapter's control flow that has no owner in any
single unit (it belongs between 4.5 and 8.1, and `LogicStepFactory`'s dispatch
chain has no place to put it today). Both would retroactively resolve red tests
in units nobody has written yet, and per `cdsi-engine/AGENTS.md` neither can be
decided inside 8.1's own Role B session, since six of the eight affected classes
belong to other units. Worth deciding once, with 8.2-8.8's Role A results in
hand, rather than four times. The first 2026-09-05 update above adds a third,
smaller piece of work that *is* unit-local: 8.2's condition 0 should be made
consistent with its own other three conditions, whichever scope the chapter-wide
decision picks. 8.3 adds no such unit-local piece - having no filter anywhere, it
has nothing to make self-consistent - so 8.3's two scoping reds are resolvable
*only* by the chapter-wide decision, which makes 8.3 a useful test of whatever
that decision turns out to be. Per the 8.4 update above, the chapter-wide
decision now has to name a *stage* as well as a scope, and the answer for 8.2
onwards is almost certainly `scorablePatientSeriesList` (once 8.1 is made to
build it per antigen and per series group), not `selectedPatientSeriesList` -
8.1's whole purpose is to decide which series are scorable, and every rule in
8.2-8.7 is phrased over scorable patient series. Switching the four
stepper-reading classes to `selectedPatientSeriesList`, which this note
originally suggested, would fix their antigen scope while silently putting 8.1's
pre-filter back out of the loop for two more steps.

**Status:** open, not yet fixed, not yet a formal finding. Confirmed from 8.1's
side (2026-09-05), 8.2's side (2026-09-05), 8.3's side (2026-09-05), 8.4's
side (2026-09-05, which corrects the suggested remedy), 8.5's side
(2026-09-05, the first tested step that reads the stepper itself), 8.6's side
(2026-09-05, where a stray series makes the row's +1 unawardable to anyone),
8.7's side (2026-09-05, where a stray series wins the selection outright, and
where the antigen axis is confirmed already correct) and 8.8's side
(2026-09-05, where one class is correct in its loop and wrong in its conditions,
and which closes out the chapter - see the closing synthesis above). **Note the sequencing
constraint the score-accumulation entry below now places on this one: a partial
fix here - re-scoping 8.3 without also re-scoping 8.5/8.6 - would activate that
latent defect. See its 2026-09-05 update from 8.7's side.**

---

## 2026-09-05 - SELECTB-3's maximum age date is calculated three times outside the domain model, and every copy drops part of a compound age

**Discovered while testing:** 8.5 In-process Patient Series
(`InProcessPatientSeriesTest`)

**Affected component:** `InProcessPatientSeries.addTimePeriodtotoDate()` /
`findMaximumAgeDate()` and the byte-identical pair in `NoValidDoses`
(8.6), plus the two same-named `findMaximumAgeDate()` methods in
`DetermineForecastNeed` (7.4) and `GenerateForecastDatesAndRecommendedVaccines`
(7.5) - measured against `TimePeriod.getDateFrom(Date)` in the domain model,
which already does this calculation correctly and which none of them call.

**What's wrong:** SELECTB-3 makes a patient series completable when its forecast
finish date is before the maximum age date of the last target dose, so 8.5 and
8.6 both need "the date this patient reaches a given maximum age". Both compute
it with their own private copy of the same 20-line `switch`:

```java
switch (type) {
  case DAY:   date = DateUtils.addDays(date, amount);   break;
  case WEEK:  date = DateUtils.addWeeks(date, amount);  break;
  ...
}
```

which reads only `TimePeriod.getAmount()` and `TimePeriod.getType()` - the
outermost term. A `TimePeriod` parsed from "8 months + 1 day" holds `8`/`MONTH`
with the "1 day" in `getChild()`, and `getChild()` is never consulted, so the
"+ 1 day" is silently dropped. `TimePeriod.getDateFrom(Date)` recurses into the
child correctly, and additionally implements CALCDT-5 (roll a date past the end
of a short month forward to the 1st), which none of the private copies do.

**Volume:** the bundled 4.65-508 release defines 77 `<maxAge>` values, 8 of them
"8 months + 1 day" - so roughly one in ten maximum age dates 8.5 and 8.6 compute
is a day early. A one-day error only changes an outcome at the boundary, but
SELECTB-3 *is* a boundary comparison, and it is a strict one ("before"), so a
series that finishes on exactly the day it would otherwise age out flips from
completable to not completable, worth 6 points of swing in 8.5's Table 8-9 (+3
becomes -3) and 2 in 8.6's Table 8-11.

**Confirmed live in 8.5:**
`selectbThreeTheMaximumAgeDateIncludesEveryPartOfACompoundMaximumAge` (red) -
a patient born 01/01/2020 with an "8 months + 1 day" maximum age and a series
finishing 09/01/2020 is completable (it ages out 09/02/2020), expected +3,
actual -3 because the maximum age date is computed as 09/01/2020. Not observable
via FITS, which asserts the final forecast rather than which series won a
selection.

**Confirmed live in 8.6 (2026-09-05, `NoValidDosesTest`):** the prediction below
that 8.6 "has the same defect" is now confirmed by a test rather than by reading
the source. `selectbThreeTheMaximumAgeDateIncludesEveryPartOfACompoundMaximumAge`
(red) is the exact 8.5 fixture re-pointed at `NoValidDoses`: a patient born
01/01/2020 with an "8 months + 1 day" maximum age and a series finishing
09/01/2020 is completable (it ages out 09/02/2020), expected +1, actual -1
because `NoValidDoses.addTimePeriodtotoDate()` computes the maximum age date as
09/01/2020. The swing is 2 points here against 8.5's 6, exactly as predicted.
Two further 8.6 reds are the same `findMaximumAgeDate()` method read
differently and are *not* this entry - they are 8.6's own SELECTB-3/SELECTB-12
defects (`selectbThreeCompletabilityIsMeasuredAgainstTheLastTargetDosesMaximumAge
Date`, which reads the *forecast* target dose rather than the last one, and
`selectbTwelveTheForecastFinishDateIsTheEarliestDatePlusTheLatestMinimumInterval
Remaining`, which reads the adjusted past due date rather than SELECTB-12's
calculation) - but they land in the same two methods, so a Role B session that
routes the maximum age date through `TimePeriod.getDateFrom()` will be editing
the same lines. One thing 8.6 adds to the remedy: `findMaximumAgeDate()` is
called from **two** conditions in `NoValidDoses`, not one -
`evaluate_ACandidatePatientSeriesIsCompletable()` and the undocumented
`evaluate_ACandidatePatientSeriesHasExceededTheMaximumAge()` - so if the
undocumented condition is kept rather than removed, the compound-age fix changes
its answers too.

**Known affected units:** 8.5 (confirmed, 1 of its 18 red tests) and **8.6**
(confirmed 2026-09-05, 1 of its 18 red tests in `NoValidDosesTest`;
`NoValidDosesCompletableTest` does not cover it because that test class was
written for one specific always-increments defect and uses a simple "5 years"
maximum age throughout). 7.4 and 7.5 have differently-shaped
`findMaximumAgeDate()` methods that have not been checked against this.

**Suggested handling:** the sequencing point is that 8.5's and 8.6's copies are
identical, so fixing SELECTB-3 in 8.5's Role B session alone would leave 8.6
computing a different maximum age date from the same Supporting Data - the same
"implemented twice and the copies disagree" shape as the FORECASTDTCAN-1 entry
above, except here a correct canonical implementation already exists and is
simply not called. Recommend deciding 8.5's and 8.6's Role B sessions together,
with the maximum age date read once from `TimePeriod.getDateFrom()`. Note the
one behavioural caveat: `getDateFrom()` also applies CALCDT-5, so switching to
it changes more than the compound-age cases and needs a FITS regression check
even though neither 8.5 nor 8.6 is FITS-observable on its own.

**Status:** open, not yet fixed, not yet a formal finding. Predicted from 8.5's
side (2026-09-05) and confirmed live from 8.6's side (2026-09-05).

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
different unit's class than the loader fix. (Confirmed 2026-09-05 that those
three changes are also *enough* on 8.1's side - see the update below.)
Downstream matters too:
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

**Updated 2026-09-05, from 8.1's side (`PreFilterPatientSeriesTest`).** The
sentence above predicting that 8.1 `PreFilterPatientSeries` is "dead too until
this chain is closed end to end" holds, and 8.1's Role A pass adds two things to
it. First, the good news for sequencing: 8.1's *consumer* side is correct and
needs no change of its own. Driving 8.1 with `PatientSeriesStatus
.CONTRAINDICATED` hand-built onto the patient series - i.e. simulating the state
7.4's Rule 5 would set once the chain closes - makes both simple halves of
SELECTB-24 behave exactly as Table 8-2 says, and both are green
(`selectbTwentyFourAContraindicatedSeriesIsNotACandidateWhenASiblingInItsGroup
IsNot` and `selectbTwentyFourContraindicatedSeriesStayCandidatesWhenEverySeries
InTheGroupIsContraindicated`). So the loader/type/`DetermineForecastNeed`
remedy described above is sufficient to bring 8.1's contraindication filtering
to life; no fourth change is needed in `PreFilterPatientSeries` to consume it.
Second, the caveat: closing the chain would still not deliver all of SELECTB-24,
because the rule's all-contraindicated escape hatch is scoped to one series
group and 8.1 decides it globally. That is a second, entirely independent defect
in the consumer, with its own cause and its own remedy - see the 2026-09-05
entry on Chapter 8's missing series group - so it should not be folded into this
entry's remedy or used to argue this entry's fix is incomplete. Note also that
8.1 reads `CONTRAINDICATED` and nothing else: `IMMUNE`, `AGED_OUT` and
`NOT_RECOMMENDED` are not part of SELECTB-24, so the immunity half of this entry
has no 8.1-side consumer to wake up at all.

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
7.4's immunity-side read is green and needs no change). **8.1** is a confirmed
downstream *consumer* but contributes **no** red tests of its own to this entry -
its two SELECTB-24 tests are green once the status is supplied, which is the
point of the 2026-09-05 update above.

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
immunity (2026-09-04) and contraindication (2026-09-04) sides, from the
consuming side in 7.4 (2026-09-04) and from the Chapter 8 consuming side in 8.1
(2026-09-05, where the remedy is confirmed sufficient); the two are the same
routing cause with materially different remedies, and the contraindication
remedy additionally reaches into `DetermineForecastNeed`.

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

**Updated 2026-09-05, from 8.6's full Role A pass (`NoValidDosesTest`).** This
entry asked whoever reached 8.6 to check it deliberately; here is what 8.6's own
vantage point can and cannot settle. What it confirms: the increment/decrement
shape is real and now pinned by a green test in both 8.5 and 8.6
(`theScoreIsARunningTotalTheStepIncrementsOrDecrementsRatherThanSets` - a series
carrying a score of 5 comes out of the completable row on 6, not on 1), so
Table 8-11's outcomes are unambiguously applied to whatever the series already
holds. What 8.6 adds that reading the field could not show: **the accumulation
and the tie column interact.** Table 8-11's first row is the only row in the
whole 8.4/8.5/8.6 family whose tie outcome is a literal "0", and the only way to
implement "0" against a running total is to leave the series untouched - which
is indistinguishable from "this row did not run", and, if the score did not
start at a known value, indistinguishable from any prior state as well. So the
question this entry raises is not merely cosmetic for 8.6: whether the +1/0/-1
row means anything at all depends on where the score started.

What 8.6 still cannot settle is materiality, for the reason this entry
anticipated - a unit test hands the step a hand-built `DataModel` and therefore
always starts from 0. Confirming the accumulation changes a real selection needs
either 8.7 (`SelectPrioritizedPatientSeries`, which reads the score to pick a
winner) or a whole-assessment run, neither of which is in 8.6's scope.

**Updated 2026-09-05, from 8.7's side
(`SelectPrioritizedPatientSeriesTest`) - materiality settled.** 8.5's and 8.6's
passes both flagged 8.7 as the place to answer this, because 8.7 is the step
that reads the score to pick a winner. It is answered here: **under the
implementation as it stands, the unreset accumulation cannot change which
patient series 8.7 selects.** The argument is not a unit test's word for it (a
hand-built `DataModel` always starts at 0, exactly as 8.6 said) but a whole-loop
one, assembled from four facts each verifiable by reading source:

1. **The score has exactly one reader in the entire engine, and it is 8.7.**
   `getScorePatientSeries()` appears twice in `cdsi-engine`/`cdsi-web`, both in
   `SelectPrioritizedPatientSeries` (lines 35 and 47). `addScore(int)` and
   `setScorePatientSeriesScore(int)` are declared on `PatientSeries` and called
   by nothing at all - so the score is written only by 8.4/8.5/8.6's
   `incPatientScoreSeries`/`descPatientScoreSeries`, and no *condition* anywhere
   reads it. The per-pass delta a series receives is therefore independent of the
   value it already carries.
2. **Every antigen pass presents 8.1-8.6 identical input.** `PreFilterPatientSeries`
   (8.1) reads only the never-re-scoped all-antigen stepper and per-series state
   that Chapter 8 does not mutate (it never reads `dataModel.getAntigen()`), so
   `scorablePatientSeriesList` is rebuilt to identical content on every pass;
   `ClassifyScorablePatientSeries` (8.3) filters nowhere, so it routes to the
   *same one* of 8.4/8.5/8.6 on every pass; and 8.5/8.6 read that same stepper,
   so they award every series in it the same delta every time. Nothing in Chapter
   8 writes `PatientSeriesStatus` (the only writers are 7.2, 7.4 and the vaccine
   group steps, all of which run outside this loop) and 8.8 only appends to
   `bestPatientSeriesList`.
3. **Therefore `score_i = M · d_i`**, where `d_i` is series `i`'s single-pass
   delta and `M` is one *global* count of how many antigen passes reached the
   scoring step - global because on any given pass either every series in the
   list is scored or none is (8.2 can short-circuit a whole pass to 8.8, which
   skips scoring for everybody equally).
4. **8.7's comparison is invariant under that scaling.** Multiplying every
   candidate's delta by the same positive `M` moves neither the maximum nor the
   tie set, so both SELECTBEST-2 clauses - "highest score" and the
   series-preference tie-break that fires only on `==` - reach the same answer
   they would after a single pass. (If `M` is 0 every candidate is on 0, which is
   also what one unscored pass would give.)

`selectbestOneAnAccumulationScaledEquallyAcrossCandidatesDoesNotChangeTheWinner`
(green) pins step 4 as a regression test, on both a three-way ranking and a
genuine tie. `selectbestOneTheScoreIsThePointsAwardedInThisSelectionNotOnesCarriedIn`
(red) pins the counterfactual: 8.7 compares the raw accumulated integers with
`==` and `>` against no baseline whatsoever, so the moment two candidates of one
selection *do* carry different histories, the history decides - a series awarded
nothing this selection but carrying 5 points beats one awarded +1.

**So this is confirmed non-material today, and the reason is worth reading
twice: it is non-material only because the Chapter 8 scoping defect makes every
pass identical.** The two open entries in this file cancel each other out. That
converts this entry from an open question into a **sequencing constraint on the
other one**, which is the actionable part:

- Re-scoping 8.5/8.6 to a per-antigen (or per-series-group) list, which is what
  the "Chapter 8 has no series group" entry recommends, is **safe** on this axis:
  each series would then be scored exactly once, and once is trivially uniform.
- **A partial remediation is not safe.** Concretely: making 8.3's counts
  antigen-scoped (a change that entry's 8.2 update already contemplates for a
  neighbouring class) while leaving 8.5/8.6 on the stepper breaks fact 2 above -
  different antigen passes would then route to different scoring steps, and since
  8.4 reads the antigen-scoped `selectedPatientSeriesList` while 8.5/8.6 read the
  all-antigen stepper, one antigen's series would accrue one 8.4-shaped delta
  *plus* several 8.6-shaped deltas earned on other antigens' passes. Those sums
  are no longer a positive multiple of any single ranking, and the foreign-pass
  points can outvote the verdict of the step that was actually meant to score
  that group. **At that point this entry becomes material and 8.7 starts
  selecting the wrong series.**
- The same is true of any future change that makes a *condition* read the score,
  which would break fact 1.

One loose end this pass also closes, from 8.6's update above: the worry that
Table 8-11 row 1's literal "0" tie outcome is "indistinguishable from any prior
state" does not bite in practice, because "0" contributes 0 to `d_i` and so stays
0 under the scaling - the row is uniformly inert rather than unpredictably so.

**Known affected units:** 8.5 and 8.6 (the increment/decrement behaviour is
pinned green in both; neither confirms the accumulation changes an outcome) and
**8.7** (confirmed 2026-09-05: the consumer's comparison is on raw accumulated
totals - 1 red test - but the accumulation is provably uniform today, so no
selection is changed - 1 green test pinning that). **8.8** had its Role A pass on
2026-09-05 and the prediction above holds exactly: `DetermineBestPatientSeries`
reads no score at all (Table 8-14's five conditions read `PatientSeriesStatus`
and `SeriesType` only, and the class contains no reference to
`getScorePatientSeries`), so it contributes no red or green test here and adds
nothing to this entry. That also confirms fact 1 of the materiality argument
above from the last remaining direction: with all eight Chapter 8 units now
tested, 8.7 really is the only reader of the score in the chapter, and no
*condition* anywhere reads it.

**Suggested handling:** no longer "needs its own investigation". Two things
follow instead. First, **resetting the score is not urgently needed and should
not be fixed on its own**: on today's code it changes no outcome, so a standalone
reset would be a behaviour-neutral tidy-up with a FITS regression risk and no
payoff. Second, and the part that matters, **it must be fixed at the same time as
the Chapter 8 scope decision, or that decision must be taken in the safe
direction described above** - because a partial scope fix is exactly what turns
this latent problem into a live one. The cheapest way to make the whole question
moot is to give `PatientSeries` a score reset at the top of each selection (or,
equivalently, to have 8.4/8.5/8.6 score into a per-selection map rather than onto
the series), which removes the dependency on every pass being identical and lets
the scope fix be sequenced freely. Recommend recording that as a precondition of
the "Chapter 8 has no series group" remedy rather than as a unit-local item, since
no single unit's Role B session owns both classes.

**Status:** open, behaviour confirmed 2026-09-05 from 8.6's side; **materiality
resolved 2026-09-05 from 8.7's side - confirmed non-material under the current
implementation, and confirmed to become material under a partial fix of the
Chapter 8 scope entry.** Not yet a formal finding. What remains is a sequencing
decision, not an investigation.
