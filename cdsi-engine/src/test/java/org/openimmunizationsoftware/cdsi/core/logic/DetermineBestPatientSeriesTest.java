package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;
import org.openimmunizationsoftware.cdsi.core.domain.AntigenSeries;
import org.openimmunizationsoftware.cdsi.core.domain.Patient;
import org.openimmunizationsoftware.cdsi.core.domain.PatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SelectPatientSeries;
import org.openimmunizationsoftware.cdsi.core.domain.SeriesType;
import org.openimmunizationsoftware.cdsi.core.domain.TargetDose;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.PatientSeriesStatus;

/**
 * Section 8.8 "Determine Best Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, page 92; Table 8-14) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/08-08-determine-best-patient-series/index.md}.
 *
 * <p>
 * 8.8 is the last step of Chapter 8 and the consumer end of 8.7. It reads
 * {@code dataModel.getPrioritizedPatientSeriesList()} - which 8.7 populated and
 * which 4.5 clears at the top of every antigen pass - and applies Table 8-14 to
 * each entry, appending the ones that survive to
 * {@code dataModel.getBestPatientSeriesList()} for 4.6 and Chapter 9. It has no
 * business rules of its own; the whole section is one five-condition,
 * three-rule decision table plus a default:
 *
 * <pre>
 *                                                           R1    R2    R3
 * Is the prioritized patient series a complete patient series?  Yes   No    No
 * Is there a prioritized patient series that is complete
 *   in an equivalent series group?                           -     No    No
 * Is the series type 'Evaluation Only'?                      -     No    No
 * Is the series type 'Risk'?                                 -     Yes   No
 * Is there a prioritized patient series with type 'Risk'
 *   in an equivalent series group?                           -     -     No
 * Outcome                                                    best  best  best
 * Default: no best patient series for the series group.
 * </pre>
 *
 * <p>
 * <b>Isolation.</b> {@code DetermineBestPatientSeries} builds its
 * {@code LogicTable}s in its constructor and evaluates them in {@code process()},
 * so - unlike 8.5/8.6/8.7 - there is no private method worth invoking
 * reflectively: driving the public {@code process()} over a hand-built
 * {@code DataModel} exercises exactly one table per prioritized patient series
 * and nothing else. No Supporting Data release, no loader and no upstream step is
 * involved, with the single exception of
 * {@link #theReleaseDeclaresTheEquivalentSeriesGroupsTableEightFourteenNeeds()},
 * which reads one bundled XML resource as text to show the data the two
 * "equivalent series group" conditions need is really there. Note that the class
 * captures {@code dataModel.getPatientSeriesStepper().getList()} in a field
 * initializer and iterates the prioritized list in its constructor, so every test
 * builds the whole fixture before constructing the step.
 *
 * <p>
 * <b>On "an equivalent series group".</b> The specification does not define the
 * phrase in Chapter 8, but 4.5 does: "A best patient series will be selected for
 * each Series Group, however, some antigen series define Equivalent Series Groups
 * which allow a single best series to be selected from across multiple Series
 * Groups." It is Supporting Data, not an inference: every {@code <series>} in the
 * bundled 4.65-508 release carries an {@code <equivalentSeriesGroups>} element
 * naming the series group(s) its own group is interchangeable with. In HepA, the
 * Standard group (series group 1) declares "2" and the Increased Risk group
 * (series group 2) declares "1" - they are equivalent to each other - while the
 * single Increased Risk - Pediatric Travel series (series group 3) declares an
 * empty element and is therefore equivalent to nothing. Of the 143 series in the
 * release, 54 declare an equivalent series group and 89 declare none. The tests
 * below use HepA's real group numbers, names and series names throughout.
 *
 * <p>
 * <b>On the series group loop.</b> Nothing in {@code cdsi-engine} loops over
 * series groups, and 8.7 leaves exactly one prioritized patient series behind per
 * antigen pass rather than one per series group - see the 2026-09-05 "Chapter 8
 * has no series group" entry in
 * {@code cdsi-reference/step-tests/cross-cutting-notes.md}. That is 8.7's and
 * 4.5's gap, not 8.8's: 8.8 evaluates whatever the prioritized list holds, one
 * table per entry, and handles several entries correctly when it is given them
 * ({@link #theStepEvaluatesEveryPrioritizedPatientSeriesOnePerSeriesGroup()},
 * green). These tests therefore hand 8.8 the list a correct 8.7 would build,
 * which is the only way to reach Table 8-14's "in an equivalent series group"
 * conditions at all.
 */
