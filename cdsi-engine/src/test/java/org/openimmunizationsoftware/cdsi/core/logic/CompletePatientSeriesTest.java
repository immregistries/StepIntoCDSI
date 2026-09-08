package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesDose;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.VaccineDoseAdministered;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.TargetDoseStatus;

/**
 * Section 8.4 "Complete Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, pages 89-90; Table 8-7 "How Many Points Are Awarded to a
 * Scorable Patient Series That Is a Complete Patient Series?" and Table 8-8
 * business rules SELECTB-6 and SELECTB-19) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-04-complete-patient-series/index.md}.
 *
 * <p>
 * 8.4 is the first member of the 8.4/8.5/8.6 scoring family. It runs when 8.3
 * found two or more complete patient series in the series group, and it has
 * exactly one condition - "has the most valid doses" (SELECTB-19) - scored
 * +1/0/-1 across Table 8-7's three outcome columns: true for this series alone,
 * true for two or more series (a tie), and not true for this series. The
 * observable output is therefore a single integer per patient series,
 * {@code PatientSeries.getScorePatientSeries()}, which 8.7 later consumes.
 *
 * <p>
 * The tests are organised one per Table 8-8 business rule (with the negative
 * direction where the rule has one), then one per Table 8-7 outcome column plus
 * each column's boundary, then the two scoping questions the section's Purpose
 * and Entry Conditions raise, then the documented State Changes and Next Steps.
 *
 * <p>
 * <b>Isolation.</b> {@link CompletePatientSeries} reads exactly one thing from
 * the {@code DataModel} - {@code getSelectedPatientSeriesList()} - plus, per
 * patient series, its {@code patientSeriesStatus} and its
 * {@code targetDoseList}. Unlike its neighbours it dereferences nothing else
 * (its logging prints counts rather than series names), but the fixture supplies
 * each series' tracked {@code AntigenSeries} and each target dose's tracked
 * {@code SeriesDose} anyway, so the shape matches what 4.5 and Chapter 7
 * actually leave behind. Every test hand-builds that shape and drives the public
 * {@code process()} directly; no Supporting Data release, no loader and no
 * upstream step is involved. {@code process()} ends in {@code next()}, which
 * constructs 8.7 but never runs it, so the returned object is used only for its
 * {@code LogicStepType}.
 *
 * <p>
 * <b>Note on list order.</b> Several tests below build the same two-series group
 * in the two possible orders and assert the same outcome for each. That is
 * deliberate rather than redundant: Table 8-7 awards points on a property of a
 * series ("has the most valid doses"), not on where the series happens to sit in
 * a list, so a specification-conforming implementation must give the same answer
 * either way. The implementation's scoring loop {@code break}s immediately after
 * awarding its single +1, which makes the answer order-dependent - see this
 * unit's {@code status.yaml} notes and 08-04's "Review Findings".
 */
public class CompletePatientSeriesTest {

  /** Series group names as they appear in the bundled Supporting Data. */
  private static final String STANDARD_GROUP = "Standard";
  private static final String INCREASED_RISK_GROUP = "Increased Risk";

  private DataModel dataModel;
  private Antigen hepB;
  private List<PatientSeries> selectedPatientSeriesList;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    hepB = dataModel.getOrCreateAntigen("HepB");
    // The state 4.5 SelectBestPatientSeries leaves behind for one antigen pass.
    // 8.4 reads scorablePatientSeriesList (8.1's output), not this one.
    dataModel.setAntigen(hepB);
    selectedPatientSeriesList = new ArrayList<PatientSeries>();
    dataModel.setSelectedPatientSeriesList(selectedPatientSeriesList);
    dataModel.setScorablePatientSeriesList(new ArrayList<PatientSeries>());
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the minimal shape 8.4 actually reads.
  // ---------------------------------------------------------------------

