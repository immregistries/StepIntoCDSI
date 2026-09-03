package org.openimmunizationsoftware.cdsi.core.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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

/**
 * Section 4.5 "Select Best Patient Series" (Logic Specification for ACIP
 * Recommendations v4.6, page 39, Figure 4-7 "Select Best Series Process Model")
 * as documented in
 * {@code cdsi-reference/logic-spec/versions/4.6/steps/04-05-select-best-patient-series/index.md}.
 *
 * <p>
 * 4.5 defines <i>no</i> inputs, <i>no</i> business rules and <i>no</i> decision
 * tables of its own. The section says so directly - "the process of selecting
 * the best patient series at the highest level is a simple iterative process
 * which loops through each antigen and applies the business rules found in
 * Chapter 8 to each antigen" - and the actual scoring and prioritization logic
 * lives in Chapter 8 (8.1 Pre-Filter Patient Series through 8.9 Determine Best
 * Patient Series). This is the same shape as 4.3: the class named for the
 * chapter-4 summary is really the outer loop.
 *
 * <p>
 * What <i>is</i> normative here is Figure 4-7, which is a small, concrete state
 * machine: <b>Get first antigen</b>, then a loop of <b>Select Best Patient
 * Series (Chapter 8)</b> followed by the decision <b>"Is there another antigen
 * to process?"</b> - Yes leads to <b>Get next antigen</b> and back into Chapter
 * 8, No ends the process. Plus one claim from the section's prose that has real
 * consequences for state: "A best patient series will be selected for each
 * Series Group ... In other cases, multiple best patient series may be selected
 * for a patient", which means the best-patient-series list must <i>accumulate</i>
 * across the loop rather than be reset per antigen. These tests pin that state
 * machine, that accumulation, and the step package's documented State Changes
 * and Next Steps.
 *
 * <p>
 * The step is driven directly through its public {@code process()}, with a
 * hand-built {@code DataModel}: {@link SelectBestPatientSeries} reads only the
 * antigen-selected list (4.3's output), the Supporting Data antigen series
 * list, and the patient series stepper (5.1's output), so no Supporting Data
 * release is needed. Constructing the returned next step is inert - both
 * {@code PreFilterPatientSeries} and {@code IdentifyAndEvaluateVaccineGroup}
 * have do-nothing constructors and all their work is in {@code process()},
 * which is never called here. That is what keeps this a 4.5 test rather than an
 * 8.1 test.
 *
 * <p>
 * A fresh {@code SelectBestPatientSeries} is constructed per iteration, exactly
 * as the real pipeline does: 4.4 enters 4.5 once, and 8.9
 * {@code DetermineBestPatientSeries.process()} returns a brand-new instance of
 * this step each time it finishes an antigen.
 *
 * <p>
 * <b>Scoping note:</b> Chapter 8's own classes are out of scope here, including
 * {@code SelectPrioritizedPatientSeries} (8.7), which is separately suspect.
 * Nothing below invokes any Chapter 8 {@code process()}; where a test needs to
 * represent "Chapter 8 ran and produced something", it writes to the shared
 * {@code DataModel} lists by hand, the way 8.7/8.9 would.
 */
public class SelectBestPatientSeriesTest {

  private static final String HEPB = "HepB";
  private static final String MEASLES = "Measles";
  private static final String POLIO = "Polio";

  private DataModel dataModel;

  @Before
  public void setUp() {
    dataModel = new DataModel();
  }

  /**
   * Stands in for 4.3's output: the antigen-selected list 4.5 iterates. 4.5
   * reads this list unconditionally, so every test that expects to reach the
   * loop has to set it up.
   */
  private List<Antigen> selectAntigens(String... antigenNames) {
    List<Antigen> selected = new ArrayList<Antigen>();
    for (String antigenName : antigenNames) {
      selected.add(dataModel.getOrCreateAntigen(antigenName));
    }
    dataModel.setAntigenSelectedList(selected);
    return selected;
  }

  /** Adds a Supporting Data antigen series for the named antigen. */
  private AntigenSeries supportingDataAntigenSeries(String seriesName, Antigen targetDisease) {
    AntigenSeries antigenSeries = new AntigenSeries();
    antigenSeries.setSeriesName(seriesName);
    antigenSeries.setTargetDisease(targetDisease);
    dataModel.getAntigenSeriesList().add(antigenSeries);
    return antigenSeries;
  }

  /**
   * Stands in for 5.1's output: a relevant patient series on the stepper, which
   * is where 4.5 looks for the patient series belonging to the current antigen.
   */
  private PatientSeries relevantPatientSeries(String seriesName, Antigen targetDisease) {
    PatientSeries patientSeries = new PatientSeries(supportingDataAntigenSeries(seriesName, targetDisease));
    dataModel.getPatientSeriesStepper().add(patientSeries);
    return patientSeries;
  }