public class DetermineBestPatientSeriesTest {

  /**
   * HepA's series groups in the bundled 4.65-508 release, with the equivalence
   * each one's series declare: groups 1 and 2 are equivalent to each other, and
   * group 3 is equivalent to nothing.
   */
  private static final String STANDARD_GROUP = "1";
  private static final String STANDARD_GROUP_NAME = "Standard";
  private static final String INCREASED_RISK_GROUP = "2";
  private static final String INCREASED_RISK_GROUP_NAME = "Increased Risk";
  private static final String NO_EQUIVALENT_GROUP = "3";
  private static final String NO_EQUIVALENT_GROUP_NAME = "Increased Risk - Pediatric Travel";

  private DataModel dataModel;
  private Antigen hepA;
  private Antigen measles;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    dataModel.setPatient(new Patient());

    hepA = dataModel.getOrCreateAntigen("HepA");
    measles = dataModel.getOrCreateAntigen("Measles");

    // The state 4.5 leaves behind at the top of one antigen pass: the antigen
    // under consideration, an empty prioritized list (4.5 clears it every pass)
    // and a best patient series list created once and never cleared again.
    dataModel.setAntigen(hepA);
    dataModel.setPrioritizedPatientSeriesList(new ArrayList<PatientSeries>());
    dataModel.setBestPatientSeriesList(new ArrayList<PatientSeries>());
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the minimal shape 8.8 actually reads.
  // ---------------------------------------------------------------------

  /**
   * A relevant patient series on 5.1's all-antigen patient series stepper, and
   * nowhere else - one that 8.1 excluded, or that lost its series group's 8.7
   * selection, and so is not a prioritized patient series.
   */
  private PatientSeries relevantSeries(String seriesName, Antigen targetDisease, String seriesGroup,
      String seriesGroupName, SeriesType seriesType, PatientSeriesStatus patientSeriesStatus) {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroup(seriesGroup);
    selectPatientSeries.setSeriesGroupName(seriesGroupName);

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setSeriesType(seriesType);
    antigenSeries.setTargetDisease(targetDisease);
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    patientSeries.setPatientSeriesStatus(patientSeriesStatus);
    patientSeries.setTargetDoseList(new ArrayList<TargetDose>());
    dataModel.getPatientSeriesStepper().getList().add(patientSeries);
    return patientSeries;
  }

  /** The same, for HepA - the antigen 4.5 is currently processing. */
  private PatientSeries relevantSeries(String seriesName, String seriesGroup, String seriesGroupName,
      SeriesType seriesType, PatientSeriesStatus patientSeriesStatus) {
    return relevantSeries(seriesName, hepA, seriesGroup, seriesGroupName, seriesType, patientSeriesStatus);
  }

  /**
   * A relevant patient series that 8.7 also selected as its series group's
   * prioritized patient series - i.e. one of the entries 8.8 evaluates.
   */
  private PatientSeries prioritizedSeries(String seriesName, Antigen targetDisease, String seriesGroup,
      String seriesGroupName, SeriesType seriesType, PatientSeriesStatus patientSeriesStatus) {
    PatientSeries patientSeries = relevantSeries(seriesName, targetDisease, seriesGroup, seriesGroupName, seriesType,
        patientSeriesStatus);
    dataModel.getPrioritizedPatientSeriesList().add(patientSeries);
    return patientSeries;
  }

  /** The same, for HepA. */
  private PatientSeries prioritizedSeries(String seriesName, String seriesGroup, String seriesGroupName,
      SeriesType seriesType, PatientSeriesStatus patientSeriesStatus) {
    return prioritizedSeries(seriesName, hepA, seriesGroup, seriesGroupName, seriesType, patientSeriesStatus);
  }

  // ---------------------------------------------------------------------
  // Driving the step.
  // ---------------------------------------------------------------------

  /** Runs the whole step and reports the step control is handed to. */
  private LogicStepType determineBestPatientSeries() throws Exception {
    return new DetermineBestPatientSeries(dataModel).process().getLogicStepType();
  }

  private boolean isBestPatientSeries(PatientSeries patientSeries) {
    return dataModel.getBestPatientSeriesList().contains(patientSeries);
  }

