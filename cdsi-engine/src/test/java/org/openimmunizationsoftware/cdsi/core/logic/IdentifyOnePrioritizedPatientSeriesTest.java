package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.YesNo;

/**
 * Section 8.2 "Identify One Prioritized Patient Series" (Logic Specification for
 * ACIP Recommendations v4.6, pages 87-88; Table 8-3 "Is There a Single
 * Prioritized Patient Series in a Series Group?", Table 8-4 business rules
 * SELECTB-6, SELECTB-7 and SELECTB-16) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-02-identify-one-prioritized-patient-series/index.md}.
 *
 * <p>
 * 8.2 is a shortcut check placed between 8.1 and the 8.3-8.7 scoring family: it
 * counts four things about the series group (how many scorable, default,
 * complete and in-process patient series it holds) and, when one of Table 8-3's
 * five rules matches, names the winner immediately and skips straight to 8.8.
 * Only the table's default outcome - genuine competition between series -
 * continues into 8.3. The tests below are organised one per Table 8-4 business
 * rule, then one per Table 8-3 rule column plus its default outcome, then the
 * two scoping sentences the section's Purpose states ("all of the patient
 * series for a given Series Group"), with each test asserting both halves of
 * the documented State Changes and Next Steps: what lands in
 * {@code prioritizedPatientSeriesList} and which step the branch goes to.
 *
 * <p>
 * <b>Isolation.</b> {@link IdentifyOnePrioritizedPatientSeries} reads exactly
 * two things from the {@code DataModel} -
 * {@code getScorablePatientSeriesList()} (which 8.1 builds) and
 * {@code getAntigen()} (which 4.5 sets) - plus, per patient series, its
 * {@code patientSeriesStatus}, its {@code targetDoseList} and its tracked
 * {@code AntigenSeries}' {@code targetDisease} and {@code SelectPatientSeries}
 * default-series flag. Every test hand-builds that shape and drives the public
 * {@code process()} directly; no Supporting Data release, no loader and no
 * upstream step is involved. {@code process()} ends in {@code next()}, which
 * constructs the next step but never runs it, so the returned object is used
 * only for its {@code LogicStepType}.
 *
 * <p>
 * <b>Relevant versus scorable patient series.</b> Table 8-4 draws a distinction
 * the tests rely on: SELECTB-6 and SELECTB-16 define "complete" and "in-process"
 * over a <i>scorable</i> patient series, while SELECTB-7 defines "default" over
 * a <i>relevant</i> patient series. Table 8-3's Rule 1 confirms the contrast is
 * deliberate rather than loose wording - it asks for a group with zero scorable
 * patient series and one default patient series, which can only be satisfied if
 * a default patient series need not be scorable. The fixture below therefore
 * keeps the two populations separate: {@code relevantSeries} adds a patient
 * series to the patient series stepper only (where 5.1 leaves the relevant
 * patient series), and {@code scorableSeries} adds it to both the stepper and
 * {@code scorablePatientSeriesList}.
 *
 * <p>
 * <b>Deliberately not covered.</b> SELECTB-16 says "at least one target dose
 * with status 'Satisfied'", whereas the implementation tests
 * {@code getSatisfiedByVaccineDoseAdministered() != null}. The only writer of
 * either is {@code SatisfyTargetDose}, which sets both on the same code path
 * (lines 115 and 117), so a target dose that is SATISFIED without a satisfying
 * vaccine dose administered - or the reverse - is not a reachable state, and a
 * test distinguishing the two readings would assert nothing about real
 * behaviour. Recorded in this unit's {@code status.yaml} notes instead.
 */
public class IdentifyOnePrioritizedPatientSeriesTest {

  /** Series group names as they appear in the bundled Supporting Data. */
  private static final String STANDARD_GROUP = "Standard";
  private static final String INCREASED_RISK_GROUP = "Increased Risk";