  /**
   * A patient series of a named series group and series type, carrying
   * {@code satisfiedDoses} target doses in the state SELECTB-19's valid dose
   * count counts, added to the list 4.5 hands to Chapter 8.
   */
  private PatientSeries series(String seriesName, PatientSeriesStatus status, String seriesGroupName,
      SeriesType seriesType, String seriesPriority, int satisfiedDoses) {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroupName(seriesGroupName);
    selectPatientSeries.setSeriesGroup(seriesGroupName);
    selectPatientSeries.setSeriesPriority(seriesPriority);

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setSeriesType(seriesType);
    antigenSeries.setTargetDisease(hepB);
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(status);
    patientSeries.setTargetDoseList(new ArrayList<TargetDose>());
    for (int i = 0; i < satisfiedDoses; i++) {
      satisfiedTargetDose(patientSeries);
    }
    selectedPatientSeriesList.add(patientSeries);
    dataModel.getScorablePatientSeriesList().add(patientSeries);
    return patientSeries;
  }

  /**
   * A complete patient series (SELECTB-6) of the Standard series group with the
   * given number of valid doses.
   */
  private PatientSeries completeSeries(String seriesName, int satisfiedDoses) {
    return series(seriesName, PatientSeriesStatus.COMPLETE, STANDARD_GROUP, SeriesType.STANDARD, null,
        satisfiedDoses);
  }

  /** A Standard-group patient series whose forecast status is not 'Complete'. */
  private PatientSeries notCompleteSeries(String seriesName, int satisfiedDoses) {
    return series(seriesName, PatientSeriesStatus.NOT_COMPLETE, STANDARD_GROUP, SeriesType.STANDARD, null,
        satisfiedDoses);
  }

  /**
   * Gives the patient series one target dose in the state SELECTB-19's valid
   * dose count counts - "a target dose status of 'Satisfied'".
   */
  private static TargetDose satisfiedTargetDose(PatientSeries patientSeries) {
    TargetDose targetDose = newTargetDose(patientSeries);
    targetDose.setTargetDoseStatus(TargetDoseStatus.SATISFIED);
    targetDose.setSatisfiedByVaccineDoseAdministered(new VaccineDoseAdministered());
    return targetDose;
  }

  /** A target dose that no vaccine dose administered has satisfied. */
  private static TargetDose notSatisfiedTargetDose(PatientSeries patientSeries) {
    TargetDose targetDose = newTargetDose(patientSeries);
    targetDose.setTargetDoseStatus(TargetDoseStatus.NOT_SATISFIED);
    return targetDose;
  }

  private static TargetDose newTargetDose(PatientSeries patientSeries) {
    SeriesDose seriesDose = new SeriesDose();
    seriesDose.setAntigenSeries(patientSeries.getTrackedAntigenSeries());
    seriesDose.setDoseNumber(String.valueOf(patientSeries.getTargetDoseList().size() + 1));

    TargetDose targetDose = new TargetDose(seriesDose);
    patientSeries.getTargetDoseList().add(targetDose);
    return targetDose;
  }

  /** Runs the step and reports the step it hands control to. */
  private LogicStepType score() throws Exception {
    return new CompletePatientSeries(dataModel).process().getLogicStepType();
  }

  // ---------------------------------------------------------------------
  // Table 8-8 - the two business rules Table 8-7 rests on
  // ---------------------------------------------------------------------

  /**
   * SELECTB-6: "A scorable patient series must be considered a complete patient
   * series if the patient series forecast made from the scorable patient series
   * has a patient series status of 'Complete'." Table 8-7 scores complete
   * patient series, so only a complete patient series can be the one that "has
   * the most valid doses" - a Not Complete series with more valid doses than any
   * complete series must not take the +1 away from the complete series that
   * genuinely has the most.
   */
  @Test
  public void selectbSixOnlyACompletePatientSeriesCompetesForTheMostValidDoses() throws Exception {
    notCompleteSeries("HepB in process", 5);
    PatientSeries complete = completeSeries("HepB complete", 2);

    score();

    assertEquals("the complete series has the most valid doses of any complete patient series", 1,
        complete.getScorePatientSeries());
  }