  private LogicStep process() throws Exception {
    return new SelectBestPatientSeries(dataModel).process();
  }

  private static List<String> seriesNamesOf(List<PatientSeries> patientSeriesList) {
    List<String> names = new ArrayList<String>();
    for (PatientSeries patientSeries : patientSeriesList) {
      names.add(patientSeries.getTrackedAntigenSeries().getSeriesName());
    }
    return names;
  }

  private static List<String> antigenSeriesNamesOf(List<AntigenSeries> antigenSeriesList) {
    List<String> names = new ArrayList<String>();
    for (AntigenSeries antigenSeries : antigenSeriesList) {
      names.add(antigenSeries.getSeriesName());
    }
    return names;
  }

  /**
   * Drives Figure 4-7's loop the way the pipeline does - one {@code process()}
   * per iteration, a fresh step instance each time, as if Chapter 8 had handed
   * control back at the "Is there another antigen to process?" decision - and
   * returns every antigen Chapter 8 would have been pointed at, in order.
   */
  private List<Antigen> runLoopToCompletion() throws Exception {
    List<Antigen> visited = new ArrayList<Antigen>();
    int maximumIterations = dataModel.getAntigenSelectedList().size() + 1;
    for (int i = 0; i <= maximumIterations; i++) {
      LogicStep next = process();
      if (next.getLogicStepType() == LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP) {
        return visited;
      }
      assertEquals("While antigens remain, 4.5 delegates to Chapter 8 (8.1) and nowhere else",
          LogicStepType.PRE_FILTER_PATIENT_SERIES, next.getLogicStepType());
      visited.add(dataModel.getAntigen());
    }
    throw new AssertionError("4.5's loop did not terminate after " + maximumIterations + " iterations");
  }

