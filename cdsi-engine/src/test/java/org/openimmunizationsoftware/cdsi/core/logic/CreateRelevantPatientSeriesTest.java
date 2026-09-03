package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openimmunizationsoftware.cdsi.core.data.DataModel;
import org.openimmunizationsoftware.cdsi.core.domain.Antigen;

/**
 * Section 4.3 "Create Relevant Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, page 34, Figure 4-4) as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/04-03-create-relevant-patient-series/index.md}.
 *
 * <p>
 * 4.3 defines <i>no</i> inputs, <i>no</i> business rules and <i>no</i> decision
 * tables of its own. The specification says so explicitly and points elsewhere
 * for the real logic: section 4.3 and Chapter 5 share the title "Create
 * Relevant Patient Series", and it is Chapter 5 (5.1 Select Relevant Patient
 * Series, Tables 5-2/5-4/5-5, CALCDTIND-1/CALCDTIND-2) that actually decides
 * whether an antigen series is relevant. 4.3 itself is a short paragraph in
 * Chapter 4's processing-model overview that the specification deliberately
 * leaves as an orchestration concern - "similar to gathering necessary data
 * (section 4.1), create relevant patient series will likely vary from system to
 * system based on design details and technologies used."
 *
 * <p>
 * There is therefore no normative rule here for the implementation to mismatch.
 * What is verifiable is the step package's documented <b>State Changes</b> and
 * <b>Next Steps</b>, which record what
 * {@link CreateRelevantPatientSeries#process()} actually contributes: it is the
 * <b>loop driver</b> around 5.1, not a decision step. On its first call it
 * builds {@code dataModel.antigenSelectedList} (every antigen, or a
 * caller-supplied filtered subset - see
 * {@code DataModel#getAntigenLabelFilterList()}) and starts iterating; on each
 * subsequent call it advances the position counter. While antigens remain it
 * transitions to 5.1; once every antigen has been iterated it transitions to
 * 4.4. It performs no antigen-series relevance decisions itself. These tests
 * pin exactly that.
 *
 * <p>
 * The step is driven directly through its public {@code process()}, with a
 * hand-built {@code DataModel} carrying only an antigen map: it reads nothing
 * else. Unlike {@code GatherNecessaryDataTest}, constructing the returned next
 * step is <i>not</i> inert - {@code SelectRelevantPatientSeries}'s constructor
 * reads {@code antigenSelectedList.get(antigenSelectedPos)} and walks
 * {@code antigenSeriesList} - but with no antigen series loaded that
 * constructor does nothing further, which is what keeps this a 4.3 test rather
 * than a 5.1 test. Each iteration constructs a fresh
 * {@code CreateRelevantPatientSeries}, exactly as the real pipeline does:
 * {@code SelectRelevantPatientSeries.process()} returns a new instance of this
 * step every time it finishes an antigen.
 *
 * <p>
 * <b>On iteration order:</b> {@code DataModel.getAntigenList()} is built from a
 * {@code HashMap}'s values, so the order antigens are visited in is not
 * specified by anything and is not asserted literally here. What is asserted is
 * that the loop visits exactly the antigens the data model holds, each exactly
 * once, in whatever order {@code getAntigenList()} reports - which is the only
 * ordering claim 4.3 or the code actually makes.
 */
public class CreateRelevantPatientSeriesTest {

  private static final String HEPB = "HepB";
  private static final String MEASLES = "Measles";
  private static final String POLIO = "Polio";

  private DataModel dataModel;

  @Before
  public void setUp() {
    dataModel = new DataModel();
  }

  /** Puts the named antigens on the data model's Supporting Data antigen map. */
  private void supportingDataAntigens(String... antigenNames) {
    for (String antigenName : antigenNames) {
      dataModel.getOrCreateAntigen(antigenName);
    }
  }

  private LogicStep process() throws Exception {
    return new CreateRelevantPatientSeries(dataModel).process();
  }

  /** The antigen 5.1 would read on the current iteration. */
  private Antigen antigenUnderIteration() {
    return dataModel.getAntigenSelectedList().get(dataModel.getAntigenSelectedPos());
  }

  private static List<String> namesOf(List<Antigen> antigens) {
    List<String> names = new ArrayList<String>();
    for (Antigen antigen : antigens) {
      names.add(antigen.getName());
    }
    return names;
  }