  /**
   * SELECTB-6 decides not only who wins but who is scored at all. Table 8-7 asks
   * "How Many Points Are Awarded to a Scorable Patient Series That Is a Complete
   * Patient Series?", and 8.3's Table 8-5 Rule 1 - the only route into this step
   * - says "All complete patient series in the series group should be scored.
   * Apply the complete patient series scoring business rules to these scorable
   * patient series <b>only</b>." A series that is not a complete patient series
   * is therefore outside the table's scope entirely, and must come out of 8.4
   * with the score Chapter 7 and 8.1 left it holding.
   *
   * <p>
   * This is the scope question {@link ClassifyScorablePatientSeriesTest}
   * deliberately left to this unit rather than forcing into a test of its own.
   * The test asserts only the half the specification settles - that a table
   * about complete patient series does not penalise a series that is not one -
   * and takes no position on whether such series should later be dropped from
   * consideration by some other means. The implementation's scoring loop opens
   * with an explicit {@code descPatientScoreSeries()} for every non-complete
   * series it sees.
   */
  @Test
  public void theStepScoresOnlyTheCompletePatientSeriesInTheGroup() throws Exception {
    PatientSeries inProcess = notCompleteSeries("HepB in process", 1);
    completeSeries("HepB complete", 2);
    completeSeries("HepB also complete", 1);

    score();

    assertEquals("Table 8-7 scores complete patient series only", 0, inProcess.getScorePatientSeries());
  }

  /**
   * SELECTB-19 rests on the valid dose count, which SELECTB-21 defines as "the
   * count of the target doses included in the scorable patient series with a
   * target dose status of 'Satisfied'". A series holding one satisfied target
   * dose and three not-satisfied ones has a valid dose count of one, not four,
   * so it loses to a series with two satisfied target doses.
   */
  @Test
  public void selectbNineteenTheValidDoseCountCountsOnlyTargetDosesWithStatusSatisfied() throws Exception {
    PatientSeries oneValidDose = completeSeries("HepB one valid dose", 1);
    notSatisfiedTargetDose(oneValidDose);
    notSatisfiedTargetDose(oneValidDose);
    notSatisfiedTargetDose(oneValidDose);
    PatientSeries twoValidDoses = completeSeries("HepB two valid doses", 2);

    score();

    assertEquals("three not-satisfied target doses are not valid doses", -1, oneValidDose.getScorePatientSeries());
    assertEquals(1, twoValidDoses.getScorePatientSeries());
  }

