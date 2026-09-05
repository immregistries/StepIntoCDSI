package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
 * Section 8.3 "Classify Scorable Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, pages 88-89; Table 8-5 "Which Scorable Patient Series
 * Should be Scored?" and Table 8-6 business rules SELECTB-6, SELECTB-16 and
 * SELECTB-21) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-03-classify-scorable-patient-series/index.md}.
 *
 * <p>
 * 8.3 runs only when 8.2 could not shortcut the selection, and its whole job is
 * to decide <i>which kind</i> of scoring applies to the series group: a
 * competition between two or more already-complete series (8.4), between two or
 * more partially-done series (8.5), or a group where nothing has a valid dose at
 * all (8.6). Per the step package's State Changes it is "purely a router" - no
 * patient series data is mutated - so the observable behaviour of every rule is
 * a single value, the next {@code LogicStepType}. The tests below are organised
 * one per Table 8-6 business rule (with the negative direction where the rule
 * has one), then one per Table 8-5 rule column plus each column's boundary, then
 * the State Changes claim, then the two scoping sentences the section's Purpose
 * and every one of Table 8-5's conditions state ("...in the series group").
 *
 * <p>
 * <b>Isolation.</b> {@link ClassifyScorablePatientSeries} reads exactly one
 * thing from the {@code DataModel} - {@code getScorablePatientSeriesList()},
 * which 8.1 builds - plus, per patient series, its {@code patientSeriesStatus}
 * and its {@code targetDoseList}. (Its logging additionally dereferences the
 * tracked {@code AntigenSeries} and, for a Satisfied target dose, that dose's
 * tracked {@code SeriesDose}, so the fixture supplies both.) Every test
 * hand-builds that shape and drives the public {@code process()} directly; no
 * Supporting Data release, no loader and no upstream step is involved.
 * {@code process()} ends in {@code next()}, which constructs the next step but
 * never runs it, so the returned object is used only for its
 * {@code LogicStepType}.
 *
 * <p>
 * <b>Why some tests assert a "not this branch" rather than a branch.</b> Table
 * 8-5 has three rule columns and no default column, and its three conditions do
 * not partition every input: a series group holding, say, one complete series
 * and one in-process series matches Rule 1 (needs 2+ complete), Rule 2 (needs
 * 2+ in-process AND zero complete) and Rule 3 (needs zero valid doses
 * everywhere) alike - none of them. The specification says nothing about what
 * should happen then, so for those inputs these tests assert only the half the
 * specification does settle: that the scoring rules whose own condition is
 * demonstrably false are not the ones applied. See this unit's
 * {@code status.yaml} notes, and 08-03's own "Review Findings".
 *
 * <p>
 * <b>Deliberately not covered.</b> The entry condition ("runs only when 8.2 did
 * not find a single obvious prioritized series") is a property of 8.2's
 * transitions, asserted from 8.2's side in
 * {@link IdentifyOnePrioritizedPatientSeriesTest}, and 8.3's class has no entry
 * guard of its own to test. Table 8-5's outcome text also says the series a
 * column does not select "are not scored and are dropped from consideration";
 * 08-03's State Changes reads that as a downstream responsibility rather than a
 * mutation 8.3 performs, and 8.4 `CompletePatientSeries` does in fact implement
 * it by scoring non-complete series down, so it is recorded as a scope question
 * in this unit's notes rather than forced into a test here.
 */
public class ClassifyScorablePatientSeriesTest {

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
  // Fixture builders - the minimal shape 8.3 actually reads.
  // ---------------------------------------------------------------------

  /** A scorable patient series of a named antigen and series group. */
  private PatientSeries scorableSeries(String seriesName, Antigen targetDisease, String seriesGroupName,
      PatientSeriesStatus status) {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroupName(seriesGroupName);
    selectPatientSeries.setSeriesGroup(seriesGroupName);

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setSeriesType(SeriesType.STANDARD);
    antigenSeries.setTargetDisease(targetDisease);
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(status);
    patientSeries.setTargetDoseList(new ArrayList<TargetDose>());
    scorablePatientSeriesList.add(patientSeries);
    return patientSeries;
  }

  /** A scorable HepB series of the Standard series group. */
  private PatientSeries scorableSeries(String seriesName, PatientSeriesStatus status) {
    return scorableSeries(seriesName, hepB, STANDARD_GROUP, status);
  }

  /**
   * Gives the patient series one target dose in the state SELECTB-21 counts as a
   * valid dose - "a target dose status of 'Satisfied'".
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

  /**
   * Runs the step and reports the branch it chose. 8.3's only observable output
   * is which of 8.4/8.5/8.6 it hands control to.
   */
  private LogicStepType classify() throws Exception {
    return new ClassifyScorablePatientSeries(dataModel).process().getLogicStepType();
  }

  // ---------------------------------------------------------------------
  // Table 8-6 - the three business rules Table 8-5's counts rest on
  // ---------------------------------------------------------------------

  /**
   * SELECTB-6: "A scorable patient series must be considered a complete patient
   * series if the patient series forecast made from the scorable patient series
   * has a patient series status of 'Complete'." Two such series make Table 8-5's
   * first condition Yes, so the complete patient series scoring rules (8.4)
   * apply.
   */
  @Test
  public void selectbSixACompletePatientSeriesIsOneWhoseForecastStatusIsComplete() throws Exception {
    scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE);
    scorableSeries("HepB also complete", PatientSeriesStatus.COMPLETE);

    assertEquals(LogicStepType.COMPLETE_PATIENT_SERIES, classify());
  }

  /**
   * SELECTB-6 from the negative side: the rule keys on the patient series
   * status alone, not on how much of the series the patient has actually
   * received. Three Not Complete series, each with a satisfied target dose,
   * therefore hold zero complete patient series - Table 8-5's first condition is
   * No and its second is Yes, so this is in-process scoring (8.5), not complete
   * scoring (8.4).
   */
  @Test
  public void selectbSixASeriesThatIsNotCompleteIsNotCountedAsACompletePatientSeries() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB third", PatientSeriesStatus.NOT_COMPLETE));

    assertEquals(LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }

  /**
   * SELECTB-16's first bullet: an in-process patient series "includes at least
   * one target dose with a target dose status of 'Satisfied'". A group of three
   * Not Complete series where only one has a satisfied target dose holds one
   * in-process patient series, not three, so Table 8-5's "2 or more in-process"
   * condition is No and the in-process scoring rules must not be the ones
   * applied.
   */
  @Test
  public void selectbSixteenAnInProcessPatientSeriesNeedsAtLeastOneSatisfiedTargetDose() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB started", PatientSeriesStatus.NOT_COMPLETE));
    notSatisfiedTargetDose(scorableSeries("HepB untouched", PatientSeriesStatus.NOT_COMPLETE));
    notSatisfiedTargetDose(scorableSeries("HepB also untouched", PatientSeriesStatus.NOT_COMPLETE));

    assertNotEquals("Only one series has a satisfied target dose, so there are not 2 or more in-process series",
        LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }

  /**
   * SELECTB-16's second bullet: an in-process patient series' forecast must also
   * have "a patient series status of 'Not Complete'". Both bullets must hold -
   * the rule says "if <i>all</i> the following are true". Two series that have
   * satisfied target doses but whose forecast aged the patient out (7.4's Table
   * 7-10 sets {@code AGED_OUT}, and 8.1 filters only contraindicated series out
   * of the scorable list, so an aged-out series is genuinely scorable) are
   * therefore not in-process patient series: the group holds zero of them,
   * Table 8-5's second condition is No, and the in-process scoring rules must
   * not be the ones applied.
   *
   * <p>
   * The implementation's second condition counts scorable series with at least
   * one Satisfied target dose and reads no patient series status other than
   * through the separate "no complete patient series" half of the same
   * condition, so SELECTB-16's second bullet is never checked: the count comes
   * back 2, the complete count is 0, and the group is routed to in-process
   * scoring.
   */
  @Test
  public void selectbSixteenAnInProcessPatientSeriesForecastStatusMustBeNotComplete() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB aged out", PatientSeriesStatus.AGED_OUT));
    satisfiedTargetDose(scorableSeries("HepB also aged out", PatientSeriesStatus.AGED_OUT));

    assertNotEquals("Neither series has a 'Not Complete' forecast, so neither is an in-process patient series",
        LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }

  /**
   * SELECTB-16 makes the <i>patient series</i> in-process ("a scorable patient
   * series must be considered an in-process patient series if..."), and Table
   * 8-5's second condition then asks how many in-process <i>patient series</i>
   * are in the series group - so a series with three satisfied target doses is
   * one in-process patient series, not three. With two doseless siblings the
   * group holds exactly one, and in-process scoring must not be the branch
   * taken.
   *
   * <p>
   * Worth pinning here because 8.2's counterpart count gets this wrong in the
   * other direction: {@code IdentifyOnePrioritizedPatientSeries} increments once
   * per satisfied target dose, while 8.3's
   * {@code calculateCountOfPatientSeriesWithValidDoses} breaks out of the inner
   * loop after the first one and so counts per series.
   */
  @Test
  public void selectbSixteenOneSeriesWithSeveralSatisfiedTargetDosesIsOneInProcessPatientSeries() throws Exception {
    PatientSeries inProcess = scorableSeries("HepB in process", PatientSeriesStatus.NOT_COMPLETE);
    satisfiedTargetDose(inProcess);
    satisfiedTargetDose(inProcess);
    satisfiedTargetDose(inProcess);
    notSatisfiedTargetDose(scorableSeries("HepB untouched", PatientSeriesStatus.NOT_COMPLETE));
    notSatisfiedTargetDose(scorableSeries("HepB also untouched", PatientSeriesStatus.NOT_COMPLETE));

    assertNotEquals("One series with three satisfied target doses is one in-process patient series",
        LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }

  /**
   * SELECTB-21: "The number of valid doses for a scorable patient series must be
   * calculated as the count of the target doses included in the scorable patient
   * series with a target dose status of 'Satisfied'." A group whose every target
   * dose is Not Satisfied has a valid dose count of 0 for every series, which is
   * Table 8-5's third condition, so the no-valid-doses scoring rules (8.6)
   * apply.
   */
  @Test
  public void selectbTwentyOneTheNumberOfValidDosesCountsTargetDosesWithStatusSatisfied() throws Exception {
    PatientSeries first = scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE);
    notSatisfiedTargetDose(first);
    notSatisfiedTargetDose(first);
    notSatisfiedTargetDose(scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE));

    assertEquals(LogicStepType.NO_VALID_DOSES, classify());
  }

  /**
   * SELECTB-21's boundary: a scorable patient series that includes no target
   * doses at all has a count of Satisfied target doses of 0, exactly like one
   * whose target doses are all Not Satisfied. This is the ordinary shape of a
   * series for a patient with no relevant immunization history, and it is the
   * case Table 8-5's third condition exists to catch.
   */
  @Test
  public void selectbTwentyOneASeriesWithNoTargetDosesHasZeroValidDoses() throws Exception {
    scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE);
    scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE);

    assertEquals(LogicStepType.NO_VALID_DOSES, classify());
  }

  // ---------------------------------------------------------------------
  // Table 8-5 - one test per rule column, plus each column's boundary
  // ---------------------------------------------------------------------

  /**
   * Table 8-5, Rule 1: "Are there 2 or more complete patient series in the series
   * group?" Yes yields "All complete patient series in the series group should be
   * scored. Apply the complete patient series scoring business rules to these
   * scorable patient series only" - which per 08-03's Next Steps is the branch to
   * 8.4.
   */
  @Test
  public void ruleOneTwoOrMoreCompletePatientSeriesUseTheCompleteScoringRules() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB also complete", PatientSeriesStatus.COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB in process", PatientSeriesStatus.NOT_COMPLETE));

    assertEquals(LogicStepType.COMPLETE_PATIENT_SERIES, classify());
  }

  /**
   * Table 8-5, Rule 1's other two cells are "-": once there are 2 or more
   * complete patient series the column fires whatever the in-process and
   * valid-dose counts answer. Two complete series that carry no target doses at
   * all make the third condition ("is the number of valid doses = 0 for all
   * scorable patient series?") true as well, and Rule 1 must still win over Rule
   * 3.
   */
  @Test
  public void ruleOneFiresWhateverTheOtherTwoConditionsAnswer() throws Exception {
    scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE);
    scorableSeries("HepB also complete", PatientSeriesStatus.COMPLETE);

    assertEquals("Rule 1's remaining cells are '-', so it fires even though no series has a valid dose",
        LogicStepType.COMPLETE_PATIENT_SERIES, classify());
  }

  /**
   * Table 8-5, Rule 2: no 2-or-more-complete group, but "2 or more in-process
   * patient series and no complete patient series in the series group" yields
   * "All in-process patient series in the series group should be scored" - the
   * branch to 8.5.
   */
  @Test
  public void ruleTwoTwoOrMoreInProcessSeriesAndNoCompleteSeriesUseTheInProcessScoringRules() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE));

    assertEquals(LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }

  /**
   * Rule 2's second condition is a conjunction - "2 or more in-process patient
   * series <b>and no</b> complete patient series in the series group". A single
   * complete series alongside two in-process ones fails the second half, so
   * in-process scoring is not the branch, even though the in-process half is
   * satisfied.
   */
  @Test
  public void ruleTwoDoesNotFireWhenTheSeriesGroupHasACompletePatientSeries() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE));

    assertNotEquals("The group holds a complete patient series, so Rule 2's conjunction fails",
        LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }

  /**
   * Table 8-5, Rule 3: no complete series, fewer than two in-process series, and
   * "the number of valid doses = 0 for all scorable patient series in the series
   * group" yields "Apply the no valid doses scoring business rules to all
   * scorable patient series in the series group" - the branch to 8.6.
   */
  @Test
  public void ruleThreeZeroValidDosesForEverySeriesUsesTheNoValidDosesScoringRules() throws Exception {
    notSatisfiedTargetDose(scorableSeries("HepB first", PatientSeriesStatus.NOT_COMPLETE));
    notSatisfiedTargetDose(scorableSeries("HepB second", PatientSeriesStatus.NOT_COMPLETE));
    scorableSeries("HepB third", PatientSeriesStatus.NOT_COMPLETE);

    assertEquals(LogicStepType.NO_VALID_DOSES, classify());
  }

  /**
   * Rule 3's condition is "= 0 for <b>all</b> scorable patient series in the
   * series group", so a group in which any series has a valid dose does not match
   * it and must not have the no-valid-doses scoring business rules applied to it.
   * One complete series and one in-process series, both with a satisfied target
   * dose, is such a group.
   *
   * <p>
   * This is the gap 08-03's "Review Findings" records: the group matches none of
   * Table 8-5's three columns (Rule 1 needs 2 or more complete, Rule 2 needs 2 or
   * more in-process and zero complete, Rule 3 needs zero valid doses), and
   * {@code process()} sets {@code NO_VALID_DOSES} as a pre-evaluation default
   * before {@code evaluateLogicTables()} runs, so a group with two valid doses in
   * it is scored by the rules for a group with none. What the specification
   * <i>should</i> do for such a group is genuinely unsettled - so this test
   * asserts only the branch the specification does rule out.
   */
  @Test
  public void ruleThreeDoesNotFireWhenAScorablePatientSeriesHasAValidDose() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB in process", PatientSeriesStatus.NOT_COMPLETE));

    assertNotEquals("Two series carry a valid dose, so the number of valid doses is not 0 for all of them",
        LogicStepType.NO_VALID_DOSES, classify());
  }

  // ---------------------------------------------------------------------
  // State Changes
  // ---------------------------------------------------------------------

  /**
   * 08-03's State Changes: "No data is directly mutated here beyond selecting
   * which scoring path runs next - this step is purely a router." Whichever
   * column fires, the scorable patient series list, each series' status, each
   * target dose's status and each series' accumulated score must all come back
   * exactly as 8.1 and Chapter 7 left them, and neither the prioritized nor the
   * selected patient series list may gain an entry - those belong to 8.7 and 8.8.
   */
  @Test
  public void theStepChangesNoPatientSeriesStateBeyondChoosingTheScoringPath() throws Exception {
    PatientSeries complete = scorableSeries("HepB complete", PatientSeriesStatus.COMPLETE);
    TargetDose satisfied = satisfiedTargetDose(complete);
    PatientSeries inProcess = scorableSeries("HepB in process", PatientSeriesStatus.NOT_COMPLETE);
    TargetDose notSatisfied = notSatisfiedTargetDose(inProcess);
    scorableSeries("HepB also complete", PatientSeriesStatus.COMPLETE);

    assertEquals(LogicStepType.COMPLETE_PATIENT_SERIES, classify());

    assertEquals("the scorable patient series list is not re-filtered here", 3,
        dataModel.getScorablePatientSeriesList().size());
    assertEquals(PatientSeriesStatus.COMPLETE, complete.getPatientSeriesStatus());
    assertEquals(PatientSeriesStatus.NOT_COMPLETE, inProcess.getPatientSeriesStatus());
    assertEquals(TargetDoseStatus.SATISFIED, satisfied.getTargetDoseStatus());
    assertEquals(TargetDoseStatus.NOT_SATISFIED, notSatisfied.getTargetDoseStatus());
    assertEquals("scoring belongs to 8.4-8.6, not to 8.3", 0, complete.getScorePatientSeries());
    assertEquals("scoring belongs to 8.4-8.6, not to 8.3", 0, inProcess.getScorePatientSeries());
    assertTrue("8.3 selects no patient series", dataModel.getSelectedPatientSeriesList().isEmpty());
    assertTrue("8.3 prioritizes no patient series", dataModel.getPrioritizedPatientSeriesList().isEmpty());
  }

  // ---------------------------------------------------------------------
  // Purpose - the scope 8.3 classifies over
  // ---------------------------------------------------------------------

  /**
   * Purpose: "Classify scorable patient series is an attempt to reduce the total
   * number of patient series <b>within a Series Group</b>...", and all three of
   * Table 8-5's conditions end "...in the series group?". Chapter 8's overview
   * says the same - "Process steps 8.1 through 8.7 are repeated for each series
   * group". So a run of 8.3 for the Standard group must see two in-process series
   * and no complete series, and route to in-process scoring (8.5), whatever the
   * Increased Risk group happens to hold.
   *
   * <p>
   * Nothing in {@code cdsi-engine} reads
   * {@code SelectPatientSeries.getSeriesGroup()}, and none of 8.3's three
   * conditions narrows {@code scorablePatientSeriesList} at all, so both groups
   * are classified together: the complete count is 2, Rule 1 fires, and the
   * Standard group's two in-process series are handed to the complete patient
   * series scoring rules instead. See the 2026-09-05 "Chapter 8 has no series
   * group" entry in {@code cdsi-reference/step-tests/cross-cutting-notes.md}.
   */
  @Test
  public void theStepClassifiesOnlyThePatientSeriesOfOneSeriesGroup() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB standard first", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB standard second", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE));
    scorableSeries("HepB risk complete", hepB, INCREASED_RISK_GROUP, PatientSeriesStatus.COMPLETE);
    scorableSeries("HepB risk also complete", hepB, INCREASED_RISK_GROUP, PatientSeriesStatus.COMPLETE);

    assertEquals("One run of 8.3 classifies one series group, whose two series are both in-process",
        LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }

  /**
   * Chapter 8 runs inside 4.5's per-antigen loop (Figure 4-7), so every count
   * Table 8-5 takes is over one antigen's series. On the HepB pass a Measles
   * series left in {@code scorablePatientSeriesList} must not be classified: HepB
   * alone holds two in-process series and no complete series, so the in-process
   * scoring rules apply.
   *
   * <p>
   * Unlike 8.2, whose class filters by {@code dataModel.getAntigen()} in three of
   * its four conditions, {@code ClassifyScorablePatientSeries} filters nowhere -
   * none of its three conditions and neither of its two counting helpers reads
   * the antigen at all - so Measles' two complete series make the complete count
   * 2 and the HepB group is routed to complete scoring. Because 8.3's outcome is
   * a control-flow branch rather than a count, the consequence is that one
   * antigen's series decide which chapter of scoring rules another antigen's
   * series are judged by. Same cross-cutting entry as the test above.
   */
  @Test
  public void theStepClassifiesOnlyThePatientSeriesOfTheAntigenBeingProcessed() throws Exception {
    Antigen measles = dataModel.getOrCreateAntigen("Measles");
    satisfiedTargetDose(scorableSeries("HepB first", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB second", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE));
    scorableSeries("Measles complete", measles, STANDARD_GROUP, PatientSeriesStatus.COMPLETE);
    scorableSeries("Measles also complete", measles, STANDARD_GROUP, PatientSeriesStatus.COMPLETE);

    assertEquals("8.3 runs inside 4.5's per-antigen loop, so only HepB's series are classified",
        LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }

  /**
   * A guard on the fixture itself, and on the claim the two scoping tests above
   * rest on: the three names used there are the whole of what the step reads, so
   * this test states the counter-case explicitly. With every scorable series
   * belonging to one antigen and one series group - the only configuration in
   * which the implementation's chapter-wide counts and the specification's
   * per-series-group counts agree - Table 8-5's Rule 2 is reached and the two
   * scoping tests' expected answer is shown to be the one the step produces when
   * scope is not in question.
   */
  @Test
  public void oneAntigenAndOneSeriesGroupIsClassifiedTheSameWayByBothReadings() throws Exception {
    satisfiedTargetDose(scorableSeries("HepB first", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE));
    satisfiedTargetDose(scorableSeries("HepB second", hepB, STANDARD_GROUP, PatientSeriesStatus.NOT_COMPLETE));

    assertEquals(Arrays.asList("HepB first", "HepB second"),
        Arrays.asList(scorablePatientSeriesList.get(0).getTrackedAntigenSeries().getSeriesName(),
            scorablePatientSeriesList.get(1).getTrackedAntigenSeries().getSeriesName()));
    assertEquals(LogicStepType.IN_PROCESS_PATIENT_SERIES, classify());
  }
}