  /**
   * Runs the loop the way the pipeline does - one {@code process()} per
   * iteration, a fresh step instance each time, as if 5.1 had handed control
   * back - and returns every antigen 5.1 would have been pointed at, in the
   * order it would have seen them. The final transition is asserted to be 4.4,
   * since that is what "the loop terminated" means.
   */
  private List<Antigen> runLoopToCompletion() throws Exception {
    List<Antigen> visited = new ArrayList<Antigen>();
    // Guards against a loop that never terminates: the real bound is the size of
    // the selected list, which is at most the number of antigens in the model.
    int maximumIterations = dataModel.getAntigenList().size() + 1;
    for (int i = 0; i <= maximumIterations; i++) {
      LogicStep next = process();
      if (next.getLogicStepType() == LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES) {
        return visited;
      }
      assertEquals("While antigens remain, 4.3 delegates to 5.1 and nowhere else",
          LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, next.getLogicStepType());
      visited.add(antigenUnderIteration());
    }
    throw new AssertionError("4.3's loop did not terminate after " + maximumIterations
        + " iterations; visited so far: " + namesOf(visited));
  }

  /**
   * State Changes: "On its first call it builds
   * {@code dataModel.antigenSelectedList} (all antigens ..., or a
   * caller-supplied filtered subset)". With no caller-supplied filter, every
   * antigen the data model holds is selected.
   */
  @Test
  public void firstCallBuildsTheAntigenSelectedListFromEveryAntigenInTheDataModel() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);
    assertNull("Precondition: nothing has built the selected list yet", dataModel.getAntigenSelectedList());

    process();

    List<Antigen> selected = dataModel.getAntigenSelectedList();
    assertNotNull("4.3 must put an antigen selected list on the data model", selected);
    assertEquals(dataModel.getAntigenList().size(), selected.size());
    assertEquals("With no label filter the selected list is the data model's antigen list, in its order",
        dataModel.getAntigenList(), selected);
  }

  /**
   * State Changes: the first call "starts iterating". Next Steps: it
   * "transitions to 5.1 Select Relevant Patient Series while antigens remain to
   * process". The position counter starts at the first antigen, so 5.1's
   * {@code antigenSelectedList.get(antigenSelectedPos)} resolves.
   */
  @Test
  public void firstCallStartsAtTheFirstAntigenAndDelegatesToSelectRelevantPatientSeries() throws Exception {
    supportingDataAntigens(HEPB, MEASLES);

    LogicStep next = process();

    assertEquals(0, dataModel.getAntigenSelectedPos());
    assertEquals(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, next.getLogicStepType());
    assertSame("5.1 must be pointed at the first antigen of the selected list",
        dataModel.getAntigenSelectedList().get(0), antigenUnderIteration());
  }

  /**
   * Plain-Language Walkthrough: "it's the outer loop that walks through every
   * antigen the patient could plausibly need, calling into
   * {@code SelectRelevantPatientSeries} (5.1) once per antigen". Each antigen
   * gets exactly one turn - no antigen is skipped, none is visited twice - and
   * the loop then terminates.
   */
  @Test
  public void everyAntigenIsVisitedExactlyOnceBeforeTheLoopEnds() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);
    List<Antigen> expectedOrder = new ArrayList<Antigen>(dataModel.getAntigenList());

    List<Antigen> visited = runLoopToCompletion();

    assertEquals("One delegation to 5.1 per antigen, no more and no fewer", expectedOrder.size(), visited.size());
    assertEquals("The loop walks the data model's antigen list in its own order", expectedOrder, visited);
  }

  /**
   * Next Steps: "and to 4.4 Evaluate and Forecast all Relevant Patient Series
   * once every antigen has been iterated." Asserted here on its own, rather
   * than only as the loop's exit condition.
   */
  @Test
  public void transitionsToEvaluateAndForecastOnceEveryAntigenHasBeenIterated() throws Exception {
    supportingDataAntigens(HEPB, MEASLES);

    assertEquals(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, process().getLogicStepType());
    assertEquals(LogicStepType.SELECT_RELEVANT_PATIENT_SERIES, process().getLogicStepType());
    assertEquals("After the last antigen the loop hands off to 4.4",
        LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, process().getLogicStepType());
  }

  /**
   * The loop's exit is stable: once the position counter has run past the end
   * of the selected list, further calls keep transitioning to 4.4 rather than
   * wrapping around, re-selecting a final antigen, or failing on an
   * out-of-bounds read.
   */
  @Test
  public void callingAgainAfterTheLoopHasFinishedStillTransitionsToEvaluateAndForecast() throws Exception {
    supportingDataAntigens(HEPB);
    runLoopToCompletion();

    assertEquals(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, process().getLogicStepType());
    assertEquals(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, process().getLogicStepType());
  }

  /**
   * State Changes: "on each subsequent call it just advances the position
   * counter" - the selected list is built once and reused, not rebuilt per
   * iteration. Identity matters: 5.1 indexes into whatever list is on the data
   * model, so a silently replaced list would restart the loop.
   */
  @Test
  public void subsequentCallsReuseTheSameSelectedListRatherThanRebuildingIt() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);

    process();
    List<Antigen> builtOnFirstCall = dataModel.getAntigenSelectedList();
    process();

    assertSame("The selected list must be built once, not rebuilt on every call",
        builtOnFirstCall, dataModel.getAntigenSelectedList());
    assertEquals("The second call advances the position counter", 1, dataModel.getAntigenSelectedPos());
  }

  /**
   * Degenerate case of "once every antigen has been iterated": with no antigens
   * at all there is nothing for 5.1 to decide about, so the step builds an
   * empty selected list and goes straight to 4.4 without ever delegating.
   *
   * <p>
   * Note this is narrower than {@code transitions.yaml}'s stated condition for
   * the 5.1 branch, "the antigen-selected list has not been built yet, or more
   * antigens remain to iterate": having not been built yet is not on its own
   * enough - the code branches purely on whether an antigen remains.
   */
  @Test
  public void noAntigensAtAllSkipsStraightToEvaluateAndForecast() throws Exception {
    LogicStep next = process();

    assertEquals(LogicStepType.EVALUATE_AND_FORECAST_ALL_PATIENT_SERIES, next.getLogicStepType());
    assertNotNull(dataModel.getAntigenSelectedList());
    assertTrue("Nothing to select from, so nothing selected", dataModel.getAntigenSelectedList().isEmpty());
  }

  /**
   * State Changes: "or a caller-supplied filtered subset - see
   * {@code dataModel.getAntigenLabelFilterList()}". A caller (e.g.
   * {@code ForecastServlet}) naming a subset of antigen labels gets a loop over
   * just those, with the rest excluded from the iteration entirely.
   */
  @Test
  public void antigenLabelFilterRestrictsTheLoopToTheNamedAntigens() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);
    dataModel.setAntigenLabelFilterList(Arrays.asList(HEPB, POLIO));

    List<Antigen> visited = runLoopToCompletion();

    assertEquals(2, visited.size());
    List<String> visitedNames = namesOf(visited);
    Collections.sort(visitedNames);
    assertEquals(Arrays.asList(HEPB, POLIO), visitedNames);
    assertEquals("The filtered subset is what lands on the data model",
        2, dataModel.getAntigenSelectedList().size());
  }

  /**
   * Implementation behaviour with no specification basis, pinned because it is
   * a caller-visible surprise: a filter that matches no antigen at all does not
   * produce an empty loop or an error - {@code foundAtLeastOne} stays false and
   * the step falls back to forecasting every antigen ("No antigens selected,
   * forecasting for all antigens"). A caller who misspells a label silently
   * gets everything rather than nothing.
   */
  @Test
  public void antigenLabelFilterMatchingNothingFallsBackToEveryAntigen() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);
    dataModel.setAntigenLabelFilterList(Arrays.asList("not-a-real-antigen"));

    List<Antigen> visited = runLoopToCompletion();

    assertEquals("An unmatched filter falls back to the full antigen list", 3, visited.size());
    assertEquals(dataModel.getAntigenList(), dataModel.getAntigenSelectedList());
  }

  /**
   * The filter is optional: an empty list is treated the same as no filter at
   * all, i.e. every antigen is iterated. (The null case is covered by
   * {@link #firstCallBuildsTheAntigenSelectedListFromEveryAntigenInTheDataModel()}.)
   */
  @Test
  public void emptyAntigenLabelFilterIsTreatedAsNoFilter() throws Exception {
    supportingDataAntigens(HEPB, MEASLES);
    dataModel.setAntigenLabelFilterList(new ArrayList<String>());

    List<Antigen> visited = runLoopToCompletion();

    assertEquals(2, visited.size());
  }

  /**
   * Filter matching is by exact antigen name - the code tests
   * {@code antigenLabelFilterList.contains(antigen.getName())} - so it is
   * case-sensitive, and a differently-cased label matches nothing and therefore
   * takes the fall-back-to-everything path above. Pinned as implementation
   * behaviour; 4.3 says nothing about how a caller's labels are matched.
   */
  @Test
  public void antigenLabelFilterMatchesOnExactAntigenNameOnly() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);
    dataModel.setAntigenLabelFilterList(Arrays.asList("hepb"));

    List<Antigen> visited = runLoopToCompletion();

    assertEquals("A differently-cased label does not match, so the filter is ignored entirely",
        3, visited.size());
  }

  /**
   * 5.1 decides relevance by comparing {@code antigenSeries.getTargetDisease()}
   * against the antigen 4.3 pointed it at, so the selected list must hold the
   * Supporting Data {@code Antigen} instances themselves rather than copies
   * carrying the same name.
   */
  @Test
  public void selectedAntigensAreTheSupportingDataInstancesThemselves() throws Exception {
    supportingDataAntigens(HEPB, MEASLES);
    Antigen hepB = dataModel.getAntigen(HEPB);

    runLoopToCompletion();

    boolean sameInstanceFound = false;
    for (Antigen selected : dataModel.getAntigenSelectedList()) {
      if (selected == hepB) {
        sameInstanceFound = true;
      }
    }
    assertTrue("The selected list must hold the Supporting Data Antigen instances, not copies",
        sameInstanceFound);
  }

  /**
   * The loop is driven by the Supporting Data antigen list, not by the
   * patient's immunization history: an antigen the patient has no dose for is
   * still iterated, because a patient with no HepB dose still needs a HepB
   * forecast. This is also the point at which the step package's
   * <b>State Changes</b> wording is imprecise - it says the first call builds
   * the selected list from "all antigens the patient has data for", where the
   * code in fact iterates {@code dataModel.getAntigenList()}, i.e. every
   * antigen in the loaded Supporting Data, regardless of what the patient
   * received. A documentation nit, not a code defect: filtering to antigens the
   * patient has data for would suppress forecasts for everything never given.
   */
  @Test
  public void antigensThePatientHasNoDataForAreStillIterated() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);
    assertTrue("Precondition: this patient has no organized history at all",
        dataModel.getAntigenAdministeredRecordList().isEmpty());

    List<Antigen> visited = runLoopToCompletion();

    assertEquals("Every antigen is iterated even with an empty immunization history", 3, visited.size());
  }

  /**
   * State Changes: "It performs no antigen-series relevance decisions itself -
   * those all happen in 5.1, once per antigen, each time this step delegates to
   * it." Driving 4.3's whole loop must therefore leave every piece of state
   * that belongs to Chapter 5 untouched: no antigen series selected, no patient
   * series created, no current antigen set.
   */
  @Test
  public void stepMakesNoRelevanceDecisionsOfItsOwn() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);

    runLoopToCompletion();

    assertNull("Choosing antigen series is 5.1's job, not 4.3's", dataModel.getAntigenSeriesSelectedList());
    assertTrue("Creating patient series is 5.1's job, not 4.3's",
        dataModel.getSelectedPatientSeriesList().isEmpty());
    assertNull("4.3 points 5.1 at an antigen via the selected list; it does not set the current antigen",
        dataModel.getAntigen());
  }

  /**
   * Undocumented state change, pinned so it is visible rather than silent:
   * besides advancing the antigen-selected position, each subsequent call also
   * advances {@code DataModel.antigenAdministeredRecordPos}. The step package's
   * State Changes says only that it "advances the position counter", singular.
   * The extra counter is written by this one line and read nowhere else in
   * {@code cdsi-engine} or {@code cdsi-web} (the field the evaluation loop
   * actually reads is the distinct {@code selectedAntigenAdministeredRecordPos}),
   * so it is currently inert - which is exactly why a test should record it
   * before anyone starts reading it. Recorded for review, not classified.
   */
  @Test
  public void subsequentCallsAlsoAdvanceTheUnrelatedAntigenAdministeredRecordPosition() throws Exception {
    supportingDataAntigens(HEPB, MEASLES, POLIO);

    process();
    assertEquals("The first call sets up the loop and leaves the other counter alone",
        -1, dataModel.getAntigenAdministeredRecordPos());

    process();
    assertEquals(0, dataModel.getAntigenAdministeredRecordPos());
    process();
    assertEquals(1, dataModel.getAntigenAdministeredRecordPos());
  }
}
