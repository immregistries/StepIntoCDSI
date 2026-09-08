package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

/**
 * Infra with no numbered specification subsection of its own (see
 * {@code mappings/spec-to-code.yaml}'s notes under section "4.5"): it
 * implements the repetition Chapter 8's own overview text describes -
 * "Process steps 8.1 through 8.7 are repeated for each series group ...
 * Process step 8.8 is then used to determine which prioritized patient
 * series are selected as a best patient series" - a loop that previously did
 * not exist anywhere in the engine (see
 * {@code cdsi-reference/step-tests/cross-cutting-notes.md}'s 2026-09-05
 * "Chapter 8 has no series group" entry).
 *
 * <p>
 * {@link SelectBestPatientSeries} (4.5) populates the series-group stepper
 * once per antigen (see {@code SelectBestPatientSeriesTest}); this class
 * steps through it, one series group per call, building
 * {@code selectedPatientSeriesList} - the antigen-and-series-group scoped
 * list 8.1 actually reads - from the patient series stepper (5.1's output).
 * Every exit from the 8.1-8.7 scoring phase (8.2's shortcut outcomes, 8.7's
 * normal path) dispatches back here rather than straight to 8.8, so this
 * class also decides when an antigen's groups are exhausted and it is time
 * for 8.8 to judge across them.
 */
public class SelectNextSeriesGroupTest {

  private static final String STANDARD_GROUP = "1";
  private static final String INCREASED_RISK_GROUP = "2";

  private DataModel dataModel;
  private Antigen hepB;
  private Antigen measles;

  @Before
  public void setUp() {
    dataModel = new DataModel();
    hepB = dataModel.getOrCreateAntigen("HepB");
    measles = dataModel.getOrCreateAntigen("Measles");
    dataModel.setAntigen(hepB);
  }

  // ---------------------------------------------------------------------
  // Fixture builders - the state 4.5 leaves behind, and the raw patient
  // series list 5.1 leaves behind, before this step runs.
  // ---------------------------------------------------------------------

  private PatientSeries relevantPatientSeries(String seriesName, Antigen targetDisease, String seriesGroup) {
    SelectPatientSeries selectPatientSeries = new SelectPatientSeries();
    selectPatientSeries.setSeriesGroup(seriesGroup);

    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setTargetDisease(targetDisease);
    antigenSeries.setSelectPatientSeries(selectPatientSeries);

    PatientSeries patientSeries = new PatientSeries(antigenSeries);
    dataModel.getPatientSeriesStepper().add(patientSeries);
    return patientSeries;
  }

  private void queueSeriesGroups(String... seriesGroups) {
    dataModel.getSeriesGroupStepper().reset();
    dataModel.getSeriesGroupStepper().setList(new ArrayList<String>(Arrays.asList(seriesGroups)));
  }

  private LogicStep process() throws Exception {
    return new SelectNextSeriesGroup(dataModel).process();
  }

  private static List<String> seriesNamesOf(List<PatientSeries> patientSeriesList) {
    List<String> names = new ArrayList<String>();
    for (PatientSeries patientSeries : patientSeriesList) {
      names.add(patientSeries.getTrackedAntigenSeries().getSeriesName());
    }
    return names;
  }

  // ---------------------------------------------------------------------
  // Stepping through the antigen's series groups.
  // ---------------------------------------------------------------------

  /**
   * The first call advances the stepper from "not started" to its first
   * queued group, builds {@code selectedPatientSeriesList} scoped to that
   * group, and dispatches to 8.1.
   */
  @Test
  public void firstCallSelectsTheFirstSeriesGroupAndDelegatesToPreFilterPatientSeries() throws Exception {
    queueSeriesGroups(STANDARD_GROUP, INCREASED_RISK_GROUP);
    PatientSeries standard = relevantPatientSeries("HepB standard", hepB, STANDARD_GROUP);
    relevantPatientSeries("HepB risk", hepB, INCREASED_RISK_GROUP);

    LogicStep next = process();

    assertEquals(STANDARD_GROUP, dataModel.getCurrentSeriesGroup());
    assertEquals(Arrays.asList("HepB standard"), seriesNamesOf(dataModel.getSelectedPatientSeriesList()));
    assertEquals(LogicStepType.PRE_FILTER_PATIENT_SERIES, next.getLogicStepType());
  }