  /**
   * Figure 4-7: "Get first antigen", then straight into "Select Best Patient
   * Series (Chapter 8)". The first call must point Chapter 8 at the first
   * antigen of the selected list and transition to 8.1.
   */
  @Test
  public void firstCallSelectsTheFirstAntigenAndDelegatesToPreFilterPatientSeries() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES, POLIO);
    assertNull("Precondition: no antigen is current before 4.5 runs", dataModel.getAntigen());

    LogicStep next = process();

    assertEquals("The loop starts at the first antigen", 0, dataModel.getAntigenPos());
    assertSame("Chapter 8 must be pointed at the first selected antigen", selected.get(0), dataModel.getAntigen());
    assertEquals(LogicStepType.PRE_FILTER_PATIENT_SERIES, next.getLogicStepType());
  }

  /**
   * Figure 4-7: "Get next antigen" on the Yes branch. State Changes: "each call
   * advances {@code dataModel.antigenPos}". Each re-entry from Chapter 8 moves
   * exactly one antigen forward.
   */
  @Test
  public void eachSubsequentCallAdvancesToTheNextAntigen() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES, POLIO);

    process();
    assertSame(selected.get(0), dataModel.getAntigen());
    process();
    assertEquals(1, dataModel.getAntigenPos());
    assertSame(selected.get(1), dataModel.getAntigen());
    process();
    assertEquals(2, dataModel.getAntigenPos());
    assertSame(selected.get(2), dataModel.getAntigen());
  }

  /**
   * Figure 4-7's loop as a whole: "For each antigen, select the best patient
   * series". Every antigen gets exactly one turn at Chapter 8 - none skipped,
   * none visited twice - and then the loop ends.
   */
  @Test
  public void everyAntigenIsHandedToChapterEightExactlyOnce() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES, POLIO);

    List<Antigen> visited = runLoopToCompletion();

    assertEquals("One delegation to Chapter 8 per antigen, no more and no fewer", 3, visited.size());
    assertEquals("The loop walks the antigen-selected list in its own order", selected, visited);
  }

  /**
   * Figure 4-7: the "No" branch of "Is there another antigen to process?" ends
   * the process. Next Steps: 4.5 "transitions to 4.6 Identify and Evaluate
   * Vaccine Group once all antigens are processed."
   */
  @Test
  public void transitionsToIdentifyAndEvaluateVaccineGroupOnceEveryAntigenIsProcessed() throws Exception {
    selectAntigens(HEPB, MEASLES);

    assertEquals(LogicStepType.PRE_FILTER_PATIENT_SERIES, process().getLogicStepType());
    assertEquals(LogicStepType.PRE_FILTER_PATIENT_SERIES, process().getLogicStepType());
    assertEquals("After the last antigen the loop hands off to 4.6",
        LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, process().getLogicStepType());
  }

  /**
   * The loop's exit is stable. 4.5 is not only re-entered from 8.9 - both
   * {@code EvaluateAndForecastAllPatientSeries}' loop guards and
   * {@code ForecastDatesAndReasons}' cycle guard force a transition here too -
   * so an extra call after the loop has finished must keep ending the process
   * rather than wrapping around or reading past the end of the list.
   */
  @Test
  public void callingAgainAfterTheLoopHasFinishedStillEndsTheProcess() throws Exception {
    selectAntigens(HEPB);
    runLoopToCompletion();

    assertEquals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, process().getLogicStepType());
    assertEquals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, process().getLogicStepType());
  }

  /**
   * Degenerate case of "loops through each antigen": with no antigen selected
   * there is nothing for Chapter 8 to score, so the step ends the process
   * immediately without ever delegating. (Figure 4-7 does not describe this
   * case - it draws "Get first antigen" as unconditional - so this pins the
   * implementation's reading of it.)
   */
  @Test
  public void noAntigensAtAllGoesStraightToIdentifyAndEvaluateVaccineGroup() throws Exception {
    selectAntigens();

    LogicStep next = process();

    assertEquals(LogicStepType.IDENTIFY_AND_EVALUATE_VACCINE_GROUP, next.getLogicStepType());
    assertNull(dataModel.getAntigen());
  }

  /**
   * State Changes: "while antigens remain, it sets up {@code
   * antigenSeriesSelectedList} ... for the current antigen". The list is
   * filtered from the full Supporting Data antigen series list down to the
   * series whose target disease is the antigen currently being processed.
   */
  @Test
  public void antigenSeriesSelectedListHoldsOnlyTheCurrentAntigensSeries() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES);
    supportingDataAntigenSeries("HepB standard", selected.get(0));
    supportingDataAntigenSeries("HepB risk", selected.get(0));
    supportingDataAntigenSeries("Measles standard", selected.get(1));

    process();

    assertEquals("Only the current antigen's series, and all of them",
        Arrays.asList("HepB standard", "HepB risk"),
        antigenSeriesNamesOf(dataModel.getAntigenSeriesSelectedList()));

    process();

    assertEquals("The list is rebuilt for the next antigen, not appended to",
        Arrays.asList("Measles standard"),
        antigenSeriesNamesOf(dataModel.getAntigenSeriesSelectedList()));
  }

  /**
   * State Changes: "while antigens remain, it sets up ... {@code
   * selectedPatientSeriesList} for the current antigen". Chapter 8 scores one
   * antigen at a time, so the patient series it is handed must be exactly the
   * relevant patient series (5.1's output, on the patient series stepper) whose
   * tracked antigen series targets the current antigen.
   */
  @Test
  public void selectedPatientSeriesListHoldsOnlyTheCurrentAntigensPatientSeries() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES);
    relevantPatientSeries("HepB standard", selected.get(0));
    relevantPatientSeries("Measles standard", selected.get(1));
    relevantPatientSeries("HepB risk", selected.get(0));

    process();

    assertEquals("Chapter 8 must only see the current antigen's patient series",
        Arrays.asList("HepB standard", "HepB risk"),
        seriesNamesOf(dataModel.getSelectedPatientSeriesList()));
  }

  /**
   * The per-antigen patient series list is rebuilt each iteration rather than
   * accumulated, so one antigen's scoring never sees the previous antigen's
   * series.
   */
  @Test
  public void selectedPatientSeriesListIsRebuiltForEachAntigen() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES);
    relevantPatientSeries("HepB standard", selected.get(0));
    relevantPatientSeries("Measles standard", selected.get(1));

    process();
    assertEquals(Arrays.asList("HepB standard"), seriesNamesOf(dataModel.getSelectedPatientSeriesList()));

    process();
    assertEquals("The second antigen's list must not carry the first antigen's series",
        Arrays.asList("Measles standard"), seriesNamesOf(dataModel.getSelectedPatientSeriesList()));
  }

  /**
   * "Loops through each antigen" means every selected antigen, not only those
   * the patient has a relevant patient series for: an antigen with no patient
   * series still gets its turn, and Chapter 8 is handed an empty list for it.
   * (An antigen with nothing to score is how a patient with no path to immunity
   * for that antigen falls out of 8.1 with no best series.)
   */
  @Test
  public void antigensWithNoPatientSeriesAreStillHandedToChapterEight() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES);
    relevantPatientSeries("Measles standard", selected.get(1));

    LogicStep next = process();

    assertEquals("The antigen is still visited", LogicStepType.PRE_FILTER_PATIENT_SERIES, next.getLogicStepType());
    assertSame(selected.get(0), dataModel.getAntigen());
    assertTrue("Nothing relevant for this antigen, so nothing handed to Chapter 8",
        dataModel.getSelectedPatientSeriesList().isEmpty());
  }

  /**
   * Implementation behaviour with no specification basis, pinned because it is
   * load-bearing for how the filters above behave: both filters compare with
   * {@code Antigen.equals}, which compares names rather than instance identity.
   * A patient series or antigen series carrying a different {@code Antigen}
   * object with the same name is still selected. This is what lets Supporting
   * Data loaded from separate files agree on an antigen; it also means a
   * duplicate-named antigen would silently be treated as the same one.
   */
  @Test
  public void antigenMatchingIsByNameNotByInstanceIdentity() throws Exception {
    selectAntigens(HEPB);
    Antigen separateHepBInstance = new Antigen();
    separateHepBInstance.setName(HEPB);
    relevantPatientSeries("HepB standard", separateHepBInstance);

    process();

    assertEquals("A same-named but distinct Antigen instance still matches",
        Arrays.asList("HepB standard"), seriesNamesOf(dataModel.getSelectedPatientSeriesList()));
    assertEquals(1, dataModel.getAntigenSeriesSelectedList().size());
  }

  /**
   * State Changes: 4.5 creates the best-patient-series list that 8.9
   * {@code DetermineBestPatientSeries} writes into. 8.9 calls
   * {@code getBestPatientSeriesList().add(...)} with no null guard of its own,
   * so it has to exist before the first antigen reaches Chapter 8.
   */
  @Test
  public void firstCallCreatesTheBestPatientSeriesListBeforeChapterEightRuns() throws Exception {
    selectAntigens(HEPB);
    assertNull("Precondition: nothing has created the best patient series list yet",
        dataModel.getBestPatientSeriesList());

    process();

    assertNotNull("8.9 adds to this list without creating it, so 4.5 must", dataModel.getBestPatientSeriesList());
    assertTrue("4.5 makes no best-series decisions of its own", dataModel.getBestPatientSeriesList().isEmpty());
  }

  /**
   * Purpose: "A best patient series will be selected for each Series Group ...
   * In other cases, multiple best patient series may be selected for a patient.
   * For example, a patient may need to complete a risk series in the short term
   * ... but still need to complete a standard series later in life." The list
   * must therefore accumulate across the whole antigen loop - a per-antigen
   * reset would leave only the last antigen's best series standing, and 4.7's
   * vaccine group steps read the accumulated list.
   */
  @Test
  public void bestPatientSeriesListAccumulatesAcrossTheWholeAntigenLoop() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES);
    PatientSeries hepBBest = new PatientSeries(supportingDataAntigenSeries("HepB standard", selected.get(0)));
    PatientSeries measlesBest = new PatientSeries(supportingDataAntigenSeries("Measles standard", selected.get(1)));

    // Antigen 1, then 8.9 records its best series.
    process();
    dataModel.getBestPatientSeriesList().add(hepBBest);
    // Antigen 2, then 8.9 records its best series.
    process();
    dataModel.getBestPatientSeriesList().add(measlesBest);
    // Loop ends.
    process();

    assertEquals("Both antigens' best patient series survive to 4.6",
        Arrays.asList("HepB standard", "Measles standard"), seriesNamesOf(dataModel.getBestPatientSeriesList()));
  }

  /**
   * The same claim from the other side: the list instance itself is created
   * once and never replaced, so a reference taken early (8.9 resolves it per
   * call, but {@code ForecastServlet} and the renderers hold it) stays valid.
   */
  @Test
  public void bestPatientSeriesListIsCreatedOnceAndNeverReplaced() throws Exception {
    selectAntigens(HEPB, MEASLES);

    process();
    List<PatientSeries> createdOnFirstCall = dataModel.getBestPatientSeriesList();
    process();
    process();

    assertSame("The best patient series list must not be replaced mid-loop",
        createdOnFirstCall, dataModel.getBestPatientSeriesList());
  }

  /**
   * "Applies the business rules found in Chapter 8 <i>to each antigen</i>": the
   * prioritized patient series list is Chapter 8's per-antigen scratch space
   * (8.7 writes it, 8.9 reads it), so 4.5 must empty it on the way in, or the
   * next antigen's 8.9 would see the previous antigen's prioritized series.
   */
  @Test
  public void prioritizedPatientSeriesListIsEmptiedBeforeEachAntigenReachesChapterEight() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES);
    process();
    // Chapter 8 (8.7) prioritizes a series for the first antigen.
    dataModel.getPrioritizedPatientSeriesList()
        .add(new PatientSeries(supportingDataAntigenSeries("HepB standard", selected.get(0))));

    process();

    assertTrue("The next antigen must start Chapter 8 with an empty prioritized list",
        dataModel.getPrioritizedPatientSeriesList().isEmpty());
  }

  /**
   * The prioritized list is emptied in place rather than replaced with a new
   * list. {@code DataModel} exposes no way for the step to be sure a caller is
   * not holding the old reference, and 8.7/8.2 both add through the getter, so
   * clearing in place is the behaviour to pin.
   */
  @Test
  public void prioritizedPatientSeriesListIsClearedInPlaceRatherThanReplaced() throws Exception {
    selectAntigens(HEPB, MEASLES);
    List<PatientSeries> beforeAnyCall = dataModel.getPrioritizedPatientSeriesList();

    process();
    process();

    assertSame(beforeAnyCall, dataModel.getPrioritizedPatientSeriesList());
  }

  /**
   * State Changes: "once exhausted, it clears per-antigen state and delegates
   * to 4.6." The current antigen and the antigen-series selected list are both
   * nulled, so nothing downstream of the loop can mistake the last antigen
   * processed for a current one.
   */
  @Test
  public void exhaustingTheLoopClearsTheCurrentAntigenAndAntigenSeriesSelectedList() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB);
    supportingDataAntigenSeries("HepB standard", selected.get(0));

    process();
    assertNotNull("Precondition: mid-loop these are populated", dataModel.getAntigen());
    assertNotNull(dataModel.getAntigenSeriesSelectedList());

    process();

    assertNull("No antigen is current once the loop has ended", dataModel.getAntigen());
    assertNull("No antigen series are selected once the loop has ended", dataModel.getAntigenSeriesSelectedList());
  }

  /**
   * The other half of "it clears per-antigen state", pinned as it actually
   * behaves rather than as the sentence reads: {@code selectedPatientSeriesList}
   * is also per-antigen state that this step set up, and it is <i>not</i>
   * cleared on the exhaustion branch - the last antigen's patient series are
   * still on the data model after the loop ends, alongside a null antigen and a
   * null antigen-series selected list.
   *
   * <p>
   * Recorded here as an imprecision in the step package's State Changes wording
   * rather than asserted as a defect, because section 4.5 itself says nothing
   * about state and nothing in {@code cdsi-engine} reads
   * {@code selectedPatientSeriesList} after the loop ends (its only readers,
   * {@code SelectPrioritizedPatientSeries} and {@code CompletePatientSeries},
   * are Chapter 8 steps that no longer run). It is not purely theoretical
   * though: {@code LogicStepRenderer.printSelectBestPatientSeriesPost} prints
   * "Done checking Antigens" and then lists this very list, so the cdsi-web
   * step view shows the last antigen's patient series under a heading that
   * implies none is current. Flagged for review, not classified.
   */
  @Test
  public void exhaustingTheLoopLeavesTheLastAntigensSelectedPatientSeriesListInPlace() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB);
    relevantPatientSeries("HepB standard", selected.get(0));

    process();
    process();

    assertEquals("Actual behaviour: the last antigen's patient series are still on the data model",
        Arrays.asList("HepB standard"), seriesNamesOf(dataModel.getSelectedPatientSeriesList()));
  }

  /**
   * Plain-Language Walkthrough: "the real decision-making (Chapter 8's
   * series-scoring/prioritization business rules) happens once per antigen in
   * the steps it delegates to". Driving 4.5's whole loop with relevant patient
   * series present must therefore select nothing: no best series, no
   * prioritized series, no scorable series. Every one of those is Chapter 8's
   * to decide.
   */
  @Test
  public void stepSelectsNoBestSeriesOfItsOwn() throws Exception {
    List<Antigen> selected = selectAntigens(HEPB, MEASLES, POLIO);
    relevantPatientSeries("HepB standard", selected.get(0));
    relevantPatientSeries("Measles standard", selected.get(1));
    relevantPatientSeries("Polio standard", selected.get(2));

    runLoopToCompletion();

    assertTrue("Choosing the best patient series is 8.9's job, not 4.5's",
        dataModel.getBestPatientSeriesList().isEmpty());
    assertTrue("Prioritizing patient series is 8.7's job, not 4.5's",
        dataModel.getPrioritizedPatientSeriesList().isEmpty());
    assertNull("Pre-filtering to scorable patient series is 8.1's job, not 4.5's",
        dataModel.getScorablePatientSeriesList());
  }
}