  private DataModel dataModel;
  private Antigen hepB;
  private List<PatientSeries> scorablePatientSeriesList;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    hepB = dataModel.getOrCreateAntigen("HepB");
    // What 4.5 has set by the time Chapter 8 runs, and what 8.1 leaves behind.
    dataModel.setAntigen(hepB);
    scorablePatientSeriesList = new ArrayList<PatientSeries>();
    dataModel.setScorablePatientSeriesList(scorablePatientSeriesList);
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the minimal shape 8.2 actually reads.
  // ---------------------------------------------------------------------

  /**
   * A relevant patient series: one that 5.1 created and 4.4 forecast, but which
   * 8.1 did not promote into the scorable list.
   */
  private PatientSeries relevantSeries(String seriesName, Antigen targetDisease, String seriesGroupName,
      PatientSeriesStatus status, YesNo defaultSeries) {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroupName(seriesGroupName);
    selectPatientSeries.setSeriesGroup(seriesGroupName);
    selectPatientSeries.setDefaultSeries(defaultSeries);

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setSeriesType(SeriesType.STANDARD);
    antigenSeries.setTargetDisease(targetDisease);
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(status);
    patientSeries.setTargetDoseList(new ArrayList<TargetDose>());
    dataModel.getPatientSeriesStepper().add(patientSeries);
    return patientSeries;
  }

  /** A relevant patient series that 8.1 also made scorable. */
  private PatientSeries scorableSeries(String seriesName, Antigen targetDisease, String seriesGroupName,
      PatientSeriesStatus status, YesNo defaultSeries) {
    PatientSeries patientSeries = relevantSeries(seriesName, targetDisease, seriesGroupName, status, defaultSeries);
    scorablePatientSeriesList.add(patientSeries);
    return patientSeries;
  }

  /** A scorable HepB series of the Standard series group, not the default one. */
  private PatientSeries scorableSeries(String seriesName, PatientSeriesStatus status) {
    return scorableSeries(seriesName, hepB, STANDARD_GROUP, status, YesNo.NO);
  }

  /** A scorable HepB series of the Standard series group flagged as the default. */
  private PatientSeries scorableDefaultSeries(String seriesName, PatientSeriesStatus status) {
    return scorableSeries(seriesName, hepB, STANDARD_GROUP, status, YesNo.YES);
  }

  /**
   * Gives the patient series one target dose in the state SELECTB-16 calls
   * "a target dose with status 'Satisfied'".
   */
  private static TargetDose satisfiedTargetDose(PatientSeries patientSeries) {
    TargetDose targetDose = new TargetDose();
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    targetDose.setSatisfiedByVaccineDoseAdministered(new VaccineDoseAdministered());
    patientSeries.getTargetDoseList().add(targetDose);
    return targetDose;
  }

  /** A target dose that no vaccine dose administered has satisfied. */
  private static TargetDose notSatisfiedTargetDose(PatientSeries patientSeries) {
    TargetDose targetDose = new TargetDose();
    targetDose.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    patientSeries.getTargetDoseList().add(targetDose);
    return targetDose;
  }

  private LogicStep process() throws Exception {
    return new IdentifyOnePrioritizedPatientSeries(dataModel).process();
  }

  private static List<String> seriesNamesOf(List<PatientSeries> patientSeriesList) {
    List<String> names = new ArrayList<String>();
    for (PatientSeries patientSeries : patientSeriesList) {
      names.add(patientSeries.getTrackedAntigenSeries().getSeriesName());
    }
    return names;
  }

  private List<String> prioritizedSeriesNames() {
    return seriesNamesOf(dataModel.getPrioritizedPatientSeriesList());
  }

  // ---------------------------------------------------------------------
  // Table 8-4 - the three business rules the decision table's counts rest on
  // ---------------------------------------------------------------------