  private List<String> bestPatientSeriesNames() {
    List<String> names = new ArrayList<String>();
    for (PatientSeries patientSeries : dataModel.getBestPatientSeriesList()) {
      names.add(patientSeries.getTrackedAntigenSeries().getSeriesName());
    }
    return names;
  }

  private static String readBundledResource(String resourceName) throws Exception {
    InputStream in = DetermineBestPatientSeriesTest.class.getResourceAsStream(resourceName);
    assertNotNull("the bundled Supporting Data resource " + resourceName + " is on the classpath", in);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      StringBuilder markup = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        markup.append(line).append('\n');
      }
      return markup.toString();
    } finally {
      in.close();
    }
  }

  // ---------------------------------------------------------------------
  // Table 8-14 Rule 1 - a complete prioritized patient series.
  // ---------------------------------------------------------------------

  /**
   * Rule 1's only condition: a prioritized patient series that is a complete
   * patient series is a best patient series for its series group.
   */
  @Test
  public void ruleOneACompletePrioritizedPatientSeriesIsTheBestPatientSeriesForItsSeriesGroup() throws Exception {
    PatientSeries complete = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.COMPLETE);

    determineBestPatientSeries();

    assertEquals("Table 8-14 Rule 1: a complete prioritized patient series is a best patient series",
        Arrays.asList(complete), dataModel.getBestPatientSeriesList());
  }

  /**
   * Rule 1's other four cells are all dashes, so completeness settles the answer
   * on its own. Here the prioritized series is complete <i>and</i> Evaluation
   * Only, with a complete series and a Risk series elsewhere - every one of the
   * four conditions answered the way that defeats Rules 2 and 3 - and it is still
   * a best patient series.
   */
  @Test
  public void ruleOneIsDecidedByCompletenessAloneWhateverTheOtherFourConditionsAnswer() throws Exception {
    PatientSeries complete = prioritizedSeries("HepA risk Twinrix tertiary 3-dose series", INCREASED_RISK_GROUP,
        INCREASED_RISK_GROUP_NAME, SeriesType.EVALUATION_ONLY, PatientSeriesStatus.COMPLETE);
    prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME, SeriesType.STANDARD,
        PatientSeriesStatus.COMPLETE);
    relevantSeries("HepA risk 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME, SeriesType.RISK,
        PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertTrue("Table 8-14 Rule 1's other four cells are dashes, so a complete prioritized patient series is a"
        + " best patient series however they answer; got " + bestPatientSeriesNames(),
        isBestPatientSeries(complete));
  }

  // ---------------------------------------------------------------------
  // Table 8-14 Rule 2 - an incomplete Risk series with nothing complete
  // covering it.
  // ---------------------------------------------------------------------

  /**
   * Rule 2: an incomplete Risk series is still a best patient series, because a
   * risk condition has to be addressed whether or not some other series is on
   * its way to completion.
   */
  @Test
  public void ruleTwoAnIncompleteRiskPrioritizedPatientSeriesIsTheBestPatientSeriesForItsSeriesGroup()
      throws Exception {
    PatientSeries risk = prioritizedSeries("HepA risk 2-dose series", INCREASED_RISK_GROUP,
        INCREASED_RISK_GROUP_NAME, SeriesType.RISK, PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertEquals("Table 8-14 Rule 2: an incomplete Risk prioritized patient series with no complete series in an"
        + " equivalent series group is a best patient series", Arrays.asList(risk),
        dataModel.getBestPatientSeriesList());
  }

  /**
   * Rule 2's fifth cell is a dash: once a series is Risk, a Risk series in an
   * equivalent series group does not make it redundant. Two Risk series in
   * equivalent groups are therefore both best patient series - which is exactly
   * 4.5's example of a patient who needs a short-term risk series as well as a
   * standard one.
   */
  @Test
  public void ruleTwoAppliesWhateverTheRiskInAnEquivalentSeriesGroupConditionAnswers() throws Exception {
    PatientSeries risk = prioritizedSeries("HepA risk 2-dose series", INCREASED_RISK_GROUP,
        INCREASED_RISK_GROUP_NAME, SeriesType.RISK, PatientSeriesStatus.NOT_COMPLETE);
    PatientSeries riskElsewhere = prioritizedSeries("HepA risk 1-dose series", NO_EQUIVALENT_GROUP,
        NO_EQUIVALENT_GROUP_NAME, SeriesType.RISK, PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertEquals("Table 8-14 Rule 2's fifth cell is a dash, so both Risk series are best patient series; got "
        + bestPatientSeriesNames(), Arrays.asList(risk, riskElsewhere), dataModel.getBestPatientSeriesList());
  }

  /**
   * Rule 2's second condition: a complete prioritized patient series in an
   * equivalent series group makes an incomplete Risk series redundant, so no
   * rule matches and the default outcome applies. HepA's Standard group
   * (series group 1) and Increased Risk group (series group 2) declare each
   * other as equivalent series groups in the bundled release.
   */
  @Test
  public void ruleTwoDoesNotApplyWhenAnEquivalentSeriesGroupHasACompletePrioritizedPatientSeries() throws Exception {
    PatientSeries risk = prioritizedSeries("HepA risk 2-dose series", INCREASED_RISK_GROUP,
        INCREASED_RISK_GROUP_NAME, SeriesType.RISK, PatientSeriesStatus.NOT_COMPLETE);
    PatientSeries completeInEquivalentGroup = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP,
        STANDARD_GROUP_NAME, SeriesType.STANDARD, PatientSeriesStatus.COMPLETE);

    determineBestPatientSeries();

    assertEquals("Table 8-14: the complete series in the equivalent series group is a best patient series by"
        + " Rule 1 and the incomplete Risk series it covers is not one at all; got " + bestPatientSeriesNames(),
        Arrays.asList(completeInEquivalentGroup), dataModel.getBestPatientSeriesList());
    assertFalse("Rule 2's second condition must answer Yes here, which defeats Rule 2",
        isBestPatientSeries(risk));
  }

  // ---------------------------------------------------------------------
  // Table 8-14 Rule 3 - an incomplete non-Risk series with nothing covering it.
  // ---------------------------------------------------------------------

  /**
   * Rule 3: an incomplete Standard series that nothing makes redundant - no
   * complete series and no Risk series in an equivalent series group - is a best
   * patient series.
   */
  @Test
  public void ruleThreeAnIncompleteNonRiskPrioritizedPatientSeriesIsTheBestPatientSeriesForItsSeriesGroup()
      throws Exception {
    PatientSeries standard = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertEquals("Table 8-14 Rule 3: an incomplete, non-Evaluation-Only, non-Risk prioritized patient series"
        + " with no complete and no Risk series in an equivalent series group is a best patient series",
        Arrays.asList(standard), dataModel.getBestPatientSeriesList());
  }

  /**
   * Rule 3's fifth condition, the only rule that reads it: a Risk prioritized
   * patient series in an equivalent series group makes an incomplete Standard
   * series redundant, so the Standard series is dropped and only the Risk series
   * remains.
   */
  @Test
  public void ruleThreeDoesNotApplyWhenAnEquivalentSeriesGroupHasARiskPrioritizedPatientSeries() throws Exception {
    PatientSeries standard = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.NOT_COMPLETE);
    PatientSeries riskInEquivalentGroup = prioritizedSeries("HepA risk 2-dose series", INCREASED_RISK_GROUP,
        INCREASED_RISK_GROUP_NAME, SeriesType.RISK, PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertEquals("Table 8-14 Rule 3's fifth condition must answer Yes here, leaving only the Risk series; got "
        + bestPatientSeriesNames(), Arrays.asList(riskInEquivalentGroup), dataModel.getBestPatientSeriesList());
    assertFalse("the incomplete Standard series is made redundant by the Risk series in its equivalent group",
        isBestPatientSeries(standard));
  }

  // ---------------------------------------------------------------------
  // Table 8-14 default - "No. There is no best patient series for the series
  // group."
  // ---------------------------------------------------------------------

  /**
   * The default outcome. An Evaluation Only series is never forecast, so an
   * incomplete one matches no rule: Rule 1 needs it complete, and Rules 2 and 3
   * both require the Evaluation Only condition to answer No.
   */
  @Test
  public void theDefaultOutcomeAnIncompleteEvaluationOnlySeriesIsNotABestPatientSeries() throws Exception {
    PatientSeries evaluationOnly = prioritizedSeries("HepA risk Twinrix tertiary 3-dose series",
        INCREASED_RISK_GROUP, INCREASED_RISK_GROUP_NAME, SeriesType.EVALUATION_ONLY,
        PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertFalse("Table 8-14's default: an incomplete Evaluation Only series matches no rule",
        isBestPatientSeries(evaluationOnly));
    assertTrue("the default outcome records nothing at all; got " + bestPatientSeriesNames(),
        dataModel.getBestPatientSeriesList().isEmpty());
  }

  // ---------------------------------------------------------------------
  // The two per-series conditions - "the series type of the prioritized patient
  // series".
  // ---------------------------------------------------------------------

  /**
   * Table 8-14's third and fourth conditions are phrased over "the series type
   * <i>of the prioritized patient series</i>", i.e. the one series the table is
   * being applied to, not over the antigen's other series. An Evaluation Only
   * series sitting elsewhere must not stop a Standard series being a best
   * patient series.
   */
  @Test
  public void theEvaluationOnlyConditionReadsTheSeriesTypeOfThePrioritizedPatientSeriesItself() throws Exception {
    PatientSeries standard = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.NOT_COMPLETE);
    relevantSeries("HepA risk Twinrix tertiary 3-dose series", INCREASED_RISK_GROUP, INCREASED_RISK_GROUP_NAME,
        SeriesType.EVALUATION_ONLY, PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertTrue("the Evaluation Only condition is about the prioritized patient series' own series type, not"
        + " about whether some other series is Evaluation Only", isBestPatientSeries(standard));
  }

  // ---------------------------------------------------------------------
  // Condition 2 - "Is there a PRIORITIZED patient series that is a complete
  // patient series in an EQUIVALENT SERIES GROUP?"
  // ---------------------------------------------------------------------

  /**
   * The condition asks about a <i>prioritized</i> patient series. A complete
   * relevant patient series that is not any series group's prioritized patient
   * series - one 8.1 excluded, or one that lost 8.7's selection - is not an
   * answer to it, because nothing will ever forecast that series and so it
   * cannot make anything redundant.
   *
   * <p>
   * {@code DetermineBestPatientSeries} scans
   * {@code dataModel.getPatientSeriesStepper().getList()}, which is 5.1's
   * unfiltered list of every relevant patient series for every antigen, so it
   * answers Yes on a series the pipeline had already set aside.
   */
  @Test
  public void theCompleteSeriesConditionAsksOnlyAboutPrioritizedPatientSeries() throws Exception {
    PatientSeries standard = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.NOT_COMPLETE);
    // Complete, in the equivalent series group - but not a prioritized patient
    // series, so Table 8-14's second condition must not see it.
    relevantSeries("HepA Twinrix 3-dose series", INCREASED_RISK_GROUP, INCREASED_RISK_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.COMPLETE);

    determineBestPatientSeries();

    assertTrue("Table 8-14's second condition asks whether there is a *prioritized* patient series that is"
        + " complete in an equivalent series group; a complete relevant patient series that is not one is not"
        + " an answer to it, so Rule 3 applies and the Standard series is a best patient series",
        isBestPatientSeries(standard));
  }

  /**
   * The condition asks about an <i>equivalent</i> series group. HepA's Increased
   * Risk - Pediatric Travel series (series group 3) declares an empty
   * {@code <equivalentSeriesGroups/>} element in the bundled release, so no other
   * series group of the antigen is equivalent to it and the condition can only
   * answer No - however complete the other groups' prioritized series are. 89 of
   * the release's 143 series are in this position.
   *
   * <p>
   * {@code DetermineBestPatientSeries} reads no series group at all: its second
   * condition answers Yes for any complete patient series of the current antigen,
   * so a complete Standard series suppresses a pediatric travel series that is
   * not equivalent to it.
   */
  @Test
  public void theCompleteSeriesConditionAnswersNoForASeriesThatDeclaresNoEquivalentSeriesGroup() throws Exception {
    PatientSeries noEquivalentGroup = prioritizedSeries("HepA risk 1-dose series", NO_EQUIVALENT_GROUP,
        NO_EQUIVALENT_GROUP_NAME, SeriesType.RISK, PatientSeriesStatus.NOT_COMPLETE);
    prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME, SeriesType.STANDARD,
        PatientSeriesStatus.COMPLETE);

    determineBestPatientSeries();

    assertTrue("the pediatric travel series group declares no equivalent series group, so Table 8-14's second"
        + " condition answers No and Rule 2 makes the incomplete Risk series a best patient series; got "
        + bestPatientSeriesNames(), isBestPatientSeries(noEquivalentGroup));
  }

  // ---------------------------------------------------------------------
  // Condition 5 - "Is there a PRIORITIZED patient series with a series type of
  // 'Risk' in an EQUIVALENT SERIES GROUP?"
  // ---------------------------------------------------------------------

  /**
   * A series group belongs to one antigen, so "an equivalent series group" is a
   * series group of the antigen 4.5 is currently processing. A Measles Risk
   * series sitting on 5.1's all-antigen patient series stepper is in no series
   * group of HepA's, equivalent or otherwise.
   *
   * <p>
   * This is the one condition in the class with no antigen filter of any kind -
   * the second condition at least compares
   * {@code getTargetDisease().equals(dataModel.getAntigen())} first - so the
   * step's fifth condition answers Yes if <i>any</i> patient series anywhere in
   * the assessment is a Risk series.
   */
  @Test
  public void theRiskElsewhereConditionAsksOnlyAboutThePatientSeriesOfTheAntigenBeingProcessed() throws Exception {
    PatientSeries standard = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.NOT_COMPLETE);
    // A different antigen's Risk series, on the stepper as 5.1 left it.
    relevantSeries("Measles risk series", measles, INCREASED_RISK_GROUP, INCREASED_RISK_GROUP_NAME, SeriesType.RISK,
        PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertTrue("Table 8-14's fifth condition asks about a series group equivalent to this one, which is a"
        + " series group of this antigen; a Measles Risk series is not in one, so Rule 3 applies",
        isBestPatientSeries(standard));
  }

  /**
   * The fifth condition asks about a <i>prioritized</i> patient series, exactly
   * as the second does. A Risk relevant patient series that no series group
   * selected cannot make anything redundant, because nothing will forecast it.
   */
  @Test
  public void theRiskElsewhereConditionAsksOnlyAboutPrioritizedPatientSeries() throws Exception {
    PatientSeries standard = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.NOT_COMPLETE);
    // Risk, in the equivalent series group - but not a prioritized patient series.
    relevantSeries("HepA risk Twinrix secondary 3-dose series", INCREASED_RISK_GROUP, INCREASED_RISK_GROUP_NAME,
        SeriesType.RISK, PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertTrue("Table 8-14's fifth condition asks whether there is a *prioritized* patient series with a series"
        + " type of 'Risk' in an equivalent series group; a Risk relevant patient series that is not one is"
        + " not an answer to it, so Rule 3 applies", isBestPatientSeries(standard));
  }

  /**
   * The series-group half of the same condition, in the majority shape: a
   * prioritized patient series whose series group declares no equivalent series
   * group cannot be made redundant by a Risk series in any other group, because
   * no other group is equivalent to it.
   */
  @Test
  public void theRiskElsewhereConditionAnswersNoForASeriesThatDeclaresNoEquivalentSeriesGroup() throws Exception {
    // HepA's series group 3 declares <equivalentSeriesGroups/>. The release's own
    // series in that group is a Risk series; a Standard-type one is used here
    // because Table 8-14's fifth condition is only ever read by Rule 3, which
    // requires the prioritized patient series itself not to be Risk.
    PatientSeries noEquivalentGroup = prioritizedSeries("HepA pediatric travel standard series",
        NO_EQUIVALENT_GROUP, NO_EQUIVALENT_GROUP_NAME, SeriesType.STANDARD, PatientSeriesStatus.NOT_COMPLETE);
    prioritizedSeries("HepA risk 2-dose series", INCREASED_RISK_GROUP, INCREASED_RISK_GROUP_NAME, SeriesType.RISK,
        PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertTrue("the pediatric travel series group declares no equivalent series group, so Table 8-14's fifth"
        + " condition answers No and Rule 3 makes the series a best patient series; got "
        + bestPatientSeriesNames(), isBestPatientSeries(noEquivalentGroup));
  }

  // ---------------------------------------------------------------------
  // Can the two "equivalent series group" conditions be asked at all?
  // ---------------------------------------------------------------------

  /**
   * The prior question behind the four tests above. Two of Table 8-14's five
   * conditions are defined entirely over "an equivalent series group", and the
   * Supporting Data carries the answer per antigen series - but nothing in the
   * domain model can hold it, so neither condition can be asked the way the
   * specification phrases it. This is the same kind of "can the condition even be
   * expressed?" test as 6.2/7.1/7.6's conditional skip context.
   */
  @Test
  public void theEquivalentSeriesGroupsTableEightFourteenTurnsOnAreCarriedByTheDomainModel() {
    List<String> accessors = new ArrayList<String>();
    for (Class<?> domainClass : Arrays.asList(SelectPatientSeries.class, AntigenSeries.class)) {
      for (Method method : domainClass.getDeclaredMethods()) {
        if (method.getName().toLowerCase().contains("equivalent")) {
          accessors.add(domainClass.getSimpleName() + "." + method.getName());
        }
      }
    }

    assertFalse("two of Table 8-14's five conditions are defined over 'an equivalent series group' and the"
        + " bundled 4.65-508 release declares one <equivalentSeriesGroups> element per antigen series (54 of"
        + " 143 populated), but neither SelectPatientSeries nor AntigenSeries carries it, so DetermineBest"
        + "PatientSeries has nothing to read and answers both conditions over the whole antigen instead",
        accessors.isEmpty());
  }

  /**
   * The companion to the test above, and the evidence for it: the data really is
   * in the release. HepA's Standard group declares series group 2 as equivalent,
   * its Increased Risk group declares series group 1, and its pediatric travel
   * series declares none.
   */
  @Test
  public void theReleaseDeclaresTheEquivalentSeriesGroupsTableEightFourteenNeeds() throws Exception {
    String markup = readBundledResource("/AntigenSupportingData- HepA-508.xml");

    assertTrue("HepA's Standard group series declares series group 2 as an equivalent series group",
        markup.contains("<equivalentSeriesGroups>2</equivalentSeriesGroups>"));
    assertTrue("HepA's Increased Risk group series declare series group 1 as an equivalent series group",
        markup.contains("<equivalentSeriesGroups>1</equivalentSeriesGroups>"));
    assertTrue("HepA's Increased Risk - Pediatric Travel series declares no equivalent series group",
        markup.contains("<equivalentSeriesGroups/>"));
  }

  // ---------------------------------------------------------------------
  // Purpose and Entry Conditions - "the set of prioritized patient series, one
  // per Series Group ... one or more non-redundant best patient series will
  // remain".
  // ---------------------------------------------------------------------

  /**
   * 8.8 applies Table 8-14 once per prioritized patient series, so an antigen
   * whose series groups each produced a complete winner leaves both of them
   * behind as best patient series. (Whether 8.7 actually produces one per series
   * group is 8.7's own question - see the class comment.)
   */
  @Test
  public void theStepEvaluatesEveryPrioritizedPatientSeriesOnePerSeriesGroup() throws Exception {
    PatientSeries standard = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.COMPLETE);
    PatientSeries increasedRisk = prioritizedSeries("HepA risk 2-dose series", INCREASED_RISK_GROUP,
        INCREASED_RISK_GROUP_NAME, SeriesType.RISK, PatientSeriesStatus.COMPLETE);

    determineBestPatientSeries();

    assertEquals("the decision table is applied to the set of prioritized patient series, one per series group;"
        + " got " + bestPatientSeriesNames(), Arrays.asList(standard, increasedRisk),
        dataModel.getBestPatientSeriesList());
  }

  /**
   * 8.8 runs inside 4.5's per-antigen loop, so it evaluates the prioritized
   * patient series of the antigen being processed. 4.5 clears
   * {@code prioritizedPatientSeriesList} on every pass, so this can only happen
   * if some other antigen's series is still on it - but the step filters for it
   * explicitly, and the filter is correct.
   */
  @Test
  public void theStepEvaluatesOnlyThePrioritizedPatientSeriesOfTheAntigenBeingProcessed() throws Exception {
    PatientSeries hepASeries = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.COMPLETE);
    prioritizedSeries("Measles 2-dose series", measles, STANDARD_GROUP, STANDARD_GROUP_NAME, SeriesType.STANDARD,
        PatientSeriesStatus.COMPLETE);

    determineBestPatientSeries();

    assertEquals("8.8 evaluates the prioritized patient series of the antigen 4.5 is currently processing; got "
        + bestPatientSeriesNames(), Arrays.asList(hepASeries), dataModel.getBestPatientSeriesList());
  }

  /**
   * The Purpose's closing sentence - "After this process, one or more
   * non-redundant best patient series will remain. Each of these best patient
   * series are necessary to fully protect the patient." Two prioritized series in
   * equivalent series groups, one complete and one not: only the complete one is
   * necessary, so only it remains.
   */
  @Test
  public void onlyNonRedundantBestPatientSeriesRemain() throws Exception {
    PatientSeries complete = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.COMPLETE);
    PatientSeries madeRedundant = prioritizedSeries("HepA Twinrix 3-dose series", INCREASED_RISK_GROUP,
        INCREASED_RISK_GROUP_NAME, SeriesType.STANDARD, PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertEquals("only the non-redundant best patient series remain; got " + bestPatientSeriesNames(),
        Arrays.asList(complete), dataModel.getBestPatientSeriesList());
    assertFalse("a series covered by a complete series in an equivalent series group is redundant",
        isBestPatientSeries(madeRedundant));
  }

  // ---------------------------------------------------------------------
  // State Changes - "Rules 1-3 add the prioritized series to
  // dataModel.getBestPatientSeriesList()".
  // ---------------------------------------------------------------------

  /**
   * The best patient series list is the one Chapter 8 output that outlives an
   * antigen pass: 4.5 creates it once, on its first pass, and never clears it,
   * so 4.6 and Chapter 9 see every antigen's best patient series together. 8.8
   * must therefore append to it rather than replace what earlier passes left.
   */
  @Test
  public void theBestPatientSeriesListAccumulatesAcrossAntigenPasses() throws Exception {
    // What an earlier antigen pass left behind.
    PatientSeries fromAnEarlierPass = relevantSeries("Measles 2-dose series", measles, STANDARD_GROUP,
        STANDARD_GROUP_NAME, SeriesType.STANDARD, PatientSeriesStatus.COMPLETE);
    dataModel.getBestPatientSeriesList().add(fromAnEarlierPass);

    PatientSeries thisPass = prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME,
        SeriesType.STANDARD, PatientSeriesStatus.COMPLETE);

    determineBestPatientSeries();

    assertEquals("8.8 appends this antigen pass's best patient series to what earlier passes left; got "
        + bestPatientSeriesNames(), Arrays.asList(fromAnEarlierPass, thisPass),
        dataModel.getBestPatientSeriesList());
  }

  /**
   * The default outcome's only state change is that there isn't one - the series
   * is excluded from the best series list and nothing else about it changes. In
   * particular 8.8 does not consume the prioritized patient series list; 4.5
   * clears it on the next pass.
   */
  @Test
  public void theStepLeavesThePrioritizedPatientSeriesListAloneForFourFiveToClear() throws Exception {
    PatientSeries evaluationOnly = prioritizedSeries("HepA risk Twinrix tertiary 3-dose series",
        INCREASED_RISK_GROUP, INCREASED_RISK_GROUP_NAME, SeriesType.EVALUATION_ONLY,
        PatientSeriesStatus.NOT_COMPLETE);

    determineBestPatientSeries();

    assertEquals("8.8's only state change is the best patient series list", Arrays.asList(evaluationOnly),
        dataModel.getPrioritizedPatientSeriesList());
    assertEquals("a series that matches no rule is left exactly as it was", PatientSeriesStatus.NOT_COMPLETE,
        evaluationOnly.getPatientSeriesStatus());
  }

  // ---------------------------------------------------------------------
  // Next Steps - unconditional to 4.5.
  // ---------------------------------------------------------------------

  /** 8.8 always hands control back to 4.5's antigen-loop driver. */
  @Test
  public void theStepAlwaysHandsOffToSelectBestPatientSeries() throws Exception {
    prioritizedSeries("HepA 2-dose series", STANDARD_GROUP, STANDARD_GROUP_NAME, SeriesType.STANDARD,
        PatientSeriesStatus.COMPLETE);

    assertEquals("8.8 is unconditional to 4.5 Select Best Patient Series, not to another Chapter 8 section",
        LogicStepType.SELECT_BEST_PATIENT_SERIES, determineBestPatientSeries());
  }

  /**
   * ... including when there is no prioritized patient series at all, which 8.2's
   * Table 8-3 can produce for a series group with no scorable patient series.
   */
  @Test
  public void theStepHandsOffToSelectBestPatientSeriesEvenWithNoPrioritizedPatientSeries() throws Exception {
    assertEquals("8.8 is unconditional to 4.5 even with an empty prioritized patient series list",
        LogicStepType.SELECT_BEST_PATIENT_SERIES, determineBestPatientSeries());
    assertTrue("and records no best patient series", dataModel.getBestPatientSeriesList().isEmpty());
  }
}