  /**
   * SELECTB-19 defines "has the most valid doses" tie-inclusively - a series has
   * the most if its valid dose count is greater than <i>or equal to</i> every
   * other scorable patient series' count in the series group - so two series
   * with the same count both have the most, and Table 8-7 must treat them
   * identically. This test asserts only that symmetry, independently of which of
   * the table's three columns applies; the column value itself is asserted
   * separately below.
   */
  @Test
  public void selectbNineteenTheMostValidDosesIsTieInclusiveSoNeitherTiedSeriesIsALoneWinner() throws Exception {
    PatientSeries first = completeSeries("HepB first", 2);
    PatientSeries second = completeSeries("HepB second", 2);

    score();

    assertEquals("two series with equal valid dose counts both have the most, so both score the same",
        first.getScorePatientSeries(), second.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Table 8-7 - one test per outcome column, plus each column's boundary
  // ---------------------------------------------------------------------

  /**
   * Table 8-7, first outcome column: the condition "has the most valid doses" is
   * true for this series alone, so it is awarded +1.
   */
  @Test
  public void tableEightSevenTrueForThisSeriesAloneAwardsPlusOne() throws Exception {
    completeSeries("HepB one valid dose", 1);
    PatientSeries winner = completeSeries("HepB two valid doses", 2);

    score();

    assertEquals(1, winner.getScorePatientSeries());
  }

  /**
   * Table 8-7, third outcome column: the condition is not true for this series,
   * so it is awarded -1.
   */
  @Test
  public void tableEightSevenNotTrueForThisSeriesAwardsMinusOne() throws Exception {
    PatientSeries loser = completeSeries("HepB one valid dose", 1);
    completeSeries("HepB two valid doses", 2);

    score();

    assertEquals(-1, loser.getScorePatientSeries());
  }

  /**
   * Table 8-7, second outcome column: the condition is true for two or more
   * series, so each of them is awarded 0 - neither the +1 of a lone winner nor
   * the -1 of a series that does not have the most.
   *
   * <p>
   * Note that "awarded 0" is a real award, not the absence of one: the score is
   * a running total that other conditions and other invocations also write to
   * (see {@link #theScoreIsARunningTotalThatTheStepIncrementsOrDecrementsRatherThanSets}),
   * so a series left untouched by the scoring loop only coincides with a series
   * correctly awarded 0 when the score happened to start at 0.
   */
  @Test
  public void tableEightSevenTrueForTwoOrMoreSeriesAwardsZeroToEachTiedSeries() throws Exception {
    PatientSeries first = completeSeries("HepB first", 2);
    PatientSeries second = completeSeries("HepB second", 2);

    score();

    assertEquals("a tie for the most valid doses awards 0, not +1", 0, first.getScorePatientSeries());
    assertEquals("a tie for the most valid doses awards 0, not +1", 0, second.getScorePatientSeries());
  }

  /**
   * Table 8-7's tie column applies to every series that is tied for the most,
   * and does not change what happens to the rest: a group of two series tied at
   * two valid doses and one series with one valid dose scores 0, 0 and -1.
   */
  @Test
  public void tableEightSevenTheTieOutcomeAppliesToEveryTiedSeriesAndTheOthersStillLose() throws Exception {
    PatientSeries first = completeSeries("HepB first", 2);
    PatientSeries second = completeSeries("HepB second", 2);
    PatientSeries behind = completeSeries("HepB behind", 1);

    score();

    assertEquals(0, first.getScorePatientSeries());
    assertEquals(0, second.getScorePatientSeries());
    assertEquals(-1, behind.getScorePatientSeries());
  }

  /**
   * Table 8-7 awards its -1 on a property of the series - the condition is not
   * true for it - so a series with fewer valid doses than the winner is awarded
   * -1 whether it sits before or after the winner in the list.
   * {@link #tableEightSevenNotTrueForThisSeriesAwardsMinusOne} is the same group
   * in the other order and is the control for this test.
   */
  @Test
  public void tableEightSevenTheMinusOneIsAwardedWhateverTheBelowMaxSeriesPositionInTheList() throws Exception {
    PatientSeries winner = completeSeries("HepB two valid doses", 2);
    PatientSeries loser = completeSeries("HepB one valid dose", 1);

    score();

    assertEquals(1, winner.getScorePatientSeries());
    assertEquals("the -1 is awarded on the series' valid dose count, not on its list position", -1,
        loser.getScorePatientSeries());
  }

  /**
   * Table 8-7's boundary at zero. 8.3's Table 8-5 Rule 1 has "-" in its other
   * two cells, so a series group of two complete patient series reaches 8.4 even
   * when the number of valid doses is 0 for all of them - see
   * {@code ClassifyScorablePatientSeriesTest#ruleOneFiresWhateverTheOtherTwoConditionsAnswer},
   * which is green. SELECTB-19 is a comparison, not a threshold, so at 0 = 0
   * both series have the most valid doses and Table 8-7's tie column awards each
   * of them 0.
   */
  @Test
  public void tableEightSevenACompleteSeriesGroupWhereNoSeriesHasAValidDoseIsATie() throws Exception {
    PatientSeries first = completeSeries("HepB first", 0);
    PatientSeries second = completeSeries("HepB second", 0);

    score();

    assertEquals("0 = 0 is a tie for the most valid doses", 0, first.getScorePatientSeries());
    assertEquals("0 = 0 is a tie for the most valid doses", 0, second.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // Purpose and Entry Conditions - the scope 8.4 scores over
  // ---------------------------------------------------------------------

  /**
   * Every row of 8.4 is about a <b>scorable</b> patient series: Table 8-7's
   * title asks how many points are awarded "to a Scorable Patient Series That Is
   * a Complete Patient Series", SELECTB-6 and SELECTB-19 are both phrased over
   * scorable patient series, and 8.3's Table 8-5 Rule 1 - the only route into
   * this step - hands 8.4 the complete patient series it counted. The scorable
   * patient series are what 8.1 produced: SELECTSCORE-2 keeps only the
   * highest-priority Risk series of a series group, so a priority "B" Risk
   * series alongside a priority "A" one is not a scorable patient series and
   * must not compete for the most valid doses.
   *
   * <p>
   * The two lists genuinely differ. 4.5 {@code SelectBestPatientSeries} builds
   * {@code selectedPatientSeriesList} as every relevant patient series of the
   * antigen being processed, before any pre-filtering; 8.1
   * {@code PreFilterPatientSeries} then narrows that to
   * {@code scorablePatientSeriesList}, which 8.2, 8.3 and 8.4 alike now read,
   * so a series 8.1 deliberately dropped from consideration does not set the
   * maximum valid dose count that every scorable series is measured against.
   */
  @Test
  public void theStepScoresTheScorablePatientSeriesEightOneProducedNotEveryRelevantSeries() throws Exception {
    PatientSeries scorable = series("HepB risk priority A", PatientSeriesStatus.COMPLETE, INCREASED_RISK_GROUP,
        SeriesType.RISK, "A", 2);
    PatientSeries dropped = series("HepB risk priority B", PatientSeriesStatus.COMPLETE, INCREASED_RISK_GROUP,
        SeriesType.RISK, "B", 5);
    // What 8.1 leaves behind: the priority B risk series is not scorable.
    dataModel.getScorablePatientSeriesList().remove(dropped);

    score();

    assertEquals("only the scorable patient series compete for the most valid doses", 1,
        scorable.getScorePatientSeries());
  }

  /**
   * Chapter 8's overview: "Process steps 8.1 through 8.7 are repeated <b>for
   * each series group</b> to identify one prioritized patient series per series
   * group", and 8.4's own Entry Conditions put it inside that loop - it runs
   * when 8.3 found two or more complete patient series "in the series group".
   * SELECTB-19 is scoped the same way: a series has the most valid doses when
   * its count is greater than or equal to every other scorable patient series'
   * count <i>in the series group</i>. So a run of 8.4 for the Standard group
   * must find its two-valid-dose series the winner, whatever the Increased Risk
   * group happens to hold.
   *
   * <p>
   * Unlike 8.1, 8.2 and 8.3, 8.4 reads a list 4.5 has already narrowed to one
   * antigen, so the antigen half of the cross-cutting entry cited above does not
   * arise here. The series group half does, and it bites harder in a scoring
   * step than in a classifying one: 8.4's condition is a comparison, so a series
   * belonging to a different series group does not merely join the group being
   * scored - it can win the competition outright and push every series actually
   * under selection to -1.
   */
  @Test
  public void theStepScoresThePatientSeriesOfOneSeriesGroup() throws Exception {
    PatientSeries standardLoser = completeSeries("HepB standard one valid dose", 1);
    PatientSeries standardWinner = completeSeries("HepB standard two valid doses", 2);
    series("HepB increased risk", PatientSeriesStatus.COMPLETE, INCREASED_RISK_GROUP, SeriesType.RISK, "A", 5);

    // SelectNextSeriesGroup hands 8.1 (and, through it, 8.4) one series group at
    // a time; simulate the Standard group's own pass.
    dataModel.setScorablePatientSeriesList(new ArrayList<PatientSeries>(Arrays.asList(standardLoser, standardWinner)));

    score();

    assertEquals("one run of 8.4 scores one series group, whose winner has two valid doses", 1,
        standardWinner.getScorePatientSeries());
  }

  // ---------------------------------------------------------------------
  // State Changes
  // ---------------------------------------------------------------------

  /**
   * 08-04's State Changes: each scorable patient series in the group has
   * {@code incPatientScoreSeries()}/{@code descPatientScoreSeries()} called on
   * it, "a running integer score field", consumed later by 8.7. So Table 8-7's
   * +1 and -1 are applied to whatever score the series already carries rather
   * than replacing it.
   *
   * <p>
   * Worth pinning explicitly because the score is never reset between
   * selections - see the 2026-09-02 "Patient series scores accumulate across a
   * whole assessment" entry in
   * {@code cdsi-reference/step-tests/cross-cutting-notes.md}. Whether that
   * accumulation is correct is not 8.4's question, but it is the reason Table
   * 8-7's tie column ("0") cannot be implemented by leaving a series untouched.
   */
  @Test
  public void theScoreIsARunningTotalThatTheStepIncrementsOrDecrementsRatherThanSets() throws Exception {
    PatientSeries loser = completeSeries("HepB one valid dose", 1);
    PatientSeries winner = completeSeries("HepB two valid doses", 2);
    loser.setScorePatientSeriesScore(5);
    winner.setScorePatientSeriesScore(5);

    score();

    assertEquals("+1 is added to the running score, not assigned to it", 6, winner.getScorePatientSeries());
    assertEquals("-1 is subtracted from the running score, not assigned to it", 4, loser.getScorePatientSeries());
  }

  /**
   * 08-04's State Changes names the score field and nothing else. Whichever
   * column of Table 8-7 applies, each patient series' status, its target doses
   * and their statuses, and the list itself must come back exactly as 4.5 and
   * Chapter 7 left them, and neither the prioritized nor the scorable patient
   * series list may gain an entry - selecting a prioritized series belongs to
   * 8.7, which {@code next()} constructs but does not run.
   */
  @Test
  public void theStepChangesNoPatientSeriesStateOtherThanTheScore() throws Exception {
    PatientSeries loser = completeSeries("HepB one valid dose", 1);
    TargetDose notSatisfied = notSatisfiedTargetDose(loser);
    PatientSeries winner = completeSeries("HepB two valid doses", 2);
    int scorableSizeBeforeScoring = dataModel.getScorablePatientSeriesList().size();

    score();

    assertEquals("the selected patient series list is not re-filtered here", 2,
        dataModel.getSelectedPatientSeriesList().size());
    assertEquals(PatientSeriesStatus.COMPLETE, loser.getPatientSeriesStatus());
    assertEquals(PatientSeriesStatus.COMPLETE, winner.getPatientSeriesStatus());
    assertEquals(2, loser.getTargetDoseList().size());
    assertEquals(2, winner.getTargetDoseList().size());
    assertEquals(TargetDoseStatus.NOT_SATISFIED, notSatisfied.getTargetDoseStatus());
    assertEquals(TargetDoseStatus.SATISFIED, winner.getTargetDoseList().get(0).getTargetDoseStatus());
    assertTrue("8.4 prioritizes no patient series", dataModel.getPrioritizedPatientSeriesList().isEmpty());
    assertEquals("8.4 does not re-derive the scorable patient series list", scorableSizeBeforeScoring,
        dataModel.getScorablePatientSeriesList().size());
  }

  // ---------------------------------------------------------------------
  // Next Steps
  // ---------------------------------------------------------------------

  /**
   * 08-04's Next Steps: unconditional to 8.7 Select Prioritized Patient Series,
   * per {@code transitions.yaml}. "Unconditional" means whatever the scoring
   * found - a lone winner, a tie, or an empty group with nothing to score at
   * all.
   */
  @Test
  public void theStepTransitionsUnconditionallyToSelectPrioritizedPatientSeries() throws Exception {
    assertEquals("nothing to score still goes to 8.7", LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, score());

    setUp();
    completeSeries("HepB one valid dose", 1);
    completeSeries("HepB two valid doses", 2);
    assertEquals("a lone winner goes to 8.7", LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, score());

    setUp();
    completeSeries("HepB first", 2);
    completeSeries("HepB second", 2);
    assertEquals("a tie goes to 8.7", LogicStepType.SELECT_PRIORITIZED_PATIENT_SERIES, score());
  }
}