  /**
   * SELECTB-6: "A scorable patient series is a complete patient series if its
   * forecast has patient series status 'Complete.'" SELECTB-16 defines
   * in-process as Satisfied target doses <i>and</i> a 'Not Complete' status, so
   * the two categories cannot overlap: a Complete series that also has satisfied
   * target doses is counted as complete and not as in-process, which is what
   * makes Table 8-3's Rule 3 (one complete series) fire here rather than its
   * Rule 4 (one in-process series).
   */
  @Test
  public void selectbSixACompleteSeriesWithSatisfiedTargetDosesCountsAsCompleteNotAsInProcess() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE));
    scorableSeries("HepB open", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals(Arrays.asList("HepB complete"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * SELECTB-7 from the negative side: "A relevant patient series is a default
   * patient series if the default series flag is 'Y' for the antigen series." A
   * group whose every series carries 'N' has zero default patient series, so
   * Table 8-3's Rule 5 (which needs exactly one) cannot fire and, with nothing
   * complete or in-process either, no rule matches at all.
   */
  @Test
  public void selectbSevenASeriesWhoseDefaultSeriesFlagIsNoIsNotADefaultPatientSeries() throws Exception {
    scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE);
    scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals("No default patient series, so Rule 5 cannot identify one",
        Collections.emptyList(), prioritizedSeriesNames());
    assertEquals(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * SELECTB-7 defines a default patient series over the <i>relevant</i> patient
   * series - unlike SELECTB-6 and SELECTB-16, which both say "scorable patient
   * series" - and Table 8-3's Rule 1 confirms that a default patient series need
   * not itself be scorable, since Rule 1 pairs zero scorable series with one
   * default series. Here the group holds two non-default scorable series and one
   * relevant-but-not-scorable series flagged 'Y': the default count is 1, and
   * with nothing complete or in-process Rule 5 identifies that default series.
   *
   * <p>
   * The implementation counts default series by walking
   * {@code scorablePatientSeriesList}, so a default series that 8.1 did not
   * promote is invisible to it: the count comes back 0, no rule matches, and the
   * step falls through to scoring.
   */
  @Test
  public void selectbSevenADefaultPatientSeriesNeedNotBeAScorablePatientSeries() throws Exception {
    scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE);
    scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE);
    relevantSeries("HepB default", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE, YesNo.YES);

    LogicStep next = process();

    assertEquals("SELECTB-7 counts default series among the relevant patient series, not the scorable ones",
        Arrays.asList("HepB default"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * SELECTB-16: an in-process patient series "has at least one target dose with
   * status 'Satisfied' AND its forecast has patient series status 'Not
   * Complete.'" A Not Complete series whose only target dose is not satisfied
   * fails the first half, so the group holds zero in-process series and Table
   * 8-3's Rule 4 cannot fire.
   */
  @Test
  public void selectbSixteenASeriesWithNoSatisfiedTargetDoseIsNotAnInProcessPatientSeries() throws Exception {
    notSatisfiedTargetDose(scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE));
    scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals("No target dose is Satisfied, so there is no in-process patient series",
        Collections.emptyList(), prioritizedSeriesNames());
    assertEquals(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * SELECTB-16 makes the <i>patient series</i> in-process ("a scorable patient
   * series is an in-process patient series if it has at least one target
   * dose..."), and Table 8-3's third condition then asks "how many in-process
   * <i>patient series</i> are in the series group?" - so a series with three
   * satisfied target doses is one in-process patient series, not three. With a
   * second, doseless series present the group has exactly one in-process series
   * and Rule 4 identifies it.
   *
   * <p>
   * The implementation increments its counter once per satisfied target dose
   * rather than once per series, so this group counts as three in-process
   * series, Rule 4's "1" is not matched, and the step falls through to scoring.
   * That makes Rule 4 reachable only for a series satisfied by exactly one dose
   * - the first dose of a multi-dose series - and unreachable for every
   * partially-completed series beyond its first dose, which is the ordinary case
   * the rule exists to catch.
   */
  @Test
  public void selectbSixteenCountsInProcessPatientSeriesNotSatisfiedTargetDoses() throws Exception {
    PatientSeries inProcess = scorableSeries("HepB in process", PatientSeriesStatus.NOT_COMPLETE);
    satisfiedTargetDose(inProcess);
    satisfiedTargetDose(inProcess);
    satisfiedTargetDose(inProcess);
    scorableSeries("HepB untouched", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals("One series with three satisfied target doses is one in-process patient series",
        Arrays.asList("HepB in process"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  // ---------------------------------------------------------------------
  // Table 8-3 - one test per rule column, plus the default outcome
  // ---------------------------------------------------------------------

  /**
   * Table 8-3, Rule 1: zero scorable patient series and one default patient
   * series in the series group yields "Yes (default series)". This is the rule
   * that keeps a series group from disappearing entirely when 8.1's pre-filter
   * promoted nothing - the group still reports its default series to 8.8 rather
   * than entering the scoring family with nothing to score.
   *
   * <p>
   * The implementation counts default patient series by iterating
   * {@code scorablePatientSeriesList}, the same list whose emptiness is Rule 1's
   * first condition, so the two conditions contradict each other: whenever the
   * scorable count is 0 the default count is necessarily 0 as well, never the 1
   * Rule 1 requires. Rule 1's column can therefore never validate, and its
   * outcome body (the only one of the five that does not filter by
   * {@code dataModel.getAntigen()}) is dead code.
   */
  @Test
  public void ruleOneAGroupWithNoScorableSeriesButOneDefaultSeriesYieldsThatDefaultSeries() throws Exception {
    relevantSeries("HepB default", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE, YesNo.YES);

    LogicStep next = process();

    assertEquals(Arrays.asList("HepB default"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * Table 8-3, Rule 2: one scorable patient series in the series group yields
   * "Yes" - that series is the prioritized patient series, whatever its status,
   * because there is nothing for it to compete with.
   */
  @Test
  public void ruleTwoTheSingleScorablePatientSeriesIsThePrioritizedPatientSeries() throws Exception {
    scorableSeries("HepB only", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals(Arrays.asList("HepB only"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * Table 8-3, Rule 3: more than one scorable patient series and exactly one
   * complete patient series yields "Yes (the single complete series)". A
   * completed series beats every incomplete competitor without scoring.
   */
  @Test
  public void ruleThreeTheSingleCompletePatientSeriesIsThePrioritizedPatientSeries() throws Exception {
    scorableSeries("HepB open", PatientSeriesStatus.NOT_COMPLETE);
    scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE);
    scorableSeries("HepB also open", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals(Arrays.asList("HepB complete"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * Rule 3's condition is "how many complete patient series are in the series
   * group?" = 1, not ">= 1". Two complete series are exactly the competition
   * 8.3-8.7 exists to resolve, so no rule of Table 8-3 matches and 8.2 must not
   * pick a winner itself.
   */
  @Test
  public void ruleThreeDoesNotFireWhenMoreThanOnePatientSeriesIsComplete() throws Exception {
    scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE);
    scorableSeries("HepB also complete", PatientSeriesStatus.COMPLETE);
    scorableSeries("HepB open", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals("Two complete series is real competition, which 8.3-8.7 resolves",
        Collections.emptyList(), prioritizedSeriesNames());
    assertEquals(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * Table 8-3, Rule 4: more than one scorable patient series, zero complete
   * patient series and exactly one in-process patient series yields "Yes (the
   * single in-process series)". The one series the patient has actually started
   * wins over series they have not.
   */
  @Test
  public void ruleFourTheSingleInProcessPatientSeriesIsThePrioritizedPatientSeries() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB in process", PatientSeriesStatus.NOT_COMPLETE));
    scorableSeries("HepB untouched", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals(Arrays.asList("HepB in process"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * Table 8-3, Rule 5: more than one scorable patient series, one default
   * patient series, zero complete and zero in-process yields "Yes (the default
   * series)". With no history to go on and nothing finished, the group's
   * declared default is the answer.
   */
  @Test
  public void ruleFiveTheDefaultPatientSeriesWinsWhenNothingIsCompleteOrInProcess() throws Exception {
    scorableSeries("HepB alternate", PatientSeriesStatus.NOT_COMPLETE);
    scorableDefaultSeries("HepB default", PatientSeriesStatus.NOT_COMPLETE);

    LogicStep next = process();

    assertEquals(Arrays.asList("HepB default"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * Table 8-3's default outcome: "No - more than one scorable patient series has
   * potential; proceed to scoring." Two in-process series with no default and
   * nothing complete match no rule column, so - per this step package's State
   * Changes ("when the default outcome fires, nothing is added here - 8.3 onward
   * is responsible") and its Next Steps - the prioritized list is left untouched
   * and control passes to 8.3.
   */
  @Test
  public void theDefaultOutcomeAddsNoPrioritizedSeriesAndContinuesToTheScoringFamily() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE));

    LogicStep next = process();

    assertTrue("8.2 identified no single prioritized series, so it must add none",
        dataModel.getPrioritizedPatientSeriesList().isEmpty());
    assertEquals(LogicStepType.CLASSIFY_SCORABLE_PATIENT_SERIES, next.getLogicStepType());
  }

  // ---------------------------------------------------------------------
  // Purpose - the scope 8.2 counts over
  // ---------------------------------------------------------------------

  /**
   * Purpose: "Identify one prioritized patient series examines all of the
   * patient series <b>for a given Series Group</b>", and every one of Table
   * 8-3's four conditions is phrased "...are in the series group?". Chapter 8's
   * overview says the same - "Process steps 8.1 through 8.7 are repeated for
   * each series group". So a run of 8.2 for the Standard group must see one
   * scorable series, not two, when the Increased Risk group also has one, and
   * Rule 2 must identify the Standard group's series.
   *
   * <p>
   * Nothing in {@code cdsi-engine} reads
   * {@code SelectPatientSeries.getSeriesGroup()}, and 8.2 counts over the whole
   * of {@code scorablePatientSeriesList}, so both groups' series are counted
   * together: the scorable count is 2, Rule 2 does not match, and with nothing
   * complete, in-process or default the step falls through to scoring a mixture
   * of two series groups. See the 2026-09-05 "Chapter 8 has no series group"
   * entry in {@code cdsi-reference/step-tests/cross-cutting-notes.md}.
   */
  @Test
  public void theStepCountsOnlyThePatientSeriesOfOneSeriesGroup() throws Exception {
    scorableSeries("HepB standard", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE, YesNo.NO);
    scorableSeries("HepB risk", hepB, INCREASED_RISK_GROUP, PatientSeriesStatus.NOT_COMPLETE, YesNo.NO);

    LogicStep next = process();

    assertEquals("One run of 8.2 counts one series group, which holds a single scorable series",
        Arrays.asList("HepB standard"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * Chapter 8 runs inside 4.5's per-antigen loop, so every count Table 8-3 takes
   * is over one antigen's series. 8.2's own class already agrees with that for
   * three of its four conditions - the default, complete and in-process counts
   * each skip any series whose tracked antigen series' target disease is not
   * {@code dataModel.getAntigen()} - and for four of its five outcome bodies.
   * Its first condition, the scorable count, has no such filter, so this test
   * pins the one condition against the three that surround it: with the HepB
   * pass in progress, a Measles series left in the scorable list must not be
   * counted, and Rule 2 must identify the single HepB series.
   *
   * <p>
   * The scorable count comes back as 2, so Rule 2 does not match; the other
   * three conditions then do filter to HepB and report zero default, zero
   * complete and zero in-process, which matches no other rule either, and the
   * step falls through to scoring. The mixed list itself is 8.1's doing (it
   * reads the all-antigen patient series stepper), but the asymmetry inside 8.2
   * is this class's own: whichever way the scope question is settled, one of
   * these four conditions is wrong.
   */
  @Test
  public void theScorableSeriesCountIsScopedToTheAntigenBeingProcessedLikeTheOtherThreeConditions() throws Exception {
    Antigen measles = dataModel.getOrCreateAntigen("Measles");
    scorableSeries("HepB standard", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE, YesNo.NO);
    scorableSeries("Measles standard", measles, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE, YesNo.NO);

    LogicStep next = process();

    assertEquals("8.2 runs inside 4.5's per-antigen loop, so only HepB's series is in scope",
        Arrays.asList("HepB standard"), prioritizedSeriesNames());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }
}