  /** Each re-entry (from 8.2's shortcuts or 8.7) advances exactly one group. */
  @Test
  public void eachSubsequentCallAdvancesToTheNextSeriesGroup() throws Exception {
    queueSeriesGroups(STANDARD_GROUP, INCREASED_RISK_GROUP);
    relevantPatientSeries("HepB standard", hepB, STANDARD_GROUP);
    PatientSeries risk = relevantPatientSeries("HepB risk", hepB, INCREASED_RISK_GROUP);

    process();
    LogicStep next = process();

    assertEquals(INCREASED_RISK_GROUP, dataModel.getCurrentSeriesGroup());
    assertEquals(Arrays.asList("HepB risk"), seriesNamesOf(dataModel.getSelectedPatientSeriesList()));
    assertEquals(LogicStepType.PRE_FILTER_PATIENT_SERIES, next.getLogicStepType());
    assertEquals(risk, dataModel.getSelectedPatientSeriesList().get(0));
  }

  /**
   * Once every queued series group has been visited, the antigen's groups are
   * exhausted: the current series group is cleared and control passes to 8.8,
   * which judges across everything 8.7 accumulated in
   * {@code prioritizedPatientSeriesList} for this antigen.
   */
  @Test
  public void groupsExhaustedDelegatesToDetermineBestPatientSeries() throws Exception {
    queueSeriesGroups(STANDARD_GROUP);
    relevantPatientSeries("HepB standard", hepB, STANDARD_GROUP);

    process();
    LogicStep next = process();

    assertNull("No series group is current once the antigen's groups are exhausted",
        dataModel.getCurrentSeriesGroup());
    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  /** An antigen with no series groups queued goes straight to 8.8. */
  @Test
  public void noSeriesGroupsQueuedGoesStraightToDetermineBestPatientSeries() throws Exception {
    LogicStep next = process();

    assertEquals(LogicStepType.DETERMINE_BEST_PATIENT_SERIES, next.getLogicStepType());
  }

  // ---------------------------------------------------------------------
  // Scoping selectedPatientSeriesList - antigen and series group both.
  // ---------------------------------------------------------------------

  /**
   * {@code selectedPatientSeriesList} must hold exactly the patient series of
   * the current antigen <b>and</b> the current series group - not the whole
   * antigen (that would undo the point of the loop) and not the whole
   * series group across every antigen that happens to use the same group id.
   */
  @Test
  public void selectedPatientSeriesListIsScopedToBothTheAntigenAndTheSeriesGroup() throws Exception {
    queueSeriesGroups(STANDARD_GROUP);
    PatientSeries hepBStandard = relevantPatientSeries("HepB standard", hepB, STANDARD_GROUP);
    relevantPatientSeries("HepB risk", hepB, INCREASED_RISK_GROUP);
    relevantPatientSeries("Measles standard", measles, STANDARD_GROUP);

    process();

    assertEquals(Arrays.asList(hepBStandard), dataModel.getSelectedPatientSeriesList());
  }

  /**
   * A series group with no matching patient series (for this antigen) still
   * gets its turn, with an empty list handed to 8.1 - the series-group
   * analogue of 4.5's own "antigen with no patient series" case.
   */
  @Test
  public void aSeriesGroupWithNoMatchingPatientSeriesStillGetsItsTurn() throws Exception {
    queueSeriesGroups(STANDARD_GROUP);

    LogicStep next = process();

    assertEquals(STANDARD_GROUP, dataModel.getCurrentSeriesGroup());
    assertTrue("Nothing relevant for this group, so nothing handed to 8.1",
        dataModel.getSelectedPatientSeriesList().isEmpty());
    assertEquals(LogicStepType.PRE_FILTER_PATIENT_SERIES, next.getLogicStepType());
  }
}
